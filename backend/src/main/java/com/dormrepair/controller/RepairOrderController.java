package com.dormrepair.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dormrepair.dto.Requests;
import com.dormrepair.entity.RepairOrder;
import com.dormrepair.service.RepairOrderService;
import com.dormrepair.support.ApiResult;
import org.springframework.security.access.prepost.PreAuthorize;
import com.dormrepair.support.Roles;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/repair-orders")
public class RepairOrderController {
    private final RepairOrderService repairOrderService;

    public RepairOrderController(RepairOrderService repairOrderService) {
        this.repairOrderService = repairOrderService;
    }

    @GetMapping
    public ApiResult<IPage<Map<String, Object>>> page(@RequestParam(defaultValue = "1") long current,
                                                      @RequestParam(defaultValue = "10") long size,
                                                      @RequestParam(required = false) Long categoryId,
                                                      @RequestParam(required = false) Long buildingId,
                                                      @RequestParam(required = false) String status,
                                                      @RequestParam(required = false) String studentName,
                                                      @RequestParam(required = false) Long repairerId,
                                                      @RequestParam(required = false) String startDate,
                                                      @RequestParam(required = false) String endDate) {
        return ApiResult.ok(repairOrderService.page(current, size, categoryId, buildingId, status,
                studentName, repairerId, startDate, endDate));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDENT')")
    public ApiResult<RepairOrder> create(@Valid @RequestBody Requests.CreateRepairRequest request) {
        return ApiResult.ok(repairOrderService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResult.ok(repairOrderService.detail(id));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('STUDENT')")
    public ApiResult<RepairOrder> cancel(@PathVariable Long id) {
        return ApiResult.ok(repairOrderService.cancel(id));
    }

    @PutMapping("/{id}/audit")
    @PreAuthorize("hasAnyRole('DORM_ADMIN','SYS_ADMIN')")
    public ApiResult<RepairOrder> audit(@PathVariable Long id, @Valid @RequestBody Requests.AuditRequest request) {
        return ApiResult.ok(repairOrderService.audit(id, request));
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('DORM_ADMIN','SYS_ADMIN')")
    public ApiResult<RepairOrder> assign(@PathVariable Long id, @Valid @RequestBody Requests.AssignRequest request) {
        return ApiResult.ok(repairOrderService.assign(id, request));
    }

    @PutMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('REPAIRER')")
    public ApiResult<RepairOrder> start(@PathVariable Long id) {
        return ApiResult.ok(repairOrderService.start(id));
    }

    @PutMapping("/{id}/finish")
    @PreAuthorize("hasAnyRole('REPAIRER')")
    public ApiResult<RepairOrder> finish(@PathVariable Long id, @Valid @RequestBody Requests.FinishRepairRequest request) {
        return ApiResult.ok(repairOrderService.finish(id, request));
    }

    @PutMapping("/{id}/feedback")
    @PreAuthorize("hasAnyRole('STUDENT')")
    public ApiResult<RepairOrder> feedback(@PathVariable Long id, @Valid @RequestBody Requests.FeedbackRequest request) {
        return ApiResult.ok(repairOrderService.feedback(id, request));
    }

    @PutMapping("/{id}/reopen")
    @PreAuthorize("hasAnyRole('DORM_ADMIN','SYS_ADMIN')")
    public ApiResult<RepairOrder> reopen(@PathVariable Long id, @Valid @RequestBody Requests.ReopenRequest request) {
        return ApiResult.ok(repairOrderService.reopen(id, request));
    }
}
