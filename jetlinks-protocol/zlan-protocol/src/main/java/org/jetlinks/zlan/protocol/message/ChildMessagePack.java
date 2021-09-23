package org.jetlinks.zlan.protocol.message;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.zlan.protocol.temp.DeviceProperties;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 子设备数据
 *
 * @author Tensai
 */
@Slf4j
@Data
public class ChildMessagePack {

    private String id;

    private Integer status;

    private Map<String, Object> data;

    /**
     * 数据派生
     */
    public void deriveData() {
        if (Objects.nonNull(data)) {
            Map<String, Object> temp = new HashMap<>(4);
            data.forEach((k, v) -> {
                if (k.startsWith("total_")) {
                    Map<String, BigDecimal> lastMap = DeviceProperties.LAST.getTotalProperties(id);
                    if (lastMap != null) {
                        if (lastMap.get(k) != null) {
                            String stepKey = k.replace("total", "step");
                            BigDecimal stepValue = ((BigDecimal) v).subtract(lastMap.get(k));
                            temp.put(stepKey, stepValue);
                        }
                    }
                    DeviceProperties.LAST.setTotalProperties(id, k, (BigDecimal) v);
                }
            });
            data.putAll(temp);
            DeviceProperties.LAST.setAllProperties(id, data);
        }
    }
}
