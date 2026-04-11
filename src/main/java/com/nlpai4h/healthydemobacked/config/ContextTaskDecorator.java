package com.nlpai4h.healthydemobacked.config;

import com.nlpai4h.healthydemobacked.common.context.BaseContext;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;

import java.util.Map;

/**
 * 异步任务上下文装饰器
 * 用于在主线程和异步子线程之间传递上下文（包括 BaseContext 和 MDC 日志追踪）
 */
public class ContextTaskDecorator implements TaskDecorator {

    @Override
    @NonNull
    public Runnable decorate(@NonNull Runnable runnable) {
        // 1. 获取主线程的上下文信息
        BaseContext.UserInfo contextUser = BaseContext.getCurrentUser();
        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        // 2. 返回包装后的 Runnable
        return () -> {
            try {
                // 3. 在子线程中恢复上下文
                if (contextUser != null) {
                    BaseContext.setCurrentUser(
                            contextUser.getUserId(),
                            contextUser.getUsername(),
                            contextUser.getRole()
                    );
                }
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                
                // 执行实际的异步任务
                runnable.run();
            } finally {
                // 4. 执行完毕后，清理子线程上下文，防止线程复用导致的内存泄漏或数据污染
                BaseContext.removeCurrentUser();
                MDC.clear();
            }
        };
    }
}
