package org.jetlinks.community.device.service.data.influxdb2;

import com.alibaba.fastjson.JSON;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProperty;
import org.jetlinks.community.device.service.LocalDeviceInstanceService;
import org.jetlinks.community.device.service.data.DeviceDataService;
import org.jetlinks.community.device.service.data.DeviceDataStoragePolicy;
import org.jetlinks.community.device.service.data.DeviceDataStorageProperties;
import org.jetlinks.community.influxdb2.config.Influxdb2Properties;
import org.jetlinks.community.influxdb2.service.Influxdb2Manager;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.timeseries.query.AggregationData;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.Converter;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 时序数据列存储策略
 *
 * @author zhouhao
 */
@Slf4j
@Component
public class Influxdb2ColumnDeviceDataStoragePolicy extends Influxdb2DeviceDataStoragePolicy implements DeviceDataStoragePolicy {


    public Influxdb2ColumnDeviceDataStoragePolicy(DeviceRegistry registry,
                                                  DeviceDataStorageProperties properties,
                                                  Influxdb2Manager manager,
                                                  LocalDeviceInstanceService service,
                                                  Influxdb2Properties influxdb2Properties) {
        super(registry, properties, manager, service, influxdb2Properties);
    }

    @Override
    public Point convertTs2Influx(String metric, TimeSeriesData data) {

        long time = data.getTimestamp();
        Map<String, String> tags = new HashMap<>(8);
        Map<String, Object> fields = new HashMap<>(16);
        data.getData().forEach((k, v) -> {
            if ("messageId".equals(k) || "productId".equals(k) || "id".equals(k) || "createTime".equals(k)
                || "timestamp".equals(k)) {

            } else if ("deviceId".equals(k) || "type".equals(k)) {
                tags.put(k, String.valueOf(v));
            } else {
                // 事件上报中所有字段全是tag
                if (metric.startsWith("event_")) {
                    tags.put(k, String.valueOf(v));
                    fields.put("value", 1);
                } else {
                    fields.put(k, v);
                }
            }
        });
        return new Point(metric)
            .addTags(tags)
            .addFields(fields)
            .time(time, WritePrecision.MS);
    }

    @Override
    public String getId() {
        return "influxdb2-column";
    }

    @Override
    public String getName() {
        return "influxdb2-列式存储";
    }

    @Override
    public String getDescription() {
        return "每个设备的全部属性为一行数据.需要设备每次上报全部属性.";
    }

    @Nonnull
    @Override
    public Mono<ConfigMetadata> getConfigMetadata() {
        // TODO
        return Mono.empty();
    }

    @Nonnull
    @Override
    public Mono<Void> registerMetadata(@Nonnull String productId, @Nonnull DeviceMetadata metadata) {
        log.info("registerMetadata >>> metric - {},metadata - {}", productId, JSON.toJSONString(metadata));
        // TODO
        return Mono.empty();
    }


    private Flux<DeviceProperty> queryEachDeviceProperty(String productId,
                                                         String deviceId,
                                                         Map<String, PropertyMetadata> property,
                                                         QueryParamEntity param) {
        //查询多个属性,分组聚合获取第一条数据
        log.info("queryEachDeviceProperty >>> productId - {},deviceId - {},property - {},param - {}",
            productId, deviceId, JSON.toJSONString(property), JSON.toJSONString(param));

        return manager.query(getLimitFlux(productId, property.keySet(), deviceId, param))
            .map(
                map -> {
                    DeviceProperty deviceProperty = new DeviceProperty()
                        .deviceId(deviceId)
                        .property((String) map.get("_field"));
                    deviceProperty.setTimestamp(((Instant) map.get("_time")).toEpochMilli());
                    deviceProperty.setFormatValue(map.get("_value"));
                    deviceProperty.setNumberValue(map.get("_value"));
//                                deviceProperty.setType();
                    deviceProperty.setValue(map.get("_value"));
                    return deviceProperty;
                }
            );
//        return param
//            .toQuery()
//            .includes(property.keySet().toArray(new String[0]))
//            .where("deviceId", deviceId)
//            .execute(q -> timeSeriesManager.getService(getPropertyTimeSeriesMetric(productId)).query(q))
//            .flatMap(data -> rowToProperty(data, property.values()));
    }

