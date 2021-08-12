package org.jetlinks.modbus.protocol.tcp;

import cn.hutool.core.util.HexUtil;
import com.alibaba.fastjson.JSON;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.jetlinks.core.Value;
import org.jetlinks.core.message.*;
import org.jetlinks.core.message.codec.*;
import org.jetlinks.core.message.codec.http.HttpExchangeMessage;
import org.jetlinks.core.message.function.FunctionInvokeMessage;
import org.jetlinks.core.message.function.FunctionInvokeMessageReply;
import org.jetlinks.core.message.interceptor.DeviceMessageDecodeInterceptor;
import org.jetlinks.core.message.interceptor.DeviceMessageEncodeInterceptor;
import org.jetlinks.core.message.property.ReadPropertyMessage;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.jetlinks.core.message.property.ReportPropertyMessage;
import org.jetlinks.core.message.property.WritePropertyMessage;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.modbus.protocol.tcp.command.CommandEnum;
import org.jetlinks.modbus.protocol.tcp.messagee.ErrorMessage;
import org.jetlinks.modbus.protocol.tcp.messagee.Heartbeat;
import org.jetlinks.modbus.protocol.tcp.messagee.ReadProperty;
import org.jetlinks.modbus.protocol.tcp.messagee.RegisterRequest;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;

/**
 * @author Tensai
 */
@Slf4j
public class ModbusTcpMessageCodec implements DeviceMessageCodec {
    /**
     * @return 返回支持的传输协议
     * @see DefaultTransport
     */
    @Override
    public Transport getSupportTransport() {
        return DefaultTransport.TCP;
    }

    /**
     * 在服务器收到设备或者网络组件中发来的消息时，会调用协议包中的此方法来进行解码，
     * 将数据{@link EncodedMessage}转为平台的统一消息{@link DeviceMessage}
     *
     * <pre>
     * //解码并返回一个消息
     * public Mono&lt;DeviceMessage&gt; decode(MessageDecodeContext context){
     *
     *  EncodedMessage message = context.getMessage();
     *  byte[] payload = message.payloadAsBytes();//上报的数据
     *
     *  DeviceMessage message = doEncode(payload);
     *
     *  return Mono.just(message);
     * }
     *
     * //解码并返回多个消息
     * public Flux&lt;DeviceMessage&gt; decode(MessageDecodeContext context){
     *
     *  EncodedMessage message = context.getMessage();
     *  byte[] payload = message.payloadAsBytes();//上报的数据
     *
     *  List&lt;DeviceMessage&gt; messages = doEncode(payload);
     *
     *  return Flux.fromIterable(messages);
     * }
     *
     * //解码,回复设备并返回一个消息
     * public Mono&lt;DeviceMessage&gt; decode(MessageDecodeContext context){
     *
     *  EncodedMessage message = context.getMessage();
     *  byte[] payload = message.payloadAsBytes();//上报的数据
     *
     *  DeviceMessage message = doEncode(payload); //解码
     *
     *  FromDeviceMessageContext ctx = (FromDeviceMessageContext)context;
     *
     *  EncodedMessage msg = createReplyMessage(); //构造回复
     *
     *  return ctx
     *     .getSession()
     *     .send(msg) //发送回复
     *     .thenReturn(message);
     * }
     *
     * </pre>
     *
     * @param context 消息上下文
     * @return 解码结果
     * @see MqttMessage
     * @see HttpExchangeMessage
     * @see CoapExchangeMessage
     * @see DeviceMessageReply
     * @see ReadPropertyMessageReply
     * @see FunctionInvokeMessageReply
     * @see ChildDeviceMessageReply
     * @see DeviceOnlineMessage
     * @see DeviceOfflineMessage
     * @see DeviceMessageDecodeInterceptor
     */
    @Nonnull
    @Override
    public Publisher<? extends Message> decode(@Nonnull MessageDecodeContext context) {
//        log.debug("收到消息：");
        return Mono.defer(() -> {
            FromDeviceMessageContext ctx = ((FromDeviceMessageContext) context);
            byte[] payload = context.getMessage().payloadAsBytes();
            String deviceId = context.getDevice() != null ? context.getDevice().getDeviceId() : null;
            if (log.isDebugEnabled()) {
//                log.debug("handle tcp message:\n{}", Hex.encodeHexString(payload));
            }
            ModbusTcpMessage message;
            try {
                message = ModbusTcpMessage.of(payload, deviceId);
                if (log.isDebugEnabled()) {
                    log.debug("decode tcp message: {} - {}", Hex.encodeHexString(payload), message);
                }
            } catch (Exception e) {
                log.warn("decode tcp message error:[{}]", Hex.encodeHexString(payload), e);
                return Mono.error(e);
            }
            DeviceSession session = ctx.getSession();
            // 认证
            if (session.getOperator() == null) {
                //设备没有认证就发送了消息
                if (message.getType() != MessageType.REGISTER_REQ) {
                    log.warn("tcp session[{}], unauthorized.", session.getId());
                    return session
                        .send(EncodedMessage.simple(
                            ModbusTcpMessage.of(MessageType.ERROR, ErrorMessage.of(TcpStatus.UN_AUTHORIZED)).toByteBuf()))
                        .then(Mono.fromRunnable(session::close));
                }
                RegisterRequest request = ((RegisterRequest) message.getData());
                deviceId = deviceId == null ? getDtuId(request.getDtuId()) : null;
                String finalDeviceId = deviceId;
                return context
                    .getDevice(getDtuId(request.getDtuId()))
                    .flatMap(operator -> operator.getConfig("tcp_auth_key")
                        .map(Value::asString)
                        .filter(key -> {
                            log.info("auth , {} - {}", request.getPrefix(), key);
                            return Arrays.equals(request.getPrefix().getBytes(), key.getBytes());
                        })
                        .flatMap(msg -> {
                            //认证通过
                            DeviceOnlineMessage onlineMessage = new DeviceOnlineMessage();
                            onlineMessage.setDeviceId(finalDeviceId);
                            onlineMessage.setTimestamp(System.currentTimeMillis());
                            log.debug("online - {}", onlineMessage);
                            return Mono.just(onlineMessage);
                        }))
                    //为空可能设备不存在或者没有配置tcp_auth_key,响应错误信息.
                    .switchIfEmpty(Mono.defer(Mono::empty));
            }
            // 心跳
            if (message.getType() == MessageType.HEARTBEAT) {
                Heartbeat heartbeat = ((Heartbeat) message.getData());
//                log.debug("HEARTBEAT - {} - {}", heartbeat.getDtuId(), heartbeat.getPrefix());
//                return session
//                    .send(EncodedMessage.simple(Unpooled.wrappedBuffer(bytes[RandomUtil.randomInt(0, 5)])))
//                    .flatMap(it -> {
//                        log.debug("发送 - {}", it);
                return Mono.fromRunnable(session::ping);
//                    });
            }
            // 上报
            if (message.getData() instanceof ReadProperty) {
                deviceId = session.getDeviceId();
                ChildDeviceMessage childDeviceMessage = new ChildDeviceMessage();
                childDeviceMessage.setDeviceId(deviceId);
                DeviceMessage children = ((ReadProperty) message.getData()).toDeviceMessage();
                childDeviceMessage.setChildDeviceMessage(children);
                childDeviceMessage.setChildDeviceId(children.getDeviceId());
                log.debug("回复 - 发送dtu设备回复 - {}", JSON.toJSONString(childDeviceMessage));

                return sendMessage(deviceId, session).flatMap(it -> Mono.justOrEmpty(childDeviceMessage));
            }
            return Mono.just(new ReportPropertyMessage());
        });
    }

