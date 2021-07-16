package org.jetlinks.community.standalone.task;

import cn.hutool.core.util.HexUtil;
import com.alibaba.fastjson.JSON;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.community.device.service.LocalDeviceInstanceService;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.core.server.session.DeviceSessionManager;
import org.jetlinks.modbus.protocol.tcp.command.CommandEnum;
import org.jetlinks.modbus.protocol.tcp.model.ProductModelEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Tensai
 */
@Slf4j
@Component
@EnableScheduling
public class Task {

    @Autowired
    private DeviceSessionManager deviceSessionManager;

    @Autowired
    private LocalDeviceInstanceService deviceInstanceService;


    /**
     * 构建设备ID和产品ID的映射
     *
     * @return Void
     */
    @PostConstruct
    public void initCollection() {
        log.error("当前设备");
        // 初始化设备产品映射
        deviceInstanceService.query(QueryParamEntity.of())
            .subscribe(it -> {
                log.error("当前设备" + JSON.toJSONString(it));
                ProductModelEnum.INSTANCE.setDeviceIdMapProductId(it.getId(), it.getProductId());
            });

        // 定时执行器
        Flux.interval(Duration.ofSeconds(10), Duration.ofSeconds(10), Schedulers.newSingle("device-session-checker"))
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

    @Scheduled(fixedDelay = 30000L)
    public void codeProvider() {
        List<String> dtuList = new ArrayList<String>() {{
            add("1");
            add("2");
            add("3");
            add("4");
        }};

        List<byte[]> codeList = new ArrayList<byte[]>() {{
            add(new byte[]{0x01, 0x03, 0x00, 0x00, 0x00, 0x02, (byte) 0xC4, 0x0B});
            add(new byte[]{0x02, 0x03, 0x00, 0x03, 0x00, 0x01, 0x74, 0x39});
            add(new byte[]{0x03, 0x03, 0x00, 0x03, 0x00, 0x01, 0x75, (byte) 0xE8});
            add(new byte[]{0x09, 0x03, 0x00, 0x00, 0x00, 0x02, (byte) 0xC5, 0x43});
            add(new byte[]{0x0b, 0x03, 0x00, 0x00, 0x00, 0x02, (byte) 0xC4, (byte) 0xA1});
        }};
        dtuList.forEach(dtu -> codeList.forEach(code -> CommandEnum.INSTANCE.offerCommand(dtu, code)));

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
