package com.nlpai4h.healthydemobacked.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ControllerLog {

    boolean enabled() default true;

    boolean logArgs() default true;

    boolean logResult() default true;

    Level level() default Level.INFO;

    int maxLen() default -1;

    enum Level {
        DEBUG,
        INFO,
        WARN
    }
}

