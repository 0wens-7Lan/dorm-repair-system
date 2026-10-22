package com.dormrepair.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dormrepair.entity.RepairCategory;
import com.dormrepair.mapper.RepairCategoryMapper;
import com.dormrepair.mapper.UserMapper;
import com.dormrepair.support.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meta")
public class MetaController {
    private final RepairCategoryMapper categoryMapper;
    private final UserMapper userMapper;

    public MetaController(RepairCategoryMapper categoryMapper, UserMapper userMapper) {
        this.categoryMapper = categoryMapper;
        this.userMapper = userMapper;
    }

    @GetMapping("/categories")
    public ApiResult<List<RepairCategory>> categories() {
        return ApiResult.ok(categoryMapper.selectList(new QueryWrapper<RepairCategory>()
                .eq("enabled", 1).orderByAsc("id")));
    }

    @GetMapping("/repairers")
    public ApiResult<List<Map<String, Object>>> repairers() {
        return ApiResult.ok(userMapper.selectActiveRepairers());
    }
}
