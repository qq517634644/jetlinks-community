package org.jetlinks.modbus.protocol.tcp.session;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import lombok.Setter;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.MqttMessage;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.server.session.DeviceSession;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Optional;

/**
 * @author Tensai
 */
public class ModbusDeviceSession implements DeviceSession {

    private final DeviceOperator deviceOperator;

    private final NetSocket netSocket;
    private final long connectTime = System.currentTimeMillis();
    private long lastPingTime = System.currentTimeMillis();
    @Setter
    private boolean closed;

    private long keepaliveTimeout = Duration.ofSeconds(30).toMillis();

    public ModbusDeviceSession(DeviceOperator deviceOperator, NetSocket netSocket) {
        this.deviceOperator = deviceOperator;
        this.netSocket = netSocket;
    }

    /**
     * @return 会话ID
     */
    @Override
    public String getId() {
        return getDeviceId();
    }

    /**
     * @return 设备ID
     */
    @Override
    public String getDeviceId() {
        return deviceOperator.getDeviceId();
    }

    /**
     * 获取设备操作对象,在类似TCP首次请求的场景下,返回值可能为<code>null</code>.
     * 可以通过判断此返回值是否为<code>null</code>,来处理首次连接的情况。
     *
     * @return void
     */
    @Nullable
    @Override
    public DeviceOperator getOperator() {
        return deviceOperator;
    }

    /**
     * @return 最近心跳时间
     */
    @Override
    public long lastPingTime() {
        return lastPingTime;
    }

    /**
     * @return 创建时间
     */
    @Override
    public long connectTime() {
        return connectTime;
    }

    /**
     * 发送消息给会话
     *
     * @param encodedMessage 消息
     * @return 是否成功
     * @see MqttMessage
     */
    @Override
    public Mono<Boolean> send(EncodedMessage encodedMessage) {
        return Mono.create(sink -> netSocket.write(Buffer.buffer(encodedMessage.getPayload()), async -> {
            if (async.succeeded()) {
                sink.success(true);
            } else {
                sink.error(async.cause());
            }
        }));
    }

    /**
     * 传输协议,比如MQTT,TCP等
     *
     * @return void
     */
    @Override
    public Transport getTransport() {
        return DefaultTransport.TCP;
    }

    /**
     * 关闭session
     */
    @Override
    public void close() {
        netSocket.close();
    }

    /**
     * 心跳
     *
     * @see DeviceSession#keepAlive()
     */
    @Override
    public void ping() {
        lastPingTime = System.currentTimeMillis();
    }

    /**
     * @return 会话是否存活
     */
    @Override
    public boolean isAlive() {
        return !closed && System.currentTimeMillis() - lastPingTime < keepaliveTimeout;
    }

    /**
     * 设置close回调
     *
     * @param call 回调
     */
    @Override
    public void onClose(Runnable call) {
    }

    @Override
    public Optional<InetSocketAddress> getClientAddress() {
        return Optional
            .ofNullable(netSocket.remoteAddress())
            .map(address -> new InetSocketAddress(address.host(), address.port()));
    }

    @Override
    public void setKeepAliveTimeout(Duration timeout) {
        keepaliveTimeout = timeout.toMillis();
    }
}
