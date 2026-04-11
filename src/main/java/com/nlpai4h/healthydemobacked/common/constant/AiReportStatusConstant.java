package com.nlpai4h.healthydemobacked.common.constant;

/**
 * AI报告状态常量
 */
public class AiReportStatusConstant {
    /**
     * 未生成
     */
    public static final Integer NOT_GENERATED = 0;

    /**
     * 已生成
     */
    public static final Integer GENERATED = 1;

    /**
     * 生成中
     */
    public static final Integer GENERATING = 2;

    /**
     * 生成失败
     */
    public static final Integer GENERATION_FAILED = 3;

    private AiReportStatusConstant() {
        // 工具类，禁止实例化
    }
}

