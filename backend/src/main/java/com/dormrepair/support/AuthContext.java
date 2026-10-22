package com.dormrepair.support;

public final class AuthContext {
    private static final ThreadLocal<CurrentUser> CURRENT = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(CurrentUser user) {
        CURRENT.set(user);
    }

    public static CurrentUser user() {
        CurrentUser user = CURRENT.get();
        if (user == null) {
            throw new BusinessException(401, "请先登录");
        }
        return user;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
