package org.jetlinks.modbus.protocol.tcp.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 指令解析枚举类
 *
 * @author Tensai
 */
public enum ProductModelEnum {
    /**
     *
     */
    INSTANCE;

    /**
     * 设备ID映射产品ID 指令拼装和解析用到
     */
    private final Map<String, String> deviceIdMapProductId = new ConcurrentHashMap<>();

    /**
     * modbus解析模型
     */
    private final Map<String, List<ModbusProductModelView>> modbusParseModelMap = new ConcurrentHashMap<>();


    /**
     * put modelView
     *
     * @param view ModbusProductModelView
     */
    public void putModbusParseModelMap(ModbusProductModelView view) {
        modbusParseModelMap.computeIfAbsent(view.getProductId(), key -> new ArrayList<>()).add(view);
    }

    /**
     * 获取modbus模型
     *
     * @param productId 产品ID
     * @return modbus模型
     */
    private List<ModbusProductModelView> getModel(String productId) {
        return modbusParseModelMap.get(productId);
    }

    /**
     * 获取modbus模型
     *
     * @param deviceId 设备ID
     * @return modbus模型
     */
    public List<ModbusProductModelView> getModbusProductModelList(String deviceId) {
        return getModel(getProductId(deviceId));
    }

    /**
     * 获取产品ID
     *
     * @param deviceId 设备ID
     * @return 产品ID
     */
    public String getProductId(String deviceId) {
        return deviceIdMapProductId.get(deviceId);
    }
//
//    /**
//     * 获取产品-modbus模型映射
//     *
//     * @return 模型映射
//     */
//    @Deprecated
//    private Map<String, List<ModbusProductModelView>> getModbusProductModelMap() {
//        if (modbusParseModeMap == null) {
//            modbusParseModeMap = getModelList().stream().collect(Collectors.groupingBy(ModbusProductModelView::getProductId));
//        }
//        return modbusParseModeMap;
//    }
//
//    /**
//     * modbus模型
//     *
//     * @return modbus模型
//     */
//    @Deprecated
//    private List<ModbusProductModelView> getModelList() {
//        return new ArrayList<ModbusProductModelView>() {{
//            add(ModbusProductModelView.of("modbus-1-001-chirld", "humidity", 0.1, 3, 2, DataType.DOUBLE));
//            add(ModbusProductModelView.of("modbus-1-001-chirld", "temperature", 0.1, 5, 2, DataType.DOUBLE));
//
//            add(ModbusProductModelView.of("WSDCGQM", "humidity", 0.1, 3, 2, DataType.DOUBLE));
//            // 负数给出的是补码
//            add(ModbusProductModelView.of("WSDCGQM", "temperature", 0.1, 5, 2, DataType.DOUBLE));
//
//            add(ModbusProductModelView.of("YWCGQM", "alarm", 1.0, 3, 2, DataType.BOOLEAN));
//
//            add(ModbusProductModelView.of("HWXCGQM", "alarm", 1.0, 3, 2, DataType.BOOLEAN));
//        }};
//    }

    /**
     * 设置设备ID到产品ID的映射
     *
     * @param deviceId  设备ID
     * @param productId 产品ID
     */
    public void setDeviceIdMapProductId(String deviceId, String productId) {
        deviceIdMapProductId.put(deviceId, productId);
    }
}
