package org.jetlinks.community.modbus.task;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * DTU数据采集控制器
 * for XXL-job任务调度器
 *
 * @author Tensai
 */
@RequestMapping("/dtu/collection/")
@RestController
@Authorize
@Tag(name = "系统管理")
public class DtuCollectionController {

    /**
     * 接受xxl-job推送的待采集的下挂设备
     *
     * @param deviceIdList 下挂设备ID
     */
    @PostMapping("/accept")
    public void accept(@RequestBody List<String> deviceIdList) {
        // TODO
        // 0. 根据productId对设备进行分组
        // 1. 获取设备的modbus模型
        // 2. 拼装指令
        // 3. 根据DTU分组入队列
    }
}
