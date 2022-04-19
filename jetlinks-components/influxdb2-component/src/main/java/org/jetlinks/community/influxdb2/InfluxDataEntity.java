package org.jetlinks.community.influxdb2;

import com.influxdb.query.FluxRecord;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;

/**
 * @author Tensai
 */
@Data
@Slf4j
public class InfluxDataEntity {

    private Long time;

//    private Map<String, Object> tags = new HashMap<>();

//    private Map<String, Object> fields = new HashMap<>();

    private Map<String, Object> values;

    public InfluxDataEntity(Long time) {
        this.time = time;
    }

//    public Map<String, Object> getAll() {
//        Map<String, Object> map = new HashMap<>(16);
//        map.putAll(tags);
//        map.putAll(fields);
//        map.put("timestamp", time);
//        return map;
//    }

    public static InfluxDataEntity fluxRecordExchange(FluxRecord fluxRecord) {
        InfluxDataEntity influxDataEntity = new InfluxDataEntity(
            Objects.requireNonNull(fluxRecord.getTime()).toEpochMilli()
        );
        influxDataEntity.values = fluxRecord.getValues();
        influxDataEntity.values.put("createTime", Objects.requireNonNull(fluxRecord.getTime()).toEpochMilli());
        influxDataEntity.values.put("timestamp", Objects.requireNonNull(fluxRecord.getTime()).toEpochMilli());
        return influxDataEntity;
    }

    // 忽略字段
//    private static List<String> ignoreList = new ArrayList<String>(){{
//        add("result");
//        add("table");
//        add("_start");
//        add("_stop");
//        add("_time");
//        add("_measurement");
//    }};
}
