package com.nlpai4h.healthydemobacked.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NursingRecordListVO {
    private Long id;
    private String registrationNo;
    private String visitNo;
    private String measureTime;
    private String itemName;
    private String upperLimit;
    private String lowerLimit;
    private String measureValue;
    private LocalDateTime createTime;
}
