package org.jetlinks.modbus.protocol.tcp.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author Tensai
 */

public enum ProductModelEnum {
    /**
     *
     */
    INSTANCE;

    private final Map<String, String> deviceIdMapProductId = new ConcurrentHashMap<>();
    private Map<String, List<ModbusProductModel>> map = null;
//    {{
//        put("2-1", "modbus-1-001-chirld");
//        put("2-2", "YWCGQM");
//        put("2-3", "HWXCGQ");
//        put("2-4", "");
//        put("2-5", "");
//        put("2-6", "");
//        put("2-9", "WSDCGQM");
//    }};

    public List<ModbusProductModel> getModel(String productId) {
        return getMap().get(productId);
    }

    public List<ModbusProductModel> get(String deviceId) {
        return getModel(getProductId(deviceId));
    }

    public String getProductId(String deviceId) {
        return deviceIdMapProductId.get(deviceId);
    }

    private Map<String, List<ModbusProductModel>> getMap() {
        if (map == null) {
            map = getModelList().stream().collect(Collectors.groupingBy(ModbusProductModel::getProductId));
        }
        return map;
    }

    private List<ModbusProductModel> getModelList() {
        return new ArrayList<ModbusProductModel>() {{
            add(ModbusProductModel.of("modbus-1-001-chirld", "humidity", 0.1, 3, 2, DataType.DOUBLE));
            add(ModbusProductModel.of("modbus-1-001-chirld", "temperature", 0.1, 5, 2, DataType.DOUBLE));

            add(ModbusProductModel.of("WSDCGQM", "humidity", 0.1, 3, 2, DataType.DOUBLE));
            // 负数给出的是补码
            add(ModbusProductModel.of("WSDCGQM", "temperature", 0.1, 5, 2, DataType.DOUBLE));

            add(ModbusProductModel.of("YWCGQM", "alarm", 1.0, 3, 2, DataType.BOOLEAN));

            add(ModbusProductModel.of("HWXCGQM", "alarm", 1.0, 3, 2, DataType.BOOLEAN));
        }};
    }

    public void setDeviceIdMapProductId(String deviceId, String productId) {
        deviceIdMapProductId.put(deviceId, productId);
    }
}
