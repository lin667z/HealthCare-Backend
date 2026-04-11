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

@Component
public class AiReportParser {

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

    private static final Pattern BULLET_PATTERN = Pattern.compile("^\\s*([-*]|\\d+\\.)\\s+");

    @Resource
    private AiProperties aiProperties;

    public ValidationResult validate(String reportContent) {
        List<String> errors = new ArrayList<>();
        String normalized = normalize(reportContent);
        if (StrUtil.isBlank(normalized)) {
            errors.add("empty report");
            return new ValidationResult(false, errors);
        }
        for (String heading : REQUIRED_HEADINGS) {
            if (!normalized.contains(heading)) {
                errors.add("missing heading: " + heading);
            }
        }
        if (CollUtil.isNotEmpty(aiProperties.getBannedPhrases())) {
            for (String phrase : aiProperties.getBannedPhrases()) {
                if (StrUtil.isNotBlank(phrase) && normalized.contains(phrase)) {
                    errors.add("banned phrase: " + phrase);
                }
            }
        }
        return new ValidationResult(errors.isEmpty(), errors);
    }

    public String deriveRiskLevel(String reportContent) {
        String riskSection = getSection(reportContent, "## 4. 风险提示");
        String value = StrUtil.blankToDefault(riskSection, reportContent).toLowerCase();
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

    public List<String> extractKeyFindings(String reportContent) {
        List<String> findings = new ArrayList<>();
        findings.addAll(extractListItems(getSection(reportContent, "## 2. 核心临床判断")));
        findings.addAll(extractListItems(getSection(reportContent, "## 3. 异常检查/检验解读")));
        return findings.stream().filter(StrUtil::isNotBlank).limit(6).toList();
    }

    public List<String> extractSuggestions(String reportContent) {
        return extractListItems(getSection(reportContent, "## 5. 诊疗建议")).stream().limit(6).toList();
    }

    public List<AiReportSummaryCardVO> buildSummaryCards(AiReportContextVO contextVO, String riskLevel, List<String> keyFindings) {
        List<AiReportSummaryCardVO> cards = new ArrayList<>();
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
        cards.add(AiReportSummaryCardVO.builder()
                .label("风险等级")
                .value(mapRiskLevelLabel(riskLevel))
                .tone("high".equals(riskLevel) ? "danger" : "medium".equals(riskLevel) ? "warning" : "info")
                .build());
        cards.add(AiReportSummaryCardVO.builder()
                .label("关键发现")
                .value(keyFindings.isEmpty() ? "未提供相关数据" : keyFindings.size() + "项")
                .tone("info")
                .build());
        return cards;
    }

    private String mapRiskLevelLabel(String riskLevel) {
        return switch (StrUtil.blankToDefault(riskLevel, "unknown")) {
            case "high" -> "高";
            case "medium" -> "中";
            case "low" -> "低";
            default -> "未判定";
        };
    }

    private List<String> extractListItems(String section) {
        if (StrUtil.isBlank(section)) {
            return List.of();
        }
        List<String> items = new ArrayList<>();
        for (String line : normalize(section).split("\n")) {
            if (BULLET_PATTERN.matcher(line).find()) {
                items.add(BULLET_PATTERN.matcher(line).replaceFirst("").trim());
            }
        }
        if (!items.isEmpty()) {
            return items;
        }
        return normalize(section).lines()
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .filter(line -> !line.startsWith("## "))
                .limit(3)
                .toList();
    }

    private String getSection(String reportContent, String heading) {
        Map<String, String> sections = splitSections(reportContent);
        return sections.getOrDefault(heading, "");
    }

    private Map<String, String> splitSections(String reportContent) {
        Map<String, String> sections = new LinkedHashMap<>();
        String currentHeading = null;
        StringBuilder current = new StringBuilder();
        for (String rawLine : normalize(reportContent).split("\n")) {
            String line = rawLine.trim();
            if (line.startsWith("## ")) {
                if (currentHeading != null) {
                    sections.put(currentHeading, current.toString().trim());
                }
                currentHeading = line;
                current = new StringBuilder();
                continue;
            }
            if (currentHeading != null) {
                current.append(rawLine).append('\n');
            }
        }
        if (currentHeading != null) {
            sections.put(currentHeading, current.toString().trim());
        }
        return sections;
    }

    private String normalize(String value) {
        return StrUtil.blankToDefault(value, "")
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .trim();
    }

    public record ValidationResult(boolean valid, List<String> errors) {}
}
