package org.jetlinks.modbus.protocol.tcp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.modbus.protocol.tcp.messagee.Heartbeat;
import org.jetlinks.modbus.protocol.tcp.messagee.ReadProperty;
import org.jetlinks.modbus.protocol.tcp.messagee.RegisterRequest;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * 消息类型
 *
 * @author Tensai
 */
@AllArgsConstructor
@Getter
public enum MessageType {
    /**
     *
     */
    REGISTER_REQ("设备注册", RegisterRequest::new),
    ERROR("设备注册", RegisterRequest::new),
    HEARTBEAT("设备心跳", Heartbeat::new),
    READ_PROPERTY("读取设备属性", ReadProperty::new);

    private final String text;

    private final Supplier<TcpPayload> payloadSupplier;

    /**
     * 获取消息类型
     *
     * @param payload 消息负载
     * @return 消息类型
     */
    public static Optional<MessageType> of(byte[] payload) {
        byte type = payload[0];
        switch (type) {
            case 'E':
                return Optional.of(MessageType.HEARTBEAT);
            case 'F':
                return Optional.of(MessageType.REGISTER_REQ);
            default:
                return Optional.of(MessageType.READ_PROPERTY);
        }
    }

    public TcpPayload read(byte[] payload, int offset, String deviceId) {
        TcpPayload tcpPayload = payloadSupplier.get();
        tcpPayload.fromBytes(payload, offset, deviceId);
        return tcpPayload;
    }

    public byte[] toBytes(TcpPayload data) {
        if (data == null) {
            return new byte[0];
        }
        return data.toBytes();
    }
}
