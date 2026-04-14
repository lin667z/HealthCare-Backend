package com.nlpai4h.healthydemobacked.service.ai;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.nlpai4h.healthydemobacked.common.properties.AiProperties;
import com.nlpai4h.healthydemobacked.model.vo.AiReportContextVO;
import com.nlpai4h.healthydemobacked.model.vo.AiReportSummaryCardVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * AI临床报告解析器
 * 负责对AI生成的临床报告进行格式校验、内容提取、风险等级判定、摘要卡片构建等核心处理
 */
@Component
public class AiReportParser {

    /**
     * AI临床报告【必填】的章节标题列表
     * 用于校验报告格式完整性
     */
    private static final List<String> REQUIRED_HEADINGS = List.of(
            "# AI临床报告",
            "## 1. 患者概览",
            "## 2. 核心临床判断",
            "## 3. 异常检查/检验解读",
            "## 4. 风险提示",
            "## 5. 诊疗建议",
            "## 6. 依据与证据摘要",
            "## 7. 局限性声明"
    );

    /**
     * 列表项匹配正则
     * 匹配：无序列表(-/*)、有序列表(数字.) 开头的内容
     */
    private static final Pattern BULLET_PATTERN = Pattern.compile("^\\s*([-*]|\\d+\\.)\\s+");

    /**
     * AI相关配置属性（包含违禁词等配置）
     */
    @Resource
    private AiProperties aiProperties;

