package org.jetlinks.community.modbus.task;

import cn.hutool.core.io.checksum.crc16.CRC16Modbus;
import cn.hutool.core.util.HexUtil;
import com.alibaba.fastjson.JSON;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.core.param.TermType;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.community.modbus.CollectConfigEnum;
import org.jetlinks.community.modbus.entity.DeviceCollectConfig;
import org.jetlinks.community.modbus.entity.ModbusProductModel;
import org.jetlinks.community.modbus.service.LocalDeviceCollectConfigService;
import org.jetlinks.community.modbus.service.LocalModbusProductModelService;
import org.jetlinks.community.modbus.service.LocalProductCollectConfigService;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.core.server.session.DeviceSessionManager;
import org.jetlinks.modbus.protocol.tcp.command.CommandEnum;
import org.jetlinks.modbus.protocol.tcp.model.ProductModelEnum;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import javax.annotation.PostConstruct;
import java.time.Duration;

/**
 * @author Tensai
 */
@Slf4j
@Component
@EnableScheduling
public class Task {

    @Getter
    private final DeviceSessionManager deviceSessionManager;

    @Getter
    private final LocalDeviceCollectConfigService deviceCollectConfigService;

    @Getter
    private final LocalProductCollectConfigService productCollectConfigService;

    @Getter
    private final LocalModbusProductModelService modbusProductModelService;

    public Task(DeviceSessionManager deviceSessionManager, LocalDeviceCollectConfigService modbusTaskConfigService, LocalProductCollectConfigService productCollectConfigStringGenericReactiveCrudService, LocalModbusProductModelService modbusProductModelService) {
        this.deviceSessionManager = deviceSessionManager;
        this.deviceCollectConfigService = modbusTaskConfigService;
        this.productCollectConfigService = productCollectConfigStringGenericReactiveCrudService;
        this.modbusProductModelService = modbusProductModelService;
    }


    /**
     * 构建设备ID和产品ID的映射
     *
     * @return Void
     */
    @PostConstruct
    public void initCollection() {
        log.error("当前设备");
        // 初始化Modbus采集 设备-产品映射
        // TODO 同步更新表
        deviceCollectConfigService.query(QueryParamEntity.of())
            .subscribe(it -> {
                log.error("当前设备" + JSON.toJSONString(it));
                ProductModelEnum.INSTANCE.setDeviceIdMapProductId(it.getDeviceId(), it.getProductId());
            });

        // 【采集指令】初始化产品采集配置 用于为设备采集提供默认值
        // TODO 同步更新表
        productCollectConfigService.query(QueryParamEntity.of())
            .subscribe(CollectConfigEnum.INSTANCE::put);


        // 【数据解析】初始化产品采集
        // TODO 同步更新表
        modbusProductModelService.query(QueryParamEntity.of()).map(ModbusProductModel::mapView)
            .subscribe(ProductModelEnum.INSTANCE::putModbusParseModelMap);

        // 指令定时生成器
        Flux.interval(Duration.ofSeconds(10), Duration.ofMinutes(1), Schedulers.newParallel("device-collect-code-provider"))
            .subscribe(this::codeProvider);

        // 指令超时执行器
        Flux.interval(Duration.ofSeconds(30), Duration.ofSeconds(15), Schedulers.newSingle("device-session-checker"))
            .subscribe(
                // 指令超时逻辑
                l -> CommandEnum.INSTANCE.getDtuIdSet().parallelStream().forEach(
                    dtuId -> {
                        // 每次执行都是一次超时 计数器-1
                        CommandEnum.INSTANCE.timeOutOrComplete(dtuId);
                        byte[] code = CommandEnum.INSTANCE.pollCommand(dtuId);
                        if (code != null) {
                            sendMessage(dtuId, code);
                        }
                    }
                )
            );
    }

    /**
     * 设备采集指令生成器
     *
     * @param fluxLong 自增长
     */
    private void codeProvider(long fluxLong) {

        // 查询开启采集的设备和默认配置的设备（排除关闭采集的设备）
        deviceCollectConfigService.createQuery().and("task_switch", TermType.not, false).fetch()
            // 过滤出要执行采集的设备
            .filter(it -> fluxLong % it.getInterval() != 0)
            // 填充默认值
            .map(it -> {
                if (it.getInterval() == null) {
                    it.setInterval(CollectConfigEnum.INSTANCE.get(it.getProductId()).getInterval());
                }
                if (it.getTaskSwitch() == null) {
                    it.setTaskSwitch(CollectConfigEnum.INSTANCE.get(it.getProductId()).getTaskSwitch());
                }
                return it;
            })
            // 过滤可以采集的设备
            .filter(DeviceCollectConfig::getTaskSwitch)
            .subscribe(it -> {
                String dtuId = it.getProductId();
                String slaveId = it.getDeviceId().split("-")[0];
                // 组合slaveId和采集指令
                String code = shortStringToHex2(slaveId) + CollectConfigEnum.INSTANCE.get(dtuId).getCode();
                // 获取完整指令
                byte[] command = makeCrcCommand(code);
                // 指令入队
                CommandEnum.INSTANCE.offerCommand(dtuId, command);
            });
    }

    /**
     * 计算CRC16Modbus 并返回整条指令
     *
     * @param code 十六进制字符串指令不带CRC
     * @return 带CRC的指令
     */
    private byte[] makeCrcCommand(String code) {
        byte[] command = HexUtil.decodeHex(code);
        CRC16Modbus crc16Modbus = new CRC16Modbus();
        crc16Modbus.reset();
        crc16Modbus.update(command);
        String crc = crc16Modbus.getHexValue(true);
        char[] crcs = crc.toCharArray();
        code += crcs[2];
        code += crcs[3];
        code += crcs[0];
        code += crcs[1];
        command = HexUtil.decodeHex(code);
        return command;
    }

    /**
     * short字符串转十六进制（两位）
     *
     * @param slaveId slaveId
     * @return 十六进制
     */
    private String shortStringToHex2(String slaveId) {
        short s = Short.parseShort(slaveId);
        String code = HexUtil.toHex(s);
        if (code.length() == 1) {
            code = "0" + code;
        }
        return code;
    }


    /**
     * 向DTU发送指令
     *
     * @param dtuId dtuId
     * @param code  指令
     */
    private void sendMessage(String dtuId, byte[] code) {
        DeviceSession deviceSession = deviceSessionManager.getSession(dtuId);
        if (deviceSession == null) {
            log.error("DTU【{}】 deviceSession为空", dtuId);
        } else {
            deviceSession.send(EncodedMessage.simple(Unpooled.wrappedBuffer(code)))
                .subscribe(it -> {
                    if (it) {
                        log.info("超时 - 发送DTU【{}】指令【{}】成功", dtuId, HexUtil.encodeHexStr(code));
                    }
                });
        }
    }
}
