package org.jetlinks.community.modbus.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.community.modbus.entity.ProductCollectConfig;
import org.jetlinks.community.modbus.service.LocalProductCollectConfigService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * @author Tensai
 */
@RestController
@RequestMapping({"/product-collect-config", "/product/collect/config"})
@Authorize
@Resource(id = "product-collect-config", name = "modbus采集-产品配置")
@Slf4j
@Tag(name = "modbus采集-产品配置")
public class ProductCollectConfigController implements ReactiveServiceCrudController<ProductCollectConfig, String> {

    @Getter
    private final LocalProductCollectConfigService service;

    public ProductCollectConfigController(LocalProductCollectConfigService service) {
        this.service = service;
    }


}
