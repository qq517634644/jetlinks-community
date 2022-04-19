package org.jetlinks.zlan.protocol.codec;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 子设备 信息
 *
 * @author Tensai
 */
public interface ChildrenDevice {

    Map<String, Integer> STATUS_MAP = new ConcurrentHashMap<>();
}
