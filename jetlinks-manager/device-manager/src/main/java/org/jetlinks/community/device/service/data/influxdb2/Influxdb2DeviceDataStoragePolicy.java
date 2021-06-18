package org.jetlinks.community.device.service.data.influxdb2;

import com.alibaba.fastjson.JSON;
import com.influxdb.client.write.Point;
import com.influxdb.query.dsl.functions.restriction.Restrictions;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.community.device.service.LocalDeviceInstanceService;
import org.jetlinks.community.device.service.data.AbstractDeviceDataStoragePolicy;
import org.jetlinks.community.device.service.data.DeviceDataStorageProperties;
import org.jetlinks.community.influxdb2.config.Influxdb2Properties;
import org.jetlinks.community.influxdb2.service.Influxdb2Manager;
import org.jetlinks.community.timeseries.SimpleTimeSeriesData;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.core.device.DeviceRegistry;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Influxdb2时序数据存储策略
 * <p>
 * 提供时序数据通用的查询存储逻辑
 * </p>
 *
 * @author Tensai
 */
@Slf4j
public abstract class Influxdb2DeviceDataStoragePolicy extends AbstractDeviceDataStoragePolicy {

    protected final Influxdb2Manager manager;

    protected final LocalDeviceInstanceService service;

    protected final Influxdb2Properties influxdb2Properties;

    public Influxdb2DeviceDataStoragePolicy(DeviceRegistry registry,
                                            DeviceDataStorageProperties properties,
                                            Influxdb2Manager manager, LocalDeviceInstanceService service, Influxdb2Properties influxdb2Properties) {
        super(registry, properties);
        this.manager = manager;
        this.service = service;
        this.influxdb2Properties = influxdb2Properties;
    }

    @Override
    protected Mono<Void> doSaveData(String metric, TimeSeriesData data) {
//        log.info("doSaveData1 >>> metric - {},data - {}", metric, JSON.toJSONString(data));
        Point point = convertTs2Influx(metric, data);
        manager.save(point);
        return Mono.empty();
    }

    @Override
    protected Mono<Void> doSaveData(String metric, Flux<TimeSeriesData> data) {
        log.info("doSaveData2 >>> metric - {},data - {}", metric, JSON.toJSONString(data));
        // TODO
        return Mono.empty();
    }

    @Override
    protected <T> Flux<T> doQuery(String metric,
                                  QueryParamEntity paramEntity,
                                  Function<TimeSeriesData, T> mapper) {
        log.info("doQuery >>> \r\nmetric - {},\r\nparamEntity - {}, \r\nmapper - {}",
            metric, JSON.toJSONString(paramEntity), JSON.toJSONString(mapper));
        return Flux.empty();
    }


    @Override
    protected <T> Mono<PagerResult<T>> doQueryPager(String metric,
                                                    QueryParamEntity paramEntity,
                                                    Function<TimeSeriesData, T> mapper) {
        log.info("doQueryPager >>> \r\nmetric - {},\r\nparamEntity - {}, \r\nmapper - {}",
            metric, JSON.toJSONString(paramEntity), JSON.toJSONString(mapper));
        return Mono.zip(
            manager.query(getCountFlux(metric, paramEntity))
                .collectList(),
            manager.query(getLimitFlux(metric, paramEntity))
                .map(
                    map -> new SimpleTimeSeriesData(((Instant) map.get("_time")).toEpochMilli(), map)
                ).map(mapper)
                .collectList(),
            (cou, res) -> PagerResult.of(((Long) cou.get(0).get("_value")).intValue(), res, paramEntity));
    }

    /**
     * TS数据转换为Influxdb2数据
     *
     * @param metric 指标名称 暂时当成influxdb的表名
     * @param data   ts数据
     * @return Point
     */
    public abstract Point convertTs2Influx(String metric, TimeSeriesData data);


    /**
     * 创建分页统计Flux语法对象
     *
     * @param productId 产品ID
     * @param property  查询指标名称
     * @param deviceId  设备ID
     * @param param     查询参数
     * @return Flux
     */
    protected com.influxdb.query.dsl.Flux getCountFlux(String productId, String property, String deviceId, QueryParamEntity param) {
        return getFlux(productId, property, deviceId, param)
            .count().yield("count");
    }

