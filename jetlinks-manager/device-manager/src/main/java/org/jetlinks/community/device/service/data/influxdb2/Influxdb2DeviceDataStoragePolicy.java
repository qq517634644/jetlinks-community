package org.jetlinks.community.device.service.data.influxdb2;

import com.alibaba.fastjson.JSON;
import com.influxdb.client.write.Point;
import com.influxdb.query.dsl.functions.restriction.Restrictions;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.core.param.Column;
import org.hswebframework.ezorm.core.param.Sort;
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
import java.util.stream.Collectors;

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
//        log.info("doSaveData1 >|> metric - {},data - {}", metric, JSON.toJSONString(point));
        return manager.save(point);
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
            manager.query(getCountFluxLog(metric, paramEntity))
                   .collectList(),
            manager.queryInfluxData(getLimitFluxLog(metric, paramEntity))
                   .map(
                       data -> (TimeSeriesData) new SimpleTimeSeriesData(data.getTime(), data.getValues())
                   ).map(mapper)
                   .collectList(),
            (cou, res) -> PagerResult.of(getTotal(cou), res, paramEntity));
    }

    private int getTotal(List<Map<String, Object>> cou) {
        if (cou == null || cou.size() == 0 || cou.get(0).get("_value") == null) {
            return 0;
        }
        return ((Long) cou.get(0).get("_value")).intValue();
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

    /**
     * 查询数据条目数
     *
     * @param metric 表名
     * @param param  参数名
     * @return Flux查询
     */
    protected com.influxdb.query.dsl.Flux getCountFluxLog(String metric, QueryParamEntity param) {
        List<Restrictions> restrictionsList = FluxBuildUtil.queryParamToFilterLog(param);
        if (restrictionsList.size() > 0) {
            return baseFlux(metric, param)
                .pivot(new String[]{"_time"}, new String[]{"_field"}, "_value")
                .filter(Restrictions.or(restrictionsList.toArray(new Restrictions[0])))
                .rename(new HashMap<String, String>() {{
                    put("content", "_value");
                }})
                .count().yield("count");
        } else {
            return baseFlux(metric, param)
                .count().yield("count");
        }
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

    /**
     * 分页查询
     *
     * @param metric 表名
     * @param param  参数
     * @return Flux语法
     */
    protected com.influxdb.query.dsl.Flux getLimitFluxLog(String metric, QueryParamEntity param) {
        List<Restrictions> restrictionsList = FluxBuildUtil.queryParamToFilterLog(param);
        if (restrictionsList.size() > 0) {
            return baseFlux(metric, param)
                .pivot(new String[]{"_time"}, new String[]{"_field"}, "_value")
                .filter(Restrictions.or(restrictionsList.toArray(new Restrictions[0])))
                .limit(param.getPageSize(),
                       (param.getPageIndex() - param.getFirstPageIndex()) * param.getPageSize()
                );
        } else {
            return baseFlux(metric, param)
                .pivot(new String[]{"_time"}, new String[]{"_field"}, "_value")
                .limit(param.getPageSize(),
                       (param.getPageIndex() - param.getFirstPageIndex()) * param.getPageSize()
                );
        }
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

    /**
     * @param productId
     * @param properties
     * @param deviceId
     * @param param
     * @return
     */
    protected com.influxdb.query.dsl.Flux getFlux(String productId, Set<String> properties, String deviceId,
                                                  QueryParamEntity param) {
        com.influxdb.query.dsl.Flux flux = baseFlux(productId, deviceId, param);
        List<Restrictions> fieldList = new ArrayList<>();
        properties.forEach(property -> fieldList.add(Restrictions.field().equal(property)));
        flux = flux.filter(Restrictions.or(fieldList.toArray(new Restrictions[0])));
        return flux;
    }

    /**
     * 指标查询
     *
     * @param productId 产品ID
     * @param deviceId  设备ID
     * @param param     查询参数
     * @return flux查询
     */
    protected com.influxdb.query.dsl.Flux baseFlux(String productId, String deviceId, QueryParamEntity param) {
        List<Instant> instantList = FluxBuildUtil.queryParamToRangeTime(param);
        com.influxdb.query.dsl.Flux flux;
        if (instantList.size() > 1) {
            flux = com.influxdb.query.dsl.Flux
                .from(influxdb2Properties.getBucket())
                .range(instantList.get(0), instantList.get(1))
                .filter(Restrictions.measurement().equal("properties_" + productId))
                .filter(Restrictions.tag("deviceId").equal(deviceId));
        } else {
            flux = com.influxdb.query.dsl.Flux
                .from(influxdb2Properties.getBucket())
                .range(Instant.now().plus(-6, ChronoUnit.HOURS))
                .filter(Restrictions.measurement().equal("properties_" + productId))
                .filter(Restrictions.tag("deviceId").equal(deviceId));
        }

        flux = flux.sort(makeSortColumnList(param.getSorts())).withDesc(true);
        return flux;
    }

    /**
     * 基础查询
     *
     * @param metric 表名
     * @param param  查询参数
     * @return flux查询
     */
    protected com.influxdb.query.dsl.Flux baseFlux(String metric, QueryParamEntity param) {
        List<Instant> instantList = FluxBuildUtil.queryParamToRangeTime(param);
        com.influxdb.query.dsl.Flux flux;
        if (instantList.size() > 1) {
            flux = com.influxdb.query.dsl.Flux
                .from(influxdb2Properties.getBucket())
                .range(instantList.get(0), instantList.get(1))
                .filter(Restrictions.measurement().equal(metric));
        } else {
            flux = com.influxdb.query.dsl.Flux
                .from(influxdb2Properties.getBucket())
                .range(Instant.now().plus(-6, ChronoUnit.HOURS))
                .filter(Restrictions.measurement().equal(metric));
        }
        Map<String, List<Restrictions>> fieldListMap = new HashMap<>(16);
        param.getTerms().forEach(term -> {
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
        flux = fluxTemp.get().sort(makeSortColumnList(param.getSorts())).withDesc(true);
        return flux;
    }

    private List<String> makeSortColumnList(List<Sort> sortList) {
        List<String> sortColumnList = new ArrayList<>();
        sortColumnList.add("_time");
        if (sortList != null) {
            sortColumnList.addAll(sortList.stream().map(Column::getName).filter(name -> !"createTime".equals(name)).collect(Collectors.toList()));
        }
        return sortColumnList;
    }
}
