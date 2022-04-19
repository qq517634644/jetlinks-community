package org.jetlinks.modbus.protocol.tcp;

import org.jetlinks.core.message.DeviceMessage;

/**
 * @author Tensai
 */
public interface ModbusDeviceMessage {

    /**
     * 转成 deviceMessage
     *
     * @return deviceMessage
     */
    DeviceMessage toDeviceMessage();

}
