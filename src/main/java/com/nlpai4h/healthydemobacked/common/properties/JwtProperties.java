package com.nlpai4h.healthydemobacked.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mxb.jwt")
@Data
public class JwtProperties {

    /**
     * 生成jwt令牌相关配置
     */
    private String mxbSecretKey;
    private long mxbTtl;
    private String mxbTokenName;

}
