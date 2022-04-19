package org.jetlinks.community.zlan.controller;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.jetlinks.zlan.protocol.temp.DeviceProperties;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;


/**
 * @author Tensai
 */
@RestController
@RequestMapping({"/zlan/test"})
@Authorize(ignore = true)
@Slf4j
@AllArgsConstructor
public class TestController {

    private final ReactiveRedisOperations<String, String> redis;

    @GetMapping("/test")
    public Mono<String> test() {
        redis.<String, String>opsForHash().entries("device:properties:last").subscribe(it -> {
            log.info("{} --> {}", it.getKey(), it.getValue());
            DeviceProperties.LAST.initMap(it.getKey(), JSON.parseObject(it.getValue(), HashMap.class));
        });
        DeviceProperties.LAST.getTotalMap().forEach((k, v) -> {
            log.info("key --> {}", k);
            v.forEach((mk, mv) -> {
                log.info("value ==> {} --> {}", mk, mv);
            });
        });
        return Mono.just("Success");
    }

}