    /**
     * 校验AI报告内容的合法性
     * 校验规则：非空、包含所有必填章节、不包含违禁词
     *
     * @param reportContent 原始AI报告内容
     * @return 校验结果（是否合法 + 错误信息列表）
     */
    public ValidationResult validate(String reportContent) {
        List<String> errors = new ArrayList<>();
        // 标准化报告文本格式
        String normalized = normalize(reportContent);
        // 校验：报告内容不能为空
        if (StrUtil.isBlank(normalized)) {
            errors.add("empty report");
            return new ValidationResult(false, errors);
        }
        // 校验：必须包含所有预设的必填章节标题
        for (String heading : REQUIRED_HEADINGS) {
            if (!normalized.contains(heading)) {
                errors.add("missing heading: " + heading);
            }
        }
        // 校验：不能包含配置中定义的违禁词
        if (CollUtil.isNotEmpty(aiProperties.getBannedPhrases())) {
            for (String phrase : aiProperties.getBannedPhrases()) {
                if (StrUtil.isNotBlank(phrase) && normalized.contains(phrase)) {
                    errors.add("banned phrase: " + phrase);
                }
            }
        }
        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * 从报告中推导风险等级
     * 优先解析【风险提示】章节，根据关键词判定高/中/低/未知风险
     *
     * @param reportContent 原始AI报告内容
     * @return 风险等级：high/medium/low/unknown
     */
    public String deriveRiskLevel(String reportContent) {
        // 获取风险提示章节内容
        String riskSection = getSection(reportContent, "## 4. 风险提示");
        // 无风险章节则使用全文匹配，统一转小写
        String value = StrUtil.blankToDefault(riskSection, reportContent).toLowerCase();
        // 按关键词匹配风险等级
        if (value.contains("高风险") || value.contains("紧急") || value.contains("危急")) {
            return "high";
        }
        if (value.contains("中风险") || value.contains("重点关注")) {
            return "medium";
        }
        if (value.contains("低风险") || value.contains("继续随访")) {
            return "low";
        }
        return "unknown";
    }

    /**
     * 提取报告中的关键发现
     * 从【核心临床判断】+【异常检查/检验解读】章节提取列表项，最多返回6条
     *
     * @param reportContent 原始AI报告内容
     * @return 关键发现列表
     */
    public List<String> extractKeyFindings(String reportContent) {
        List<String> findings = new ArrayList<>();
        findings.addAll(extractListItems(getSection(reportContent, "## 2. 核心临床判断")));
        findings.addAll(extractListItems(getSection(reportContent, "## 3. 异常检查/检验解读")));
        // 去空值 + 限制最多6条
        return findings.stream().filter(StrUtil::isNotBlank).limit(6).toList();
    }

    /**
     * 提取报告中的诊疗建议
     * 从【诊疗建议】章节提取列表项，最多返回6条
     *
     * @param reportContent 原始AI报告内容
     * @return 诊疗建议列表
     */
    public List<String> extractSuggestions(String reportContent) {
        return extractListItems(getSection(reportContent, "## 5. 诊疗建议")).stream().limit(6).toList();
    }

    /**
     * 构建AI报告摘要卡片
     * 组装患者基础信息、风险等级、关键发现等核心数据为前端展示卡片
     *
     * @param contextVO     报告上下文VO（患者基础信息）
     * @param riskLevel     风险等级
     * @param keyFindings   关键发现列表
     * @return 摘要卡片列表
     */
    public List<AiReportSummaryCardVO> buildSummaryCards(AiReportContextVO contextVO, String riskLevel, List<String> keyFindings) {
        List<AiReportSummaryCardVO> cards = new ArrayList<>();
        // 患者基础信息卡片：主要诊断、科室/就诊
        if (contextVO != null && contextVO.getPatientSummary() != null) {
            cards.add(AiReportSummaryCardVO.builder()
                    .label("主要诊断")
                    .value(StrUtil.blankToDefault(contextVO.getPatientSummary().getDischargeMainDiag(), "未提供相关数据"))
                    .tone("info")
                    .build());
            cards.add(AiReportSummaryCardVO.builder()
                    .label("科室/就诊")
                    .value(StrUtil.blankToDefault(contextVO.getPatientSummary().getDeptName(), "未提供相关数据"))
                    .tone("neutral")
                    .build());
        }
        // 风险等级卡片：根据等级设置不同样式
        cards.add(AiReportSummaryCardVO.builder()
                .label("风险等级")
                .value(mapRiskLevelLabel(riskLevel))
                .tone("high".equals(riskLevel) ? "danger" : "medium".equals(riskLevel) ? "warning" : "info")
                .build());
        // 关键发现数量卡片
        cards.add(AiReportSummaryCardVO.builder()
                .label("关键发现")
                .value(keyFindings.isEmpty() ? "未提供相关数据" : keyFindings.size() + "项")
                .tone("info")
                .build());
        return cards;
    }

    /**
     * 风险等级映射：英文标识转中文标签
     *
     * @param riskLevel 英文风险等级
     * @return 中文风险标签
     */
    private String mapRiskLevelLabel(String riskLevel) {
        return switch (StrUtil.blankToDefault(riskLevel, "unknown")) {
            case "high" -> "高";
            case "medium" -> "中";
            case "low" -> "低";
            default -> "未判定";
        };
    }

    /**
     * 从章节内容中提取列表项
     * 优先提取带项目符号的列表，无列表则提取纯文本行（最多3行）
     *
     * @param section 章节内容
     * @return 清理后的列表项集合
     */
    private List<String> extractListItems(String section) {
        if (StrUtil.isBlank(section)) {
            return List.of();
        }
        List<String> items = new ArrayList<>();
        // 第一步：匹配并提取带项目符号的列表项，去除符号
        for (String line : normalize(section).split("\n")) {
            if (BULLET_PATTERN.matcher(line).find()) {
                items.add(BULLET_PATTERN.matcher(line).replaceFirst("").trim());
            }
        }
        // 有列表项直接返回
        if (!items.isEmpty()) {
            return items;
        }
        // 第二步：无列表项时，提取纯文本行（过滤标题行、空行）
        return normalize(section).lines()
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .filter(line -> !line.startsWith("## "))
                .limit(3)
                .toList();
    }

    /**
     * 获取报告中指定标题的章节内容
     *
     * @param reportContent 完整报告内容
     * @param heading       目标章节标题
     * @return 章节内容（无则返回空字符串）
     */
    private String getSection(String reportContent, String heading) {
        Map<String, String> sections = splitSections(reportContent);
        return sections.getOrDefault(heading, "");
    }

    /**
     * 将完整报告按【## 】标题分割为章节Map
     * Key：章节标题，Value：章节内容
     *
     * @param reportContent 完整报告内容
     * @return 有序的章节Map（保持报告原有顺序）
     */
    private Map<String, String> splitSections(String reportContent) {
        Map<String, String> sections = new LinkedHashMap<>();
        String currentHeading = null;
        StringBuilder current = new StringBuilder();
        // 逐行解析报告，拆分章节
        for (String rawLine : normalize(reportContent).split("\n")) {
            String line = rawLine.trim();
            // 识别章节标题（以## 开头）
            if (line.startsWith("## ")) {
                // 上一个章节已存在，存入Map
                if (currentHeading != null) {
                    sections.put(currentHeading, current.toString().trim());
                }
                // 更新当前标题，重置内容
                currentHeading = line;
                current = new StringBuilder();
                continue;
            }
            // 非标题行，追加到当前章节内容
            if (currentHeading != null) {
                current.append(rawLine).append('\n');
            }
        }
        // 存入最后一个章节
        if (currentHeading != null) {
            sections.put(currentHeading, current.toString().trim());
        }
        return sections;
    }

    /**
     * 文本标准化处理
     * 统一换行符、空值处理、首尾去空格
     *
     * @param value 原始文本
     * @return 标准化后的文本
     */
    private String normalize(String value) {
        return StrUtil.blankToDefault(value, "")
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .trim();
    }

    /**
     * 报告校验结果封装
     * @param valid 是否校验通过
     * @param errors 错误信息列表
     */
    public record ValidationResult(boolean valid, List<String> errors) {}
}