package org.jetlinks.community.modbus.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.community.modbus.entity.ModbusProductModel;
import org.jetlinks.community.modbus.service.LocalModbusProductModelService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * @author Tensai
 */
@RestController
@RequestMapping({"/modbus_product_model", "/modbus/product/model"})
@Authorize(ignore = true)
@Resource(id = "modbus-product-model", name = "modbus产品解析模型")
@Slf4j
@Tag(name = "modbus产品解析模型")
public class ModbusProductModelController implements ReactiveServiceCrudController<ModbusProductModel, String> {

    @Getter
    private final LocalModbusProductModelService service;

    public ModbusProductModelController(LocalModbusProductModelService service) {
        this.service = service;
    }


}
