package com.dormrepair.controller;

import com.dormrepair.entity.DormBuilding;
import com.dormrepair.entity.DormRoom;
import com.dormrepair.entity.Notice;
import com.dormrepair.entity.RepairCategory;
import com.dormrepair.mapper.RepairCategoryMapper;
import com.dormrepair.service.DormService;
import com.dormrepair.service.NoticeService;
import com.dormrepair.support.ApiResult;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public")
public class PublicController {
    private final DormService dormService;
    private final NoticeService noticeService;
    private final RepairCategoryMapper categoryMapper;

    public PublicController(DormService dormService, NoticeService noticeService, RepairCategoryMapper categoryMapper) {
        this.dormService = dormService;
        this.noticeService = noticeService;
        this.categoryMapper = categoryMapper;
    }

    @GetMapping("/dorm/buildings")
    public ApiResult<List<DormBuilding>> buildings() {
        return ApiResult.ok(dormService.buildings());
    }

    @GetMapping("/dorm/rooms")
    public ApiResult<List<DormRoom>> rooms(@RequestParam(required = false) Long buildingId) {
        return ApiResult.ok(dormService.roomsByBuilding(buildingId));
    }

    @GetMapping("/categories")
    public ApiResult<List<RepairCategory>> categories() {
        return ApiResult.ok(categoryMapper.selectList(new QueryWrapper<RepairCategory>()
                .eq("enabled", 1).orderByAsc("id")));
    }

    @GetMapping("/notices")
    public ApiResult<List<Notice>> notices() {
        return ApiResult.ok(noticeService.publicNotices());
    }
}
