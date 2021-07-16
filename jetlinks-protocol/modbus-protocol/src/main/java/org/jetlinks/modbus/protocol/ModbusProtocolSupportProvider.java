package org.jetlinks.modbus.protocol;

import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.defaults.CompositeProtocolSupport;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.core.spi.ProtocolSupportProvider;
import org.jetlinks.core.spi.ServiceContext;
import org.jetlinks.modbus.protocol.tcp.ModbusTcpMessageCodec;
import org.jetlinks.supports.official.JetLinksDeviceMetadataCodec;
import reactor.core.publisher.Mono;

/**
 * @author Tensai
 */
public class ModbusProtocolSupportProvider implements ProtocolSupportProvider {

    private static final DefaultConfigMetadata TCP_CONFIG = new DefaultConfigMetadata(
        "TCP认证配置"
        , "")
        .add("tcp_auth_key", "key", "TCP认证KEY", StringType.GLOBAL);

    /**
     * 创建协议支持
     *
     * @param context 上下文
     * @return 协议支持
     */
    @Override
    public Mono<? extends ProtocolSupport> create(ServiceContext context) {
        CompositeProtocolSupport support = new CompositeProtocolSupport();
        support.setId("modbus");
        support.setName("modbus");
        support.setDescription("modbus");
        //固定为JetLinksDeviceMetadataCodec
        support.setMetadataCodec(new JetLinksDeviceMetadataCodec());

        //TCP 演示协议
        {
            ModbusTcpMessageCodec codec = new ModbusTcpMessageCodec();
            support.addMessageCodecSupport(DefaultTransport.TCP, () -> Mono.just(codec));
            support.addMessageCodecSupport(DefaultTransport.TCP_TLS, () -> Mono.just(codec));
            support.addConfigMetadata(DefaultTransport.TCP, TCP_CONFIG);
            support.addConfigMetadata(DefaultTransport.TCP_TLS, TCP_CONFIG);

        }
        return Mono.just(support);
    }
}
