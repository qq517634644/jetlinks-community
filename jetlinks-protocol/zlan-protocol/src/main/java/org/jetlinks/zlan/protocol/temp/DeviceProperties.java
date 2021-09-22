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

    Map<String, Map<String, BigDecimal>> map = new ConcurrentHashMap<>();

    EmitterProcessor<Tuple2<String, Map<String, BigDecimal>>> processor = EmitterProcessor.create(false);
    FluxSink<Tuple2<String, Map<String, BigDecimal>>> sink = processor.sink(FluxSink.OverflowStrategy.BUFFER);

    @Nonnull
    public Flux<Tuple2<String, Map<String, BigDecimal>>> handleFlux() {
        return processor
            .map(Function.identity());
    }

    public Map<String, BigDecimal> getProperties(String key) {
        return map.get(key);
    }

    public void initMap(String key, Map<String, BigDecimal> value) {
        map.put(key, value);
    }

    public void setProperties(String id, String key, BigDecimal value) {
        Map<String, BigDecimal> tempMap = map.computeIfAbsent(id, k -> new HashMap<>());
        tempMap.put(key, value);
        map.put(id, tempMap);
        sinkNext(id, tempMap);
    }

    public void sinkNext(String id, Map<String, BigDecimal> map) {
        sinkNext(new Tuple2<>(id, map));
    }

    public void sinkNext(Tuple2<String, Map<String, BigDecimal>> tp2) {
        sink.next(tp2);
    }

    public Map<String, Map<String, BigDecimal>> getMap() {
        return map;
    }

}
