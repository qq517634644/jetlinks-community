package org.jetlinks.community.influxdb2.service;

import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

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
//        Map<Long, List<Map<String, Object>>> mapT = new HashMap<>(32);
//        service.query(flux, FluxRecord::getValues).map(
//            map->{
//                mapT.computeIfAbsent(((Instant) map.get("_time")).toEpochMilli(), k ->new ArrayList<>()).add(map);
//                return mapT;
//            }
//        ).map(
//            map->{
//
//            }
//        );
//        List<Map<String, Object>> mapList = new ArrayList<>();
//
//        return Flux.fromIterable(mapList);
    }
}
