package com.nlpai4h.healthydemobacked.aspect;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nlpai4h.healthydemobacked.common.annotation.ControllerLog;
import com.nlpai4h.healthydemobacked.common.annotation.NoControllerLog;
import com.nlpai4h.healthydemobacked.common.context.BaseContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Slf4j
public class LogAspect {

    private static final String TRACE_ID = "traceId";
    private static final String UNKNOWN = "UNKNOWN";
    private static final String MASK = "***";
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password",
            "token",
            "secret",
            "authorization",
            "api-key",
            "apikey"
    );
    private static final List<String> HEADER_WHITELIST = List.of(
            "x-request-id",
            "x-trace-id",
            "content-type",
            "accept"
    );
    private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();

    @Value("${app.log.max-len:2000}")
    private int defaultMaxLen;

    @Value("${app.log.response.enabled:true}")
    private boolean logResponseEnabled;

    @Pointcut("execution(* com.nlpai4h.healthydemobacked.controller..*.*(..))")
    public void controllerLog() {
    }

    @Around("controllerLog()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startNanos = System.nanoTime();
        TraceScope traceScope = TraceScope.open();
        LogPolicy policy = resolvePolicy(joinPoint);
        RequestSnapshot requestSnapshot = RequestSnapshot.from(joinPoint);

        try {
            // 日志失败绝不能阻止控制器调用
            safeLogRequest(policy, requestSnapshot, joinPoint);

            Object result = joinPoint.proceed();

            safeLogResponse(policy, requestSnapshot, result, startNanos);
            return result;
        } catch (Throwable throwable) {
            safeLogException(policy, requestSnapshot, throwable, startNanos);
            throw throwable;
        } finally {
            traceScope.close();
        }
    }

    private void safeLogRequest(LogPolicy policy, RequestSnapshot requestSnapshot, ProceedingJoinPoint joinPoint) {
        if (!policy.canLogRequest() || !isLevelEnabled(policy.level())) {
            return;
        }
        try {
            Map<String, Object> logData = new LinkedHashMap<>();
            logData.put("type", "request");
            logData.put("timestamp", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            logData.put("url", requestSnapshot.url());
            logData.put("method", requestSnapshot.httpMethod());
            logData.put("ip", requestSnapshot.clientIp());
            logData.put("user", buildUserInfo());
            logData.put("classMethod", requestSnapshot.classMethod());
            logData.put("headers", buildHeaders(requestSnapshot.request(), policy.maxLen()));
            if (policy.logArgs()) {
                logData.put("args", formatArgsMap(joinPoint, policy.maxLen()));
            }
            logAt(policy.level(), OBJECT_MAPPER.writeValueAsString(logData));
        } catch (Exception ex) {
            log.debug("Skip request logging due to formatter error", ex);
        }
    }

    private void safeLogResponse(LogPolicy policy, RequestSnapshot requestSnapshot, Object result, long startNanos) {
        if (!policy.canLogResponse() || !logResponseEnabled || !isLevelEnabled(policy.level())) {
            return;
        }
        try {
            long durationMillis = elapsedMillis(startNanos);
            Map<String, Object> logData = new LinkedHashMap<>();
            logData.put("type", "response");
            logData.put("timestamp", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            logData.put("url", requestSnapshot.url());
            logData.put("method", requestSnapshot.httpMethod());
            logData.put("timeConsumedMs", durationMillis);
            if (policy.logResult()) {
                logData.put("responseBody", formatResult(result, policy.maxLen()));
            }
            logAt(policy.level(), OBJECT_MAPPER.writeValueAsString(logData));
        } catch (Exception ex) {
            log.debug("Skip response logging due to formatter error", ex);
        }
    }

    private void safeLogException(LogPolicy policy, RequestSnapshot requestSnapshot, Throwable throwable, long startNanos) {
        if (!policy.enabled()) {
            return;
        }
        try {
            long durationMillis = elapsedMillis(startNanos);
            Map<String, Object> logData = new LinkedHashMap<>();
            logData.put("type", "exception");
            logData.put("timestamp", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            logData.put("url", requestSnapshot.url());
            logData.put("method", requestSnapshot.httpMethod());
            logData.put("timeConsumedMs", durationMillis);
            logData.put("exceptionType", throwable.getClass().getName());
            logData.put("message", limitLength(throwable.getMessage(), defaultMaxLen));
            logAt(ControllerLog.Level.WARN, OBJECT_MAPPER.writeValueAsString(logData));
        } catch (Exception ex) {
            log.debug("Skip exception logging due to formatter error", ex);
        }
    }

    private String buildUserInfo() {
        BaseContext.UserInfo user = BaseContext.getCurrentUser();
        if (user == null) {
            return "guest";
        }
        return String.format("%s (%s) [Role: %s]",
                user.getUserId(),
                defaultText(user.getUsername(), "-"),
                defaultText(user.getRole(), "-"));
    }

    private Object formatArgsMap(ProceedingJoinPoint joinPoint, int maxLen) {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return new HashMap<>();
        }
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Map<String, Object> argsMap = new LinkedHashMap<>();
        
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (shouldSerialize(arg)) {
                String paramName = (parameterNames != null && i < parameterNames.length) ? parameterNames[i] : "arg" + i;
                argsMap.put(paramName, arg);
            }
        }
        if (argsMap.isEmpty()) {
            return new HashMap<>();
        }
        // Serialize to apply masking and length limits, then deserialize back to Object/JsonNode to keep it as a structured JSON field.
        try {
            String json = OBJECT_MAPPER.writeValueAsString(argsMap);
            String sanitized = sanitizeJson(json);
            if (sanitized.length() > maxLen && maxLen > 0) {
                return truncate(sanitized, maxLen);
            }
            return OBJECT_MAPPER.readTree(sanitized);
        } catch (Exception e) {
            return truncate(argsMap.toString(), maxLen);
        }
    }

    private boolean shouldSerialize(Object arg) {
        return !(arg instanceof HttpServletRequest)
                && !(arg instanceof HttpServletResponse)
                && !(arg instanceof MultipartFile);
    }

    private Object formatResult(Object result, int maxLen) {
        if (result == null) {
            return null;
        }
        if (result instanceof SseEmitter) {
            return "SseEmitter";
        }
        if (result instanceof StreamingResponseBody) {
            return "StreamingResponseBody";
        }
        if (result instanceof byte[]) {
            return "byte[]";
        }
        try {
            String json = OBJECT_MAPPER.writeValueAsString(result);
            String sanitized = sanitizeJson(json);
            if (sanitized.length() > maxLen && maxLen > 0) {
                return truncate(sanitized, maxLen);
            }
            return OBJECT_MAPPER.readTree(sanitized);
        } catch (Exception ex) {
            return truncate(String.valueOf(result), maxLen);
        }
    }

    private String sanitizeJson(String raw) {
        if (!StringUtils.hasText(raw)) {
            return raw;
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(raw);
            maskSensitiveFields(node);
            return OBJECT_MAPPER.writeValueAsString(node);
        } catch (Exception ignored) {
            return fallbackMask(raw);
        }
    }

    private void maskSensitiveFields(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            objectNode.fieldNames().forEachRemaining(fieldName -> {
                if (isSensitiveKey(fieldName)) {
                    objectNode.put(fieldName, MASK);
                    return;
                }
                maskSensitiveFields(objectNode.get(fieldName));
            });
            return;
        }
        if (node.isArray()) {
            node.forEach(this::maskSensitiveFields);
        }
    }

    private String fallbackMask(String raw) {
        String masked = raw;
        for (String key : SENSITIVE_KEYS) {
            masked = masked.replaceAll("(?i)(\"" + java.util.regex.Pattern.quote(key) + "\"\\s*:\\s*\")([^\"]*)(\")", "$1" + MASK + "$3");
        }
        return masked;
    }

    private boolean isSensitiveKey(String key) {
        if (!StringUtils.hasText(key)) {
            return false;
        }
        String normalized = key.toLowerCase(Locale.ROOT);
        return SENSITIVE_KEYS.stream().anyMatch(normalized::contains);
    }

    private Map<String, String> buildHeaders(HttpServletRequest request, int maxLen) {
        Map<String, String> collectedHeaders = new LinkedHashMap<>();
        if (request == null) {
            return collectedHeaders;
        }
        for (String headerName : HEADER_WHITELIST) {
            String value = request.getHeader(headerName);
            if (StringUtils.hasText(value)) {
                String sanitizedValue = isSensitiveKey(headerName) ? MASK : value;
                collectedHeaders.put(headerName, truncate(sanitizedValue, maxLen));
            }
        }
        return collectedHeaders;
    }

    private LogPolicy resolvePolicy(ProceedingJoinPoint joinPoint) {
        if (!(joinPoint.getSignature() instanceof MethodSignature signature)) {
            return new LogPolicy(true, true, true, ControllerLog.Level.INFO, defaultMaxLen);
        }
        Method sourceMethod = signature.getMethod();
        Class<?> targetClass = joinPoint.getTarget() != null ? joinPoint.getTarget().getClass() : sourceMethod.getDeclaringClass();
        Method targetMethod = resolveTargetMethod(targetClass, sourceMethod);

        if (targetMethod.isAnnotationPresent(NoControllerLog.class) || targetClass.isAnnotationPresent(NoControllerLog.class)) {
            return new LogPolicy(false, false, false, ControllerLog.Level.INFO, defaultMaxLen);
        }

        ControllerLog controllerLog = targetMethod.getAnnotation(ControllerLog.class);
        if (controllerLog == null) {
            controllerLog = targetClass.getAnnotation(ControllerLog.class);
        }
        if (controllerLog == null) {
            return new LogPolicy(true, true, true, ControllerLog.Level.INFO, defaultMaxLen);
        }
        
        int resolvedMaxLen = controllerLog.maxLen() > 0 ? controllerLog.maxLen() : defaultMaxLen;
        return new LogPolicy(
                controllerLog.enabled(),
                controllerLog.logArgs(),
                controllerLog.logResult(),
                controllerLog.level(),
                resolvedMaxLen
        );
    }

    private Method resolveTargetMethod(Class<?> targetClass, Method method) {
        try {
            return targetClass.getMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException ex) {
            return method;
        }
    }

    private boolean isLevelEnabled(ControllerLog.Level level) {
        if (level == ControllerLog.Level.DEBUG) {
            return log.isDebugEnabled();
        }
        if (level == ControllerLog.Level.WARN) {
            return log.isWarnEnabled();
        }
        return log.isInfoEnabled();
    }

    private void logAt(ControllerLog.Level level, String message) {
        if (level == ControllerLog.Level.DEBUG) {
            log.debug(message);
            return;
        }
        if (level == ControllerLog.Level.WARN) {
            log.warn(message);
            return;
        }
        log.info(message);
    }

    private long elapsedMillis(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    private String truncate(String value, int maxLen) {
        if (value == null) {
            return "null";
        }
        int limit = maxLen > 0 ? maxLen : defaultMaxLen;
        // -1 means no limit
        if (limit <= 0) {
            return value;
        }
        if (value.length() <= limit) {
            return value;
        }
        return value.substring(0, limit) + "... (truncated)";
    }

    private String limitLength(String value, int maxLen) {
        return value == null ? "-" : truncate(value, maxLen);
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }

    private record RequestSnapshot(
            HttpServletRequest request,
            String url,
            String httpMethod,
            String clientIp,
            String classMethod
    ) {
        static RequestSnapshot from(ProceedingJoinPoint joinPoint) {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
            String classMethod = joinPoint.getTarget().getClass().getName() + "." + joinPoint.getSignature().getName();
            String clientIp = request != null ? request.getRemoteAddr() : UNKNOWN;
            if ("0:0:0:0:0:0:0:1".equals(clientIp)) {
                clientIp = "127.0.0.1";
            }
            return new RequestSnapshot(
                    request,
                    request != null ? request.getRequestURL().toString() : UNKNOWN,
                    request != null ? request.getMethod() : UNKNOWN,
                    clientIp,
                    classMethod
            );
        }
    }

    private record TraceScope(boolean created) {
        static TraceScope open() {
            String traceId = MDC.get(TRACE_ID);
            if (StringUtils.hasText(traceId)) {
                return new TraceScope(false);
            }
            MDC.put(TRACE_ID, UUID.randomUUID().toString().replace("-", ""));
            return new TraceScope(true);
        }

        void close() {
            if (created) {
                MDC.remove(TRACE_ID);
            }
        }
    }

    private record LogPolicy(
            boolean enabled,
            boolean logArgs,
            boolean logResult,
            ControllerLog.Level level,
            int maxLen
    ) {
        boolean canLogRequest() {
            return enabled;
        }

        boolean canLogResponse() {
            return enabled;
        }
    }
}
