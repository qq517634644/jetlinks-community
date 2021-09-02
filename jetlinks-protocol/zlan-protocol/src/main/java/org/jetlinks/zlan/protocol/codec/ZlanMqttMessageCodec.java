package org.jetlinks.zlan.protocol.codec;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.message.DeviceOfflineMessage;
import org.jetlinks.core.message.DeviceOnlineMessage;
import org.jetlinks.core.message.Message;
import org.jetlinks.core.message.codec.*;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.zlan.protocol.message.MessageType;
import org.jetlinks.zlan.protocol.message.ZlanMessagePack;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

/**
 * @author Tensai
 */
@Slf4j
public class ZlanMqttMessageCodec extends BaseMessageCodec implements DeviceMessageCodec {
    @Override
    public Transport getSupportTransport() {
        return DefaultTransport.MQTT;
    }

    @Nonnull
    @Override
    public Publisher<? extends Message> decode(@Nonnull MessageDecodeContext context) {
        FromDeviceMessageContext ctx = ((FromDeviceMessageContext) context);
        String payload = new String(context.getMessage().payloadAsBytes());
        ZlanMessagePack message;
        try {
            log.info("zlan mqtt message: {}", payload);
            message = JSON.parseObject(payload, ZlanMessagePack.class);
        } catch (Exception e) {
            log.error("zlan mqtt message: {}", payload);
            return Mono.error(e);
        }
        if (message.getType() == MessageType.OFFLINE) {
            DeviceOfflineMessage deviceOfflineMessage = new DeviceOfflineMessage();
            deviceOfflineMessage.setDeviceId(message.getId());
            return Mono.just(deviceOfflineMessage);
        }
        DeviceSession session = ctx.getSession();
        // 认证
        if (session.getOperator() == null) {
            DeviceOnlineMessage onlineMessage = new DeviceOnlineMessage();
            onlineMessage.setDeviceId(message.getId());
            onlineMessage.setTimestamp(System.currentTimeMillis());
            log.info("online - {}", onlineMessage);
            return Mono.just((Message) onlineMessage);
        }
        return report(message);
    }

    @Nonnull
    @Override
    public Publisher<EncodedMessage> encode(MessageEncodeContext context) {
        Message message = context.getMessage();
        return Mono.empty();

    }
}