    @Nonnull
    @Override
    public Flux<DeviceProperty> queryEachOneProperties(@Nonnull String deviceId,
                                                       @Nonnull QueryParamEntity query,
                                                       @Nonnull String... properties) {
        // TODO
        return Flux.empty();
    }

    @Nonnull
    @Override
    public Mono<PagerResult<DeviceProperty>> queryPropertyPage(@Nonnull String deviceId,
                                                               @Nonnull String property,
                                                               @Nonnull QueryParamEntity param) {
        log.info("queryPropertyPage >>> {},{},{}", deviceId, property, JSON.toJSONString(param));
        return service.findById(deviceId)
            .map(DeviceInstanceEntity::getProductId)
            .flatMap(productId -> Mono.zip(
                manager.query(getCountFlux(productId, property, deviceId, param))
                    .collectList(),
                manager.query(getLimitFlux(productId, property, deviceId, param))
                    .map(
                        map -> {
                            DeviceProperty deviceProperty = new DeviceProperty()
                                .deviceId(deviceId)
                                .property(property);
                            deviceProperty.setTimestamp(((Instant) map.get("_time")).toEpochMilli());
                            deviceProperty.setFormatValue(map.get("_value"));
                            deviceProperty.setNumberValue(map.get("_value"));
//                                deviceProperty.setType();
                            deviceProperty.setValue(map.get("_value"));
                            return deviceProperty;
                        }
                    )
                    .collectList(),
                (cou, res) -> PagerResult.of(((Long) cou.get(0).get("_value")).intValue(), res, param)));
    }

    @Nonnull
    @Override
    public Flux<DeviceProperty> queryProperty(@Nonnull String deviceId,
                                              @Nonnull QueryParamEntity query,
                                              @Nonnull String... property) {
        // TODO
        return Flux.empty();
    }

    @Nonnull
    @Override
    public Flux<DeviceProperty> queryEachProperties(@Nonnull String deviceId,
                                                    @Nonnull QueryParamEntity query,
                                                    @Nonnull String... property) {
        return this
            .getProductAndMetadataByDevice(deviceId)
            .flatMapMany(tp2 -> {

                Map<String, PropertyMetadata> propertiesMap = getPropertyMetadata(tp2.getT2(), property)
                    .stream()
                    .collect(Collectors.toMap(PropertyMetadata::getId, Function.identity(), (a, b) -> a));

                return queryEachDeviceProperty(tp2.getT1().getId(), deviceId, propertiesMap, query);
            });
    }


    @Override
    public Flux<AggregationData> aggregationPropertiesByProduct(@Nonnull String productId,
                                                                @Nonnull DeviceDataService.AggregationRequest request,
                                                                @Nonnull DeviceDataService.DevicePropertyAggregation... properties) {
        // TODO
        return Flux.empty();
    }

    @Override
    public Flux<AggregationData> aggregationPropertiesByDevice(@Nonnull String deviceId,
                                                               @Nonnull DeviceDataService.AggregationRequest request,
                                                               @Nonnull DeviceDataService.DevicePropertyAggregation... properties) {
        // TODO
        return Flux.empty();
    }

    /**
     * 设备消息转换 二元组{deviceId, tsData}
     *
     * @param productId  产品ID
     * @param message    设备属性消息
     * @param properties 物模型属性
     * @return 数据集合
     * @see this#convertPropertiesForColumnPolicy(String, DeviceMessage, Map)
     * @see this#convertPropertiesForRowPolicy(String, DeviceMessage, Map)
     */
    @Override
    protected Flux<Tuple2<String, TimeSeriesData>> convertProperties(String productId, DeviceMessage message, Map<String, Object> properties) {
        return convertPropertiesForColumnPolicy(productId, message, properties);
    }

    @Override
    protected Object convertPropertyValue(Object value, PropertyMetadata metadata) {
//        log.info("convertPropertyValue >>> value - {}, metadata - {}", value, JSON.toJSONString(metadata));
        if (value == null || metadata == null) {
            return value;
        }
        if (metadata instanceof Converter) {
            return ((Converter<?>) metadata).convert(value);
        }
//        log.info("value - {}", value);
        return value;
    }

}
