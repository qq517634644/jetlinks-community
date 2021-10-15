package org.jetlinks.zlan.protocol.codec;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONValidator;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.message.DeviceOnlineMessage;
import org.jetlinks.core.message.Message;
import org.jetlinks.core.message.codec.*;
import org.jetlinks.core.message.function.FunctionInvokeMessage;
import org.jetlinks.core.message.property.ReadPropertyMessage;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.jetlinks.core.message.property.ReportPropertyMessage;
import org.jetlinks.core.message.property.WritePropertyMessage;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.zlan.protocol.message.MessageType;
import org.jetlinks.zlan.protocol.message.ZlanMessagePack;
import org.jetlinks.zlan.protocol.temp.DeviceProperties;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        JSONValidator validator = JSONValidator.from(payload);
        if (!validator.validate()) {
            log.info("payload validate: {}", payload);
            return Mono.empty();
        }
        ZlanMessagePack message;
        try {
            message = JSON.parseObject(payload, ZlanMessagePack.class);
        } catch (Exception e) {
            log.error("zlan mqtt message: {}", payload);
            return Mono.error(e);
        }
        if (message.getType() == MessageType.OFFLINE) {
            return Mono.empty();
//            DeviceOfflineMessage deviceOfflineMessage = new DeviceOfflineMessage();
//            deviceOfflineMessage.setDeviceId(message.getId());
//            return Mono.just(deviceOfflineMessage);
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
        log.info("功能测试 - {}", JSON.toJSONString(context.getMessage().getMessageType()));
        Message message = context.getMessage();
        EncodedMessage encodedMessage = null;
        log.info("推送设备消息，消息ID：{}", message.getMessageId());
        // 获取设备属性
        if (message instanceof ReadPropertyMessage) {
            log.info("ReadPropertyMessage");
            ReadPropertyMessage readPropertyMessage = (ReadPropertyMessage) message;
            log.info(JSON.toJSONString(readPropertyMessage));

            String deviceId = readPropertyMessage.getDeviceId();
            String messageId = readPropertyMessage.getMessageId();
            List<String> properties = readPropertyMessage.getProperties();

            ReadPropertyMessageReply readPropertyMessageReply = new ReadPropertyMessageReply();
            readPropertyMessageReply.setDeviceId(deviceId);
            readPropertyMessageReply.setMessageId(messageId);
            Map<String, Object> propertiesMap = new HashMap<>(8);
            Map<String, Long> propertySourceTimes = new HashMap<>(8);
            Map<String, String> propertyStates = new HashMap<>(8);
            DeviceProperties.LAST.getAllProperties(deviceId).forEach((k, v) -> {
                if (properties.contains(k)) {
                    propertiesMap.put(k, v);
                    propertySourceTimes.put(k, System.currentTimeMillis());
                    propertyStates.put(k, "success");
                }
            });
            readPropertyMessageReply.setProperties(propertiesMap);
            readPropertyMessageReply.setPropertySourceTimes(propertySourceTimes);
            readPropertyMessageReply.setPropertyStates(propertyStates);
            log.info(JSON.toJSONString(readPropertyMessage));
            DeviceProperties.LAST.removeAllProperties(deviceId);
            return context
                .reply(readPropertyMessageReply)
                .then(Mono.empty());
        }
        //修改设备属性
        if (message instanceof WritePropertyMessage) {
            log.info("WritePropertyMessage");
        }
        // 设备上报属性
        if (message instanceof ReportPropertyMessage) {
            log.info("ReportPropertyMessage");
            ReportPropertyMessage reportPropertyMessage = (ReportPropertyMessage) message;
            log.info(JSON.toJSONString(reportPropertyMessage));
        }

        if (message instanceof FunctionInvokeMessage) {
            log.info("Function --> {}", ((FunctionInvokeMessage) message).getInputs());
        }
        return Mono.empty();
//        return Mono.just(encodedMessage).switchIfEmpty(Mono.empty());
    }
}
