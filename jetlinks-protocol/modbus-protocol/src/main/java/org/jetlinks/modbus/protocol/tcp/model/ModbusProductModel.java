package org.jetlinks.modbus.protocol.tcp.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * modbus 解析模型
 *
 * @author Tensai
 */
@Data
@AllArgsConstructor(staticName = "of")
public class ModbusProductModel {

    /**
     * 产品ID
     */
    private String productId;

    /**
     * 指标编码 对应物模型中的 属性标识
     */
    private String metricCode;

    /**
     * 指标系数
     */
    private Double ratio;

    /**
     * 偏移量
     */
    private Integer offset;

    /**
     * 字节长度
     */
    private Integer length;

    /**
     * 数据类型
     */
    private DataType dataType;
}