    /**
     * 编码,将消息进行编码,用于发送到设备端.
     * <p>
     * 平台在发送指令给设备时,会调用协议包中设置的此方法,将平台消息{@link DeviceMessage}转为设备能理解的消息{@link EncodedMessage}.
     * <p>
     * 例如:
     * <pre>
     *
     * //返回单个消息给设备,多个使用Flux&lt;EncodedMessage&gt;作为返回值
     * public Mono&lt;EncodedMessage&gt; encode(MessageEncodeContext context){
     *
     *     return Mono.just(doEncode(context.getMessage()));
     *
     * }
     * </pre>
     *
     * <pre>
     * //忽略发送给设备,直接返回结果给指令发送者
     * public Mono&lt;EncodedMessage&gt; encode(MessageEncodeContext context){
     *    DeviceMessage message = (DeviceMessage)context.getMessage();
     *
     *    return context
     *      .reply(handleMessage(message)) //返回结果给指令发送者
     *      .then(Mono.empty())
     * }
     *
     * </pre>
     * <p>
     * 如果要串行发送数据,可以参考使用{@link org.jetlinks.core.utils.ParallelIntervalHelper}工具类
     *
     * @param context 消息上下文
     * @return 编码结果
     * @see MqttMessage
     * @see Message
     * @see ReadPropertyMessage 读取设备属性
     * @see WritePropertyMessage 修改设备属性
     * @see FunctionInvokeMessage 调用设备功能
     * @see ChildDeviceMessage 子设备消息
     * @see DeviceMessageEncodeInterceptor
     * @see org.jetlinks.core.utils.ParallelIntervalHelper
     */
    @Nonnull
    @Override
    public Publisher<? extends EncodedMessage> encode(@Nonnull MessageEncodeContext context) {
        log.info("功能测试 - {}", JSON.toJSONString(context));
        Message message = context.getMessage();
        EncodedMessage encodedMessage = null;
        log.info("推送设备消息，消息ID：{}", message.getMessageId());
        // 获取设备属性
        if (message instanceof ReadPropertyMessage) {
            log.info("ReportPropertyMessage");
        }
        //修改设备属性
        if (message instanceof WritePropertyMessage) {
            log.info("ReportPropertyMessage");
        }
        // 设备上报属性
        if (message instanceof ReportPropertyMessage) {
            log.info("ReportPropertyMessage");
        }
        encodedMessage = EncodedMessage.simple(Unpooled.wrappedBuffer(new byte[]{0x09, 0x03, 0x00, 0x00, 0x00, 0x02, (byte) 0xc5, 0x43}));
        return encodedMessage != null ? Mono.just(encodedMessage) : Mono.empty();
    }

    /**
     * 获取协议描述
     *
     * @return 协议描述
     */
    @Override
    public Mono<? extends MessageCodecDescription> getDescription() {
        return Mono.just(new MessageCodecDescription() {
            @Override
            public String getDescription() {
                return "Modbus";
            }

            @Nullable
            @Override
            public ConfigMetadata getConfigMetadata() {
                return null;
            }
        });
    }

    private String getDtuId(long dtuId) {
        return String.valueOf(dtuId);
    }


    private Mono<Boolean> sendMessage(String dtuId, DeviceSession session) {
        CommandEnum.INSTANCE.timeOutOrComplete(dtuId);
        byte[] code = CommandEnum.INSTANCE.pollCommand(dtuId);
        if (code != null) {
            return session
                .send(EncodedMessage.simple(Unpooled.wrappedBuffer(code)))
                .map(it -> {
                    log.debug("连续 - 发送dtu[{}]指令[{}]状态 - {}", dtuId, HexUtil.encodeHexStr(code), it);
                    return it;
                });
        } else {
            return Mono.empty();
        }
    }
}
