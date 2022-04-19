package org.jetlinks.zlan.protocol.temp;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author Tensai
 */
@Data
public class LastMetric {

    private String name;

    private BigDecimal value;
}
