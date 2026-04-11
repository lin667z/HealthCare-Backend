package com.nlpai4h.healthydemobacked.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiReportSummaryCardVO {
    private String label;
    private String value;
    private String tone;
}
