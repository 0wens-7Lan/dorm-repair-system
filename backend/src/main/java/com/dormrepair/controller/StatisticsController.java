package com.dormrepair.controller;

import com.dormrepair.service.RepairOrderService;
import com.dormrepair.support.ApiResult;
import org.springframework.security.access.prepost.PreAuthorize;
import com.dormrepair.support.Roles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@PreAuthorize("hasAnyRole('DORM_ADMIN','SYS_ADMIN')")
public class StatisticsController {
    private final RepairOrderService repairOrderService;

    public StatisticsController(RepairOrderService repairOrderService) {
        this.repairOrderService = repairOrderService;
    }

    @GetMapping("/overview")
    public ApiResult<Map<String, Object>> overview() {
        return ApiResult.ok(repairOrderService.statistics());
    }
}
