package org.jetlinks.community.modbus;

import org.jetlinks.community.modbus.entity.ProductCollectConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 采集配置枚举类
 *
 * @author Tensai
 */
public enum CollectConfigEnum {

    /**
     * 采集配置
     */
    INSTANCE;

    /**
     * product - INSTANCE 映射
     */
    Map<String, ProductCollectConfig> productCollectConfigMap = new ConcurrentHashMap<>();

    /**
     * Map.put
     *
     * @param config 产品采集配置
     */
    public void put(ProductCollectConfig config) {
        productCollectConfigMap.putIfAbsent(config.getProductId(), config);
    }

    /**
     * map.get
     *
     * @param productId 产品ID
     * @return 产品采集配置
     */
    public ProductCollectConfig get(String productId) {
        return productCollectConfigMap.getOrDefault(productId, new ProductCollectConfig());
    }

}
