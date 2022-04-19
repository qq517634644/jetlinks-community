package org.jetlinks.community.zlan.task;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.zlan.protocol.temp.DeviceProperties;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;

/**
 * @author Tensai
 */
@Slf4j
@Component
@EnableScheduling
@AllArgsConstructor
public class ZlanTask {

    private final ReactiveRedisOperations<String, String> redis;

    /**
     * 缓存最后一次的指标数据 方便计算步进值
     */
    @PostConstruct
    public void init() {
        redis.<String, String>opsForHash().entries("device:properties:last").subscribe(it -> {
            log.info("{} --> {}", it.getKey(), it.getValue());
            DeviceProperties.LAST.initMap(it.getKey(), JSON.parseObject(it.getValue(), HashMap.class));
        });
        DeviceProperties.LAST.handleFlux().subscribe(it -> {
            redis
                .<String, String>opsForHash()
                .put("device:properties:last", it._1(), JSON.toJSONString(it._2()))
                .subscribe();
        });
    }

}
