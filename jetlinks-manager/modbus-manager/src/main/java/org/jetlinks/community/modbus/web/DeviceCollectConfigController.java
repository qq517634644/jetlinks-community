package org.jetlinks.community.modbus.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.community.modbus.entity.DeviceCollectConfig;
import org.jetlinks.community.modbus.service.LocalDeviceCollectConfigService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * @author Tensai
 */
@RestController
@RequestMapping({"/modbus-task-config", "/modbus/task/config"})
@Authorize
@Resource(id = "modbus-task-config", name = "modbus采集-设备配置")
@Slf4j
@Tag(name = "modbus采集-设备配置")
public class DeviceCollectConfigController implements ReactiveServiceCrudController<DeviceCollectConfig, String> {

    @Getter
    private final LocalDeviceCollectConfigService service;

    public DeviceCollectConfigController(LocalDeviceCollectConfigService service) {
        this.service = service;
    }


}
