package com.dormrepair.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dormrepair.mapper.OperationLogMapper;
import com.dormrepair.support.ApiResult;
import org.springframework.security.access.prepost.PreAuthorize;
import com.dormrepair.support.Roles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/logs")
@PreAuthorize("hasAnyRole('SYS_ADMIN')")
public class OperationLogController {
    private final OperationLogMapper operationLogMapper;

    public OperationLogController(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @GetMapping
    public ApiResult<IPage<Map<String, Object>>> page(@RequestParam(defaultValue = "1") long current,
                                                      @RequestParam(defaultValue = "10") long size,
                                                      @RequestParam(required = false) String action,
                                                      @RequestParam(required = false) String username) {
        return ApiResult.ok(operationLogMapper.selectLogPage(new Page<>(current, size), action, username));
    }
}
