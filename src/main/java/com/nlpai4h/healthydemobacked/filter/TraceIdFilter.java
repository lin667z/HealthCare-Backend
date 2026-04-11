package com.nlpai4h.healthydemobacked.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID = "traceId";
    public static final String REQUEST_ID = "requestId";

    private static final String HEADER_REQUEST_ID = "X-Request-Id";
    private static final String HEADER_TRACE_ID = "X-Trace-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String incoming = firstNonBlank(
                request.getHeader(HEADER_REQUEST_ID),
                request.getHeader(HEADER_TRACE_ID)
        );
        String id = StringUtils.hasText(incoming)
                ? incoming.trim()
                : UUID.randomUUID().toString().replace("-", "");

        MDC.put(TRACE_ID, id);
        MDC.put(REQUEST_ID, id);
        response.setHeader(HEADER_REQUEST_ID, id);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(REQUEST_ID);
            MDC.remove(TRACE_ID);
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}

