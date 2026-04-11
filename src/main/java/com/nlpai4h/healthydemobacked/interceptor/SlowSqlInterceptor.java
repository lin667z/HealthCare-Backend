package com.nlpai4h.healthydemobacked.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Intercepts({
        @Signature(type = StatementHandler.class, method = "query", args = {Statement.class, org.apache.ibatis.session.ResultHandler.class}),
        @Signature(type = StatementHandler.class, method = "update", args = {Statement.class}),
        @Signature(type = StatementHandler.class, method = "batch", args = {Statement.class})
})
public class SlowSqlInterceptor implements Interceptor {

    @Value("${app.log.slow-sql.threshold-ms:1000}")
    private long thresholdMs;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long startNanos = System.nanoTime();
        Object result = invocation.proceed();
        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

        if (durationMs >= thresholdMs) {
            try {
                StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
                BoundSql boundSql = statementHandler.getBoundSql();
                String sql = boundSql.getSql().replaceAll("[\\s]+", " ");
                log.warn("Slow SQL detected! Time Consumed: {} ms, SQL: {}", durationMs, sql);
            } catch (Exception e) {
                log.warn("Slow SQL detected! Time Consumed: {} ms, but failed to extract SQL.", durationMs);
            }
        }
        return result;
    }

    @Override
    public Object plugin(Object target) {
        return Interceptor.super.plugin(target);
    }

    @Override
    public void setProperties(Properties properties) {
        Interceptor.super.setProperties(properties);
    }
}
