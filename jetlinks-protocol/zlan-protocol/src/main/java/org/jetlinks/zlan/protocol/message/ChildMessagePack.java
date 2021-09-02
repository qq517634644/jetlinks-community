package org.jetlinks.zlan.protocol.message;

import lombok.Data;

import java.util.Map;

/**
 * 子设备数据
 *
 * @author Tensai
 */
@Data
public class ChildMessagePack {

    private String id;

    private Integer status;

    private Map<String, Object> data;
}
