package com.nlpai4h.healthydemobacked.model.event;

import org.springframework.context.ApplicationEvent;

public class AiReportProgressEvent extends ApplicationEvent {
    private final String visitNo;
    private final String registrationNo;

    public AiReportProgressEvent(Object source, String visitNo, String registrationNo) {
        super(source);
        this.visitNo = visitNo;
        this.registrationNo = registrationNo;
    }

    public String getVisitNo() {
        return visitNo;
    }

    public String getRegistrationNo() {
        return registrationNo;
    }
}
