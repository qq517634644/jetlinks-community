package org.jetlinks.modbus.protocol.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.utils.BytesUtils;

import javax.annotation.Nonnull;
import java.util.Arrays;

/**
 * demo tcp 报文协议格式
 * <p>
 * 第0字节为消息类型
 * 第1-4字节为消息体长度
 * 第5-n为消息体
 *
 * @author Tensai
 */
@Slf4j
@Getter
@Setter
@AllArgsConstructor(staticName = "of")
public class ModbusTcpMessage {

    private MessageType type;

    private TcpPayload data;

    public static ModbusTcpMessage of(byte[] payload, String deviceId) {
        MessageType type = MessageType.of(payload).orElseThrow(IllegalArgumentException::new);
        if (deviceId == null) {
            log.error("未获取到deviceId");
        }
        return ModbusTcpMessage.of(type, type.read(payload, 0, deviceId));
    }

    /**
     * 生成设备指令
     *
     * @return 设备指令
     */
    public ByteBuf toByteBuf() {
        return Unpooled.wrappedBuffer(toBytes());
    }

    @Nonnull
    private byte[] toBytes() {
        byte[] header = new byte[5];
        header[0] = (byte) type.ordinal();

        byte[] body = type.toBytes(data);
        int bodyLength = body.length;

        BytesUtils.intToLe(header, bodyLength, 1);

        if (bodyLength == 0) {
            return header;
        }
        byte[] data = Arrays.copyOf(header, bodyLength + 5);
        System.arraycopy(body, 0, data, 5, bodyLength);

        return data;
    }

    @Override
    public String toString() {
        return "TcpMessage{" +
            "type=" + type +
            ", data=" + data +
            '}';
    }
}
