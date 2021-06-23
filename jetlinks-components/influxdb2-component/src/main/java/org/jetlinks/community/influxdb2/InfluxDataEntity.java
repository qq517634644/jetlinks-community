package org.jetlinks.community.influxdb2;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Tensai
 */
@Data
@Slf4j
public class InfluxDataEntity {

    private Long time;

    private Map<String, Object> tags = new HashMap<>();

    private Map<String, Object> fields = new HashMap<>();

    public InfluxDataEntity(Long time) {
        this.time = time;
    }

    public Map<String, Object> getAll() {
        Map<String, Object> map = new HashMap<>(16);
        map.putAll(tags);
        map.putAll(fields);
        map.put("timestamp", time);
        return map;
    }
}
