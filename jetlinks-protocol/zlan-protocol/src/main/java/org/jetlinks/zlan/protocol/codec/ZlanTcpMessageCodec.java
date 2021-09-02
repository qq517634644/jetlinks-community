package org.jetlinks.zlan.protocol.codec;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
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
import org.jetlinks.zlan.protocol.message.MessageType;
import org.jetlinks.zlan.protocol.message.ZlanMessagePack;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Tensai
 */
@Slf4j
public class ZlanTcpMessageCodec extends BaseMessageCodec implements DeviceMessageCodec {
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
//
        FromDeviceMessageContext ctx = ((FromDeviceMessageContext) context);
        String payload = new String(context.getMessage().payloadAsBytes());
        ZlanMessagePack message;
        try {
            if (log.isDebugEnabled()) {
                log.debug("zlan tcp message: {}", payload);
            }
            message = JSON.parseObject(payload, ZlanMessagePack.class);
        } catch (Exception e) {
            return Mono.error(e);
        }
        DeviceSession session = ctx.getSession();
        // 认证
        if (session.getOperator() == null) {
            return auth(context, session, message);
        } else {
            // 心跳
            if (MessageType.HEARTBEAT == message.getType()) {
                session.ping();
                return Flux.empty();
            } else if (MessageType.REPORT == message.getType()) {
                // 上报
                return report(message);
            }
        }
        return Mono.empty();
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
        EncodedMessage encodedMessage;
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
        return Mono.just(encodedMessage);
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
                return "zlan";
            }

            @Nullable
            @Override
            public ConfigMetadata getConfigMetadata() {
                return null;
            }
        });
    }

}
