package org.jetlinks.community.influxdb2.service;

import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.influxdb2.InfluxDataEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Tensai
 */
@Service
@Slf4j
public class Influxdb2Manager {

    private final Influxdb2Service service;

    public Influxdb2Manager(Influxdb2Service service) {
        this.service = service;
    }


    public Mono<Void> save(Point point) {
        service.save(Flux.just(point));
        return Mono.empty();
    }

    public Flux<Map<String, Object>> query(com.influxdb.query.dsl.Flux flux) {
        log.error("Flux -- {}", flux.toString());
        return service.query(flux, FluxRecord::getValues);
    }


    public Flux<InfluxDataEntity> queryInfluxData(com.influxdb.query.dsl.Flux flux) {
        log.error("queryInfluxData -- {}", flux.toString());
        List<InfluxDataEntity> listTemp = new ArrayList<>();
        return service.query(flux, Function.identity())
            .collectList()
            .flatMap(list -> {
                list.stream().collect(Collectors.groupingBy(fluxRecord -> Objects.requireNonNull(fluxRecord.getTime()).toEpochMilli()))
                    .forEach((k, v) -> {
                        InfluxDataEntity dataEntity = new InfluxDataEntity(k);
                        v.forEach(item -> {
                            dataEntity.getFields().put(String.valueOf(item.getValues().get("_field")), item.getValues().get("_value"));
                            item.getValues().forEach((m, n) -> {
                                if (!m.startsWith("_") && !"result".equals(m) && !"table".equals(m)) {
                                    dataEntity.getTags().put(m, n);
                                }
                            });
                        });
                        listTemp.add(dataEntity);
                    });
                return Mono.just(listTemp);
            })
            .flux()
            .flatMap(list -> Flux.just(list.toArray(new InfluxDataEntity[0])));
    }
}
