package com.dormrepair.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dormrepair.dto.Requests;
import com.dormrepair.entity.Role;
import com.dormrepair.entity.User;
import com.dormrepair.mapper.RoleMapper;
import com.dormrepair.mapper.UserMapper;
import com.dormrepair.support.BusinessException;
import com.dormrepair.support.Roles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class UserAdminService {
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final OperationLogService operationLogService;

    public UserAdminService(UserMapper userMapper,
                            RoleMapper roleMapper,
                            PasswordEncoder passwordEncoder,
                            OperationLogService operationLogService) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
        this.operationLogService = operationLogService;
    }

    public IPage<Map<String, Object>> page(long current, long size, String keyword, String roleCode, String status) {
        return userMapper.selectUserPage(new Page<>(current, size), keyword, roleCode, status);
    }

    @Transactional
    public User create(Requests.UserCreateRequest request) {
        if (userMapper.findByUsername(request.username()) != null) {
            throw new BusinessException("账号已存在");
        }
        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        fillUser(user, request.name(), request.studentNo(), request.phone(), request.roleId(), request.dormRoomId(), request.status());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        operationLogService.log("CREATE_USER", "USER", user.getId(), "新增用户：" + user.getUsername());
        return withoutPassword(user);
    }

    @Transactional
    public User update(Long id, Requests.UserUpdateRequest request) {
        User user = requireUser(id);
        Role oldRole = roleMapper.selectById(user.getRoleId());
        Role newRole = roleMapper.selectById(request.roleId());
        if (oldRole != null && Roles.SYS_ADMIN.equals(oldRole.getCode()) && "ACTIVE".equals(user.getStatus())
                && (newRole == null || !Roles.SYS_ADMIN.equals(newRole.getCode()) || "DISABLED".equals(request.status()))
                && countActiveSysAdmins() <= 1) {
            throw new BusinessException("不能移除最后一个有效系统管理员");
        }
        fillUser(user, request.name(), request.studentNo(), request.phone(), request.roleId(), request.dormRoomId(), request.status());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        operationLogService.log("UPDATE_USER", "USER", id, "修改用户：" + user.getUsername());
        return withoutPassword(user);
    }

    @Transactional
    public void disable(Long id) {
        User user = requireUser(id);
        Role role = roleMapper.selectById(user.getRoleId());
        if (role != null && Roles.SYS_ADMIN.equals(role.getCode()) && countActiveSysAdmins() <= 1) {
            throw new BusinessException("不能禁用最后一个系统管理员账号");
        }
        user.setStatus("DISABLED");
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        operationLogService.log("DISABLE_USER", "USER", id, "禁用用户：" + user.getUsername());
    }

    @Transactional
    public void delete(Long id) {
        User user = requireUser(id);
        Role role = roleMapper.selectById(user.getRoleId());
        if (role != null && Roles.SYS_ADMIN.equals(role.getCode())) {
            throw new BusinessException("系统管理员账号不允许删除，可改为禁用其他账号");
        }
        userMapper.deleteById(id);
        operationLogService.log("DELETE_USER", "USER", id, "删除用户：" + user.getUsername());
    }

    @Transactional
    public void resetPassword(Long id, Requests.PasswordResetRequest request) {
        User user = requireUser(id);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        operationLogService.log("RESET_PASSWORD", "USER", id, "重置用户密码：" + user.getUsername());
    }

    private void fillUser(User user, String name, String studentNo, String phone, Long roleId, Long dormRoomId, String status) {
        if (roleMapper.selectById(roleId) == null) {
            throw new BusinessException("角色不存在");
        }
        if (status != null && !status.isBlank() && !java.util.Set.of("ACTIVE", "DISABLED").contains(status)) {
            throw new BusinessException("账号状态只能为 ACTIVE 或 DISABLED");
        }
        user.setName(name);
        user.setStudentNo(studentNo);
        user.setPhone(phone);
        user.setRoleId(roleId);
        user.setDormRoomId(dormRoomId);
        user.setStatus(status == null || status.isBlank() ? "ACTIVE" : status);
    }

    private User requireUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    private long countActiveSysAdmins() {
        Role sysAdmin = roleMapper.findByCode(Roles.SYS_ADMIN);
        return userMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                .eq("role_id", sysAdmin.getId()).eq("status", "ACTIVE"));
    }

    private User withoutPassword(User user) {
        user.setPasswordHash(null);
        return user;
    }
}
