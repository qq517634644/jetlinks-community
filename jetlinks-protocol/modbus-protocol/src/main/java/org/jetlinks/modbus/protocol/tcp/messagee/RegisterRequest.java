package org.jetlinks.modbus.protocol.tcp.messagee;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.modbus.protocol.tcp.TcpPayload;

/**
 * @author Tensai
 */
@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class RegisterRequest implements TcpPayload {

    private String prefix;
    private long dtuId;

    @Override
    public byte[] toBytes() {
        return new byte[0];
    }

    @Override
    public void fromBytes(byte[] bytes, int offset, String deviceId) {
        String message = new String(bytes);
        String[] data = message.split("\\|\\|");
        prefix = data[0];
        dtuId = Long.parseLong(data[1]);
    }

    @Override
    public String toString() {
        return "RegisterRequest{" +
            "prefix='" + prefix + '\'' +
            ", dtuId=" + dtuId +
            '}';
    }
}
