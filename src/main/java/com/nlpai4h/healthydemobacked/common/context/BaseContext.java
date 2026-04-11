package com.nlpai4h.healthydemobacked.common.context;

import lombok.Data;

/**
 * 线程上下文，用于存储当前登录用户信息
 */
public class BaseContext {

    @Data
    public static class UserInfo {
        private Long userId;
        private String username;
        private String role;
    }

    public static ThreadLocal<UserInfo> threadLocal = new ThreadLocal<>();

    public static void setCurrentUser(Long userId, String username, String role) {
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(userId);
        userInfo.setUsername(username);
        userInfo.setRole(role);
        threadLocal.set(userInfo);
    }

    public static UserInfo getCurrentUser() {
        return threadLocal.get();
    }

    public static Long getCurrentId() {
        UserInfo userInfo = threadLocal.get();
        return userInfo != null ? userInfo.getUserId() : null;
    }

    public static String getCurrentUsername() {
        UserInfo userInfo = threadLocal.get();
        return userInfo != null ? userInfo.getUsername() : null;
    }

    public static void removeCurrentUser() {
        threadLocal.remove();
    }

}
