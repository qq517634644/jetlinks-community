package org.jetlinks.zlan.protocol.temp;

import io.vavr.Tuple2;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * @author Tensai
 */

public enum DeviceProperties {
    /**
     * 设备最后上报的属性
     */
    LAST;

    /**
     * total属性
     */
    Map<String, Map<String, BigDecimal>> totalMap = new ConcurrentHashMap<>();

    EmitterProcessor<Tuple2<String, Map<String, BigDecimal>>> processor = EmitterProcessor.create(false);
    FluxSink<Tuple2<String, Map<String, BigDecimal>>> sink = processor.sink(FluxSink.OverflowStrategy.BUFFER);

    /**
     * 所有属性
     */
    Map<String, Map<String, Object>> allPropertiesMap = new ConcurrentHashMap<>();

    @Nonnull
    public Flux<Tuple2<String, Map<String, BigDecimal>>> handleFlux() {
        return processor
            .map(Function.identity());
    }

    public Map<String, BigDecimal> getTotalProperties(String key) {
        return totalMap.get(key);
    }

    public Map<String, Object> getAllProperties(String key) {
        return allPropertiesMap.computeIfAbsent(key, k -> new HashMap<>());
    }

    public void initMap(String key, Map<String, BigDecimal> value) {
        totalMap.put(key, value);
    }

    public void setTotalProperties(String id, String key, BigDecimal value) {
        Map<String, BigDecimal> tempMap = totalMap.computeIfAbsent(id, k -> new HashMap<>());
        tempMap.put(key, value);
        totalMap.put(id, tempMap);
        sinkNext(id, tempMap);
    }

    public void setAllProperties(String id, Map<String, Object> valueMap) {
        allPropertiesMap.put(id, valueMap);
    }

    private void sinkNext(String id, Map<String, BigDecimal> map) {
        sinkNext(new Tuple2<>(id, map));
    }

    private void sinkNext(Tuple2<String, Map<String, BigDecimal>> tp2) {
        sink.next(tp2);
    }

    public Map<String, Map<String, BigDecimal>> getTotalMap() {
        return totalMap;
    }

}
