package org.jetlinks.modbus.protocol.tcp.messagee;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.modbus.protocol.tcp.TcpPayload;
import org.jetlinks.modbus.protocol.tcp.TcpStatus;

/**
 * @author Tensai
 */
@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class ErrorMessage implements TcpPayload {

    private TcpStatus status;

    @Override
    public byte[] toBytes() {
        return new byte[]{status.getStatus()};
    }

    @Override
    public void fromBytes(byte[] bytes, int offset, String deviceId) {
        status = TcpStatus.of(bytes[offset]).orElse(TcpStatus.UNKNOWN);
    }
}