    protected com.influxdb.query.dsl.Flux getCountFlux(String productId, Set<String> properties, String deviceId, QueryParamEntity param) {
        return getFlux(productId, properties, deviceId, param)
            .count().yield("count");
    }

    protected com.influxdb.query.dsl.Flux getCountFlux(String metric, QueryParamEntity param) {
        return baseFlux(metric, param)
            .count().yield("count");
    }

    /**
     * 创建分页查询Flux语法
     *
     * @param productId 产品ID
     * @param property  查询指标名称
     * @param deviceId  设备ID
     * @param param     查询参数
     * @return Flux
     */
    protected com.influxdb.query.dsl.Flux getLimitFlux(String productId, String property, String deviceId, QueryParamEntity param) {
        return getFlux(productId, property, deviceId, param)
            .limit(param.getPageSize(),
                (param.getPageIndex() - param.getFirstPageIndex()) * param.getPageSize()
            );
    }

    protected com.influxdb.query.dsl.Flux getLimitFlux(String productId, Set<String> properties, String deviceId, QueryParamEntity param) {
        return getFlux(productId, properties, deviceId, param)
            .limit(param.getPageSize(),
                (param.getPageIndex() - param.getFirstPageIndex()) * param.getPageSize()
            );
    }

    protected com.influxdb.query.dsl.Flux getLimitFlux(String metric, QueryParamEntity param) {
        return baseFlux(metric, param)
            .limit(param.getPageSize(),
                (param.getPageIndex() - param.getFirstPageIndex()) * param.getPageSize()
            );
    }

    /**
     * 创建Flux语法对象
     *
     * @param productId 产品ID
     * @param property  查询指标名称
     * @param deviceId  设备ID
     * @param param     查询参数
     * @return Flux
     */
    protected com.influxdb.query.dsl.Flux getFlux(String productId, String property, String deviceId, QueryParamEntity param) {
        return baseFlux(productId, deviceId, param)
            .filter(Restrictions.field().equal(property));
    }

    protected com.influxdb.query.dsl.Flux getFlux(String productId, Set<String> properties, String deviceId, QueryParamEntity param) {
        com.influxdb.query.dsl.Flux flux = baseFlux(productId, deviceId, param);
        List<Restrictions> fieldList = new ArrayList<>();
        properties.forEach(property -> fieldList.add(Restrictions.field().equal(property)));
        flux = flux.filter(Restrictions.or(fieldList.toArray(new Restrictions[0])));
        return flux;
    }

    protected com.influxdb.query.dsl.Flux baseFlux(String productId, String deviceId, QueryParamEntity param) {
        com.influxdb.query.dsl.Flux flux = com.influxdb.query.dsl.Flux
            .from(influxdb2Properties.getBucket())
            // TODO 完善
            .range(Instant.now().plus(-2, ChronoUnit.DAYS))
            .filter(Restrictions.measurement().equal("properties_" + productId))
            .filter(Restrictions.tag("deviceId").equal(deviceId));
        if (param.getSorts() != null) {
            // TODO 完善
            flux = flux.sort();
        }
        return flux;
    }

    protected com.influxdb.query.dsl.Flux baseFlux(String metric, QueryParamEntity param) {
        com.influxdb.query.dsl.Flux flux = com.influxdb.query.dsl.Flux
            .from(influxdb2Properties.getBucket())
            // TODO 完善
            .range(Instant.now().plus(-2, ChronoUnit.DAYS))
            .filter(Restrictions.measurement().equal(metric));
        Map<String, List<Restrictions>> fieldListMap = new HashMap<>(16);
        param.getTerms().forEach(term -> {
            // TODO
            switch (term.getTermType()) {
                case "eq":
                    fieldListMap.computeIfAbsent(term.getColumn(), r -> new ArrayList<>())
                        .add(Restrictions.tag(term.getColumn()).equal(term.getValue()));
                    break;
                default:
                    log.error(JSON.toJSONString(term));
                    break;
            }
        });
        AtomicReference<com.influxdb.query.dsl.Flux> fluxTemp = new AtomicReference<>();
        com.influxdb.query.dsl.Flux finalFlux = flux;
        fieldListMap.forEach((k, v) -> fluxTemp.set(finalFlux.filter(Restrictions.or(v.toArray(new Restrictions[0])))));
        if (param.getSorts() != null) {
            // TODO 完善
            flux = fluxTemp.get().sort();
        }
        return flux;
    }
}
