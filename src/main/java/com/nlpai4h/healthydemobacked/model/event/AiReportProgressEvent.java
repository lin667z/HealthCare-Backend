package com.nlpai4h.healthydemobacked.model.event;

import org.springframework.context.ApplicationEvent;

/**
 * AI 报告生成进度事件
 * 用于在 Spring 容器中发布 AI 报告生成进度的通知
 */
public class AiReportProgressEvent extends ApplicationEvent {
    /**
     * 就诊号
     */
    private final String visitNo;
    /**
     * 登记号
     */
    private final String registrationNo;

    /**
     * 构造函数
     * @param source 事件源
     * @param visitNo 就诊号
     * @param registrationNo 登记号
     */
    public AiReportProgressEvent(Object source, String visitNo, String registrationNo) {
        super(source);
        this.visitNo = visitNo;
        this.registrationNo = registrationNo;
    }

    /**
     * 获取就诊号
     * @return 就诊号
     */
    public String getVisitNo() {
        return visitNo;
    }

    /**
     * 获取登记号
     * @return 登记号
     */
    public String getRegistrationNo() {
        return registrationNo;
    }
}
