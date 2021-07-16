package org.jetlinks.modbus.protocol.tcp.command;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 指令执行相关单例
 *
 * @author Tensai
 */
@Slf4j
public enum CommandEnum {
    /**
     * 单一实例
     */
    INSTANCE;

    /**
     * 指令队列Map
     * key - DTU-ID
     * value - 采集指令
     */
    private final Map<String, Queue<byte[]>> commandQueueMap = new ConcurrentHashMap<>();

    /**
     * 指令执行计数器
     * pop +1
     * timeout -1
     * finished -1
     */
    private final Map<String, AtomicInteger> commandCounterMap = new ConcurrentHashMap<>();

    /**
     * 获取当前所有的队列名称
     *
     * @return 队列名称
     */
    public Set<String> getDtuIdSet() {
        return commandQueueMap.keySet();
    }

    /**
     * command入队到指DTU队列
     *
     * @param dtuId   dtuId
     * @param command modbus指令
     * @return 执行结果
     */
    public boolean offerCommand(String dtuId, byte[] command) {
        return commandQueueMap.computeIfAbsent(dtuId, it -> new ConcurrentLinkedQueue<>()).add(command);
    }

    /**
     * 指令出队
     *
     * @param dtuId dtuId
     * @return modbus指令
     */
    public byte[] pollCommand(String dtuId) {
        // 计数器未清零 当前队列执行未收到回复
        AtomicInteger counter = commandCounterMap.computeIfAbsent(dtuId, it -> new AtomicInteger(0));
        if (counter.get() > 0) {
            log.warn("当前计数器【{}】未清零 -- {}", dtuId, counter);
            return null;
        }
        if (counter.get() < 0) {
            counter = new AtomicInteger(0);
            commandCounterMap.put(dtuId, counter);
        }
        // poll之前计数器+1 阻止其他进程拿到指令
        commandCounterMap.computeIfAbsent(dtuId, it -> new AtomicInteger(0)).incrementAndGet();
        return commandQueueMap.computeIfAbsent(dtuId, it -> new ConcurrentLinkedQueue<>()).poll();
    }

    /**
     * 指令完成或者超时 计数器-1
     *
     * @param dtuId DtuId
     */
    public void timeOutOrComplete(String dtuId) {
        int counter = commandCounterMap.computeIfAbsent(dtuId, it -> new AtomicInteger(0)).decrementAndGet();
        if (counter < 0) {
            commandCounterMap.put(dtuId, new AtomicInteger(0));
        }
    }
}
