package org.jetlinks.community.influxdb2.config;

import com.influxdb.client.reactive.InfluxDBClientReactive;
import com.influxdb.client.reactive.InfluxDBClientReactiveFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Tensai
 */
@Configuration
@EnableConfigurationProperties(Influxdb2Properties.class)
public class Influxdb2Configuration {

    @Autowired
    private Influxdb2Properties properties;

    @Bean
    public InfluxDBClientReactive influxReactiveClient() {
        return InfluxDBClientReactiveFactory.create(
            properties.getUrl(),
            properties.getToken().toCharArray(),
            properties.getOrg(),
            properties.getBucket()
        ).enableGzip();

    }
}
