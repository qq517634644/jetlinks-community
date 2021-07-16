package org.jetlinks.modbus.protocol.tcp.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 物模型属性数据类型
 *
 * @author Tensai
 */
@AllArgsConstructor
@Getter
public enum DataType {
    /**
     *
     */
    INTEGER("整型"),
    SHORT("短整型"),
    LONG("长整型"),
    BOOLEAN("布尔型"),
    DOUBLE("双精度浮点型"),
    FLOAT("浮点型");


    private final String name;
}
