package com.nlpai4h.healthydemobacked.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "ai.report")
@Data
public class AiProperties {
    /**
     * 生成报告的Prompt
     */
    private String prompt;

    private String systemPrompt;

    private String developerPrompt;

    private List<String> bannedPhrases;
}
