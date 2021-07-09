package org.jetlinks.demo.protocol.tcp;

public interface TcpPayload {

    byte[] toBytes();

    void fromBytes(byte[] bytes, int offset);


}
