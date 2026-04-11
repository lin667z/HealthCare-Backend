package com.nlpai4h.healthydemobacked.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VisitListVO {
    private Long id;
    private String registrationNo;
    private String visitNo;
    private String ageAtVisit;
    private String visitStatus;
    private String visitType;
    private LocalDateTime createTime;
}
