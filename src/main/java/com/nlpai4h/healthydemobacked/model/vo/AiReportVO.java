package com.nlpai4h.healthydemobacked.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI报告视图对象
 * 用于向前端展示AI病历报告的完整状态和解析后的结构化数据
 */
@Data
@Builder
public class AiReportVO {
    /** 报告主键ID */
    private Long reportId;
    /** 乐观锁版本号/生成次数 */
    private Integer version;
    /** 完整的报告内容（Markdown或纯文本格式） */
    private String reportContent;
    /** 流式生成过程中的部分内容 */
    private String partialContent;
    /** 状态文本描述（如：未生成、生成中、已生成、生成失败） */
    private String status;
    /** 状态机枚举值（如：PREPARING, GENERATING, SUCCEEDED, FAILED） */
    private String statusPhase;
    /** 记录最后更新时间 */
    private LocalDateTime updateTime;
    /** 报告完成生成的时间 */
    private LocalDateTime generatedAt;
    /** 状态信息或错误提示信息 */
    private String statusMessage;
    /** 解析出的风险等级（低、中、高） */
    private String riskLevel;
    /** 解析出的摘要卡片列表 */
    private List<AiReportSummaryCardVO> summaryCards;
    /** 解析出的关键发现列表 */
    private List<String> keyFindings;
    /** 解析出的随访建议列表 */
    private List<String> followUpSuggestions;
    /** 原始上下文数据快照（生成报告时使用的数据） */
    private AiReportContextVO sourceSnapshot;
    /** 输入数据摘要 */
    private AiReportContextVO inputSummary;
}
