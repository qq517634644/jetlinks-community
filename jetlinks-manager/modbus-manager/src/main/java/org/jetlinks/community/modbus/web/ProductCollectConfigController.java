package org.jetlinks.community.modbus.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.community.modbus.entity.ProductCollectConfig;
import org.jetlinks.community.modbus.service.LocalProductCollectConfigService;
import org.jetlinks.core.message.codec.DeviceMessageCodec;
import org.jetlinks.core.message.codec.MessageCodecDescription;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.core.server.session.DeviceSessionManager;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;


/**
 * @author Tensai
 */
@RestController
@RequestMapping({"/product-collect-config", "/product/collect/config"})
@Authorize(ignore = true)
@Resource(id = "product-collect-config", name = "modbus采集-产品配置")
@Slf4j
@Tag(name = "modbus采集-产品配置")
public class ProductCollectConfigController implements ReactiveServiceCrudController<ProductCollectConfig, String> {

    @Getter
    private final LocalProductCollectConfigService service;

    @Getter
    private final DeviceSessionManager deviceSessionManager;

    public ProductCollectConfigController(LocalProductCollectConfigService service, DeviceSessionManager deviceSessionManager) {
        this.service = service;
        this.deviceSessionManager = deviceSessionManager;
    }


    @RequestMapping("test")
    public Flux<String> test() {
        return deviceSessionManager.getAllSession().map(DeviceSession::getDeviceId);
    }

    @RequestMapping("test1")
    public Mono<String> test1() {
        DeviceSession deviceSession = deviceSessionManager.getSession("2");
        assert deviceSession != null;
        return Objects.requireNonNull(deviceSession.getOperator())
            .getProtocol()
            .flatMap(it -> it.getMessageCodec(deviceSession.getTransport()))
            .flatMap(DeviceMessageCodec::getDescription)
            .map(MessageCodecDescription::getDescription);
    }
}
