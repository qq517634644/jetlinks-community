package org.jetlinks.modbus.protocol.tcp.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * modbus 解析模型
 *
 * @author Tensai
 */
@Getter
@AllArgsConstructor(staticName = "of")
public class ModbusProductModelView {

    /**
     * 产品ID
     */
    private final String productId;

    /**
     * 指标编码 对应物模型中的 属性标识
     */
    private final String metricCode;

    /**
     * 指标系数
     */
    private final Double ratio;

    /**
     * 偏移量
     */
    private final Integer offset;

    /**
     * 字节长度
     */
    private final Integer length;

    /**
     * 数据类型
     */
    private final DataType dataType;

    /**
     * 前置JS处理
     */
    private final String preScript = null;

    /**
     * 后置JS处理
     */
    private final String postScript = null;
}
