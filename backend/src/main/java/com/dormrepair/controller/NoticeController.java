package com.dormrepair.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dormrepair.dto.Requests;
import com.dormrepair.entity.Notice;
import com.dormrepair.service.NoticeService;
import com.dormrepair.support.ApiResult;
import org.springframework.security.access.prepost.PreAuthorize;
import com.dormrepair.support.Roles;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {
    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping
    public ApiResult<IPage<Notice>> page(@RequestParam(defaultValue = "1") long current,
                                         @RequestParam(defaultValue = "10") long size,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) String status) {
        return ApiResult.ok(noticeService.page(current, size, keyword, status));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DORM_ADMIN','SYS_ADMIN')")
    public ApiResult<Notice> create(@Valid @RequestBody Requests.NoticeRequest request) {
        return ApiResult.ok(noticeService.save(null, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DORM_ADMIN','SYS_ADMIN')")
    public ApiResult<Notice> update(@PathVariable Long id, @Valid @RequestBody Requests.NoticeRequest request) {
        return ApiResult.ok(noticeService.save(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DORM_ADMIN','SYS_ADMIN')")
    public ApiResult<Void> delete(@PathVariable Long id) {
        noticeService.delete(id);
        return ApiResult.ok();
    }
}
