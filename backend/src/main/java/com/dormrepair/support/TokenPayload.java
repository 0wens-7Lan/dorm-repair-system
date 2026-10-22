package com.dormrepair.support;

public record TokenPayload(Long userId, String username, String roleCode, long expiresAt) {
}
