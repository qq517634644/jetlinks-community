package org.jetlinks.zlan.protocol.codec;

import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.Value;
import org.jetlinks.core.message.ChildDeviceMessage;
import org.jetlinks.core.message.DeviceOfflineMessage;
import org.jetlinks.core.message.DeviceOnlineMessage;
import org.jetlinks.core.message.Message;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.MessageDecodeContext;
import org.jetlinks.core.message.property.ReportPropertyMessage;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.zlan.protocol.message.ZlanMessagePack;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;


/**
 * @author Tensai
 */
@Slf4j
public class BaseMessageCodec {


    /**
     * 设备认证
     *
     * @param context 消息上下文
     * @param session 设备session
     * @param message 设备消息
     * @return 认证结果
     */
    public Publisher<? extends Message> auth(MessageDecodeContext context, DeviceSession session, ZlanMessagePack message) {
        //设备没有认证就发送了消息
        String deviceId = message.getId();
        return context
            .getDevice(deviceId)
            .flatMap(operator -> operator
                .getConfig("tcp_auth_key")
                .map(Value::asString)
                .filter(key -> {
                    log.info("auth , {} - {}", message.getKey(), key);
                    return Arrays.equals(message.getKey().getBytes(), key.getBytes());
                })
                .flatMap(msg -> {
                    //认证通过
                    DeviceOnlineMessage onlineMessage = new DeviceOnlineMessage();
                    onlineMessage.setDeviceId(deviceId);
                    onlineMessage.setTimestamp(System.currentTimeMillis());
                    log.debug("online - {}", onlineMessage);
                    return Mono.just((Message) onlineMessage);
                })
            )
            //为空可能设备不存在或者没有配置tcp_auth_key,响应错误信息.
            .switchIfEmpty(session
                .send(EncodedMessage.simple(
                    Unpooled.wrappedBuffer("401".getBytes(StandardCharsets.UTF_8))
                ))
                .then(Mono.fromRunnable(session::close))
            )
            .concatWith(report(message));
    }


    /**
     * 属性上报
     *
     * @param message 设备消息
     * @return 结果
     */
    public Publisher<? extends Message> report(ZlanMessagePack message) {
        return Flux.fromIterable(message.getData())
            .filter(it -> !ChildrenDevice.STATUS_MAP.computeIfAbsent(it.getId(), k -> 0).equals(it.getStatus()))
            .flatMap(it -> {
                ChildrenDevice.STATUS_MAP.put(it.getId(), it.getStatus());
                // 上下线逻辑
                log.info("设备上下线 - {} - {}", it.getId(), it.getStatus());
                if (it.getStatus() == 1) {
                    DeviceOnlineMessage deviceOnlineMessage = new DeviceOnlineMessage();
                    deviceOnlineMessage.setDeviceId(it.getId());
                    return Flux.just(deviceOnlineMessage);
                } else {
                    DeviceOfflineMessage deviceOfflineMessage = new DeviceOfflineMessage();
                    deviceOfflineMessage.setDeviceId(it.getId());
                    return Flux.just(deviceOfflineMessage);
                }
            }).concatWith(Flux.fromIterable(message.getData())
                .filter(it -> it.getStatus() == 1)
                .map(it -> {
                    // 只发送在线设备的数据
                    ChildDeviceMessage childDeviceMessage = new ChildDeviceMessage();
                    childDeviceMessage.setDeviceId(message.getId());
                    childDeviceMessage.setChildDeviceId(it.getId());
                    ReportPropertyMessage deviceMessage = new ReportPropertyMessage();
                    deviceMessage.setDeviceId(it.getId());
                    deviceMessage.setProperties(it.getData());
                    childDeviceMessage.setChildDeviceMessage(deviceMessage);
                    return childDeviceMessage;
                }).map(it -> {
//                    log.info("report - {}", JSON.toJSONString(it));
                    return it;
                }));
    }
}
