package org.jetlinks.community.influxdb2.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Tensai
 */
@Data
@ConfigurationProperties(prefix = "spring.influxdb2")
public class Influxdb2Properties {

    private String url;

    private String token;

    private String org;

    private String bucket;

}