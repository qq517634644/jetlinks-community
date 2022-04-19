package org.jetlinks.zlan.protocol.message;

/**
 * 消息类型
 *
 * @author Tensai
 */
public enum MessageType {
    /**
     * 注册
     */
    REGISTER,
    /**
     * 心跳
     */
    HEARTBEAT,
    /**
     * 数据上报
     */
    REPORT,
    /**
     * 在线
     */
    ONLINE,
    /**
     * 离线
     */
    OFFLINE
}
