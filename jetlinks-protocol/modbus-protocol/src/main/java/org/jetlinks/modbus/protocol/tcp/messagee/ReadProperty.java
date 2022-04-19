package org.jetlinks.modbus.protocol.tcp.messagee;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.HexUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.property.ReportPropertyMessage;
import org.jetlinks.modbus.protocol.tcp.ModbusDeviceMessage;
import org.jetlinks.modbus.protocol.tcp.TcpPayload;
import org.jetlinks.modbus.protocol.tcp.model.ModbusProductModelView;
import org.jetlinks.modbus.protocol.tcp.model.ProductModelEnum;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Tensai
 */
@Getter
@Slf4j
public class ReadProperty implements TcpPayload, ModbusDeviceMessage {

    private final Map<String, Object> properties = new HashMap<>(8);
    /**
     * 子设备ID
     */
    private long deviceId;
    /**
     * 功能码
     */
    private short funCode;

    @Override
    public void fromBytes(byte[] bytes, int offset, String deviceId) {
        log.debug("from byte - {}", HexUtil.encodeHexStr(bytes));
        this.deviceId = Long.parseLong(String.valueOf(bytes[0]));
        funCode = Short.parseShort(String.valueOf(bytes[1]));
        List<ModbusProductModelView> list = ProductModelEnum.INSTANCE.getModbusProductModelList(deviceId + "-" + this.deviceId);
        if (list == null) {
            log.error("找不到【{}】设备对应的物模型", deviceId);
            return;
        }
//        log.info("Modbus 模型 - {}", JSON.toJSONString(list));
        list.forEach(it -> {
            int length = it.getLength();
            byte[] byteValue = new byte[length];
            System.arraycopy(bytes, it.getOffset(), byteValue, 0, length);
//            log.info("value - {}", HexUtil.encodeHexStr(byteValue));
            // 解算字节码
            Number value;
            // TODO 有符号 无符号
            // TODO 前置脚本
            // 默认小端序【hutool】其实实际上是大端序
            switch (length) {
                case 1:
                    value = Convert.byteToUnsignedInt(byteValue[0]);
                    break;
                case 2:
                    value = Convert.bytesToShort(byteValue);
                    break;
                case 4:
                    value = Convert.bytesToInt(byteValue);
                    break;
                case 8:
                    value = Convert.bytesToLong(byteValue);
                    break;
                default:
                    value = 0;
                    break;
            }
            value = value.longValue() * it.getRatio();
            // 映射物模型指标类型
            switch (it.getDataType()) {
                case FLOAT:
                    properties.put(it.getMetricCode(), value.floatValue());
                    break;
                case INTEGER:
                    value = value.intValue();
                    properties.put(it.getMetricCode(), value.intValue());
                    break;
                case BOOLEAN:
                    properties.put(it.getMetricCode(), value.intValue() == 1);
                    break;
                case LONG:
                    properties.put(it.getMetricCode(), value.longValue());
                    break;
                case SHORT:
                    properties.put(it.getMetricCode(), value.shortValue());
                    break;
                case DOUBLE:
                    properties.put(it.getMetricCode(), value.doubleValue());
                    break;
                default:
                    properties.put(it.getMetricCode(), 0);
                    break;
            }
            // TODO 后置脚本

        });
//        byte[] humidityBytes = new byte[2];
//        System.arraycopy(bytes, 3, humidityBytes, 0, 2);
//        humidity = (Convert.bytesToShort(humidityBytes)) / 10.0;
//        byte[] temperatureBytes = new byte[2];
//        System.arraycopy(bytes, 5, temperatureBytes, 0, 2);
//        temperature = (Convert.bytesToShort(temperatureBytes)) / 10.0;
    }

    /**
     * 转成 deviceMessage
     *
     * @return deviceMessage
     */
    @Override
    public DeviceMessage toDeviceMessage() {
        ReportPropertyMessage deviceMessage = new ReportPropertyMessage();
        deviceMessage.setProperties(getProperties());
        deviceMessage.setDeviceId("2-" + deviceId + "");
        return deviceMessage;
    }

//    private Map<String, Object> getProperties() {
//        Map<String, Object> properties = new HashMap<>();
//        properties.put("humidity", humidity);
//        properties.put("temperature", temperature);
//        return properties;
//    }
}
