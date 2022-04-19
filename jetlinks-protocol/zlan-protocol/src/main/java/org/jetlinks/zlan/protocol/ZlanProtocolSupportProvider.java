package org.jetlinks.zlan.protocol;

import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.defaults.CompositeProtocolSupport;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.types.PasswordType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.core.spi.ProtocolSupportProvider;
import org.jetlinks.core.spi.ServiceContext;
import org.jetlinks.supports.official.JetLinksDeviceMetadataCodec;
import org.jetlinks.zlan.protocol.codec.ZlanMqttMessageCodec;
import org.jetlinks.zlan.protocol.codec.ZlanTcpMessageCodec;
import reactor.core.publisher.Mono;

/**
 * ZLan 设备协议支持
 *
 * @author Tensai
 */
public class ZlanProtocolSupportProvider implements ProtocolSupportProvider {

    private static final DefaultConfigMetadata TCP_CONFIG = new DefaultConfigMetadata(
        "TCP认证配置"
        , "")
        .add("tcp_auth_key", "key", "TCP认证KEY", StringType.GLOBAL);

    private static final DefaultConfigMetadata MQTT_CONFIG = new DefaultConfigMetadata(
        "MQTT认证配置"
        , "")
        .add("username", "username", "MQTT用户名", StringType.GLOBAL)
        .add("password", "password", "MQTT密码", PasswordType.GLOBAL);

    /**
     * 创建协议支持
     *
     * @param context 上下文
     * @return 协议支持
     */
    @Override
    public Mono<? extends ProtocolSupport> create(ServiceContext context) {
        CompositeProtocolSupport support = new CompositeProtocolSupport();
        support.setId("zlan");
        support.setName("zlan");
        support.setDescription("zlan");
        //固定为JetLinksDeviceMetadataCodec

        support.setMetadataCodec(new JetLinksDeviceMetadataCodec());

        //TCP
        {
            ZlanTcpMessageCodec codec = new ZlanTcpMessageCodec();
            support.addMessageCodecSupport(DefaultTransport.TCP, () -> Mono.just(codec));
            support.addMessageCodecSupport(DefaultTransport.TCP_TLS, () -> Mono.just(codec));
            support.addConfigMetadata(DefaultTransport.TCP, TCP_CONFIG);
            support.addConfigMetadata(DefaultTransport.TCP_TLS, TCP_CONFIG);

        }

        {
            ZlanMqttMessageCodec codec = new ZlanMqttMessageCodec();
            support.addMessageCodecSupport(DefaultTransport.MQTT, () -> Mono.just(codec));
            support.addMessageCodecSupport(DefaultTransport.MQTT_TLS, () -> Mono.just(codec));
            support.addConfigMetadata(DefaultTransport.MQTT, MQTT_CONFIG);
            support.addConfigMetadata(DefaultTransport.MQTT_TLS, MQTT_CONFIG);
            support.addAuthenticator(DefaultTransport.MQTT, new ZlanAuthenticator());
            support.addAuthenticator(DefaultTransport.MQTT_TLS, new ZlanAuthenticator());
        }
        return Mono.just(support);
    }
}
