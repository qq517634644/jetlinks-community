package org.jetlinks.modbus.protocol.tcp;

/**
 * @author Tensai
 */
public interface TcpPayload {

    byte[] toBytes();

    /**
     * 原始负载转换
     *
     * @param bytes    原始负载
     * @param offset   偏移量
     * @param deviceId 设备ID
     */
    void fromBytes(byte[] bytes, int offset, String deviceId);


}
