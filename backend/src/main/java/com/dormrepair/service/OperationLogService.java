package com.dormrepair.service;

import com.dormrepair.entity.OperationLog;
import com.dormrepair.mapper.OperationLogMapper;
import com.dormrepair.support.AuthContext;
import com.dormrepair.support.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Service
public class OperationLogService {
    private final OperationLogMapper operationLogMapper;

    public OperationLogService(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    public void log(String action, String bizType, Long bizId, String detail) {
        CurrentUser user = AuthContext.user();
        logAs(user.getId(), user.getUsername(), user.getRoleCode(), action, bizType, bizId, detail);
    }

    public void logAs(Long userId, String username, String roleCode, String action, String bizType, Long bizId, String detail) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setRoleCode(roleCode);
        log.setAction(action);
        log.setBizType(bizType);
        log.setBizId(bizId);
        log.setDetail(detail);
        log.setIp(clientIp());
        log.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(log);
    }

    private String clientIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            String forwarded = attrs.getRequest().getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return attrs.getRequest().getRemoteAddr();
        }
        return "unknown";
    }
}
