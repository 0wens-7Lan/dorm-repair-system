package com.dormrepair.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dormrepair.dto.Requests;
import com.dormrepair.entity.Role;
import com.dormrepair.entity.User;
import com.dormrepair.mapper.RoleMapper;
import com.dormrepair.service.UserAdminService;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasAnyRole('SYS_ADMIN')")
public class UserController {
    private final UserAdminService userAdminService;
    private final RoleMapper roleMapper;

    public UserController(UserAdminService userAdminService, RoleMapper roleMapper) {
        this.userAdminService = userAdminService;
        this.roleMapper = roleMapper;
    }

    @GetMapping
    public ApiResult<IPage<Map<String, Object>>> page(@RequestParam(defaultValue = "1") long current,
                                                      @RequestParam(defaultValue = "10") long size,
                                                      @RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) String roleCode,
                                                      @RequestParam(required = false) String status) {
        return ApiResult.ok(userAdminService.page(current, size, keyword, roleCode, status));
    }

    @GetMapping("/roles")
    public ApiResult<List<Role>> roles() {
        return ApiResult.ok(roleMapper.selectList(null));
    }

    @PostMapping
    public ApiResult<User> create(@Valid @RequestBody Requests.UserCreateRequest request) {
        return ApiResult.ok(userAdminService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResult<User> update(@PathVariable Long id, @Valid @RequestBody Requests.UserUpdateRequest request) {
        return ApiResult.ok(userAdminService.update(id, request));
    }

    @PutMapping("/{id}/disable")
    public ApiResult<Void> disable(@PathVariable Long id) {
        userAdminService.disable(id);
        return ApiResult.ok();
    }

    @PutMapping("/{id}/reset-password")
    public ApiResult<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody Requests.PasswordResetRequest request) {
        userAdminService.resetPassword(id, request);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        userAdminService.delete(id);
        return ApiResult.ok();
    }
}
