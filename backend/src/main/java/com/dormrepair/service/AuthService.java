package com.dormrepair.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dormrepair.dto.Requests;
import com.dormrepair.entity.Role;
import com.dormrepair.entity.User;
import com.dormrepair.mapper.RoleMapper;
import com.dormrepair.mapper.UserMapper;
import com.dormrepair.support.BusinessException;
import com.dormrepair.support.CurrentUser;
import com.dormrepair.support.Roles;
import com.dormrepair.support.TokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AuthService {
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final OperationLogService operationLogService;

    public AuthService(UserMapper userMapper,
                       RoleMapper roleMapper,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService,
                       OperationLogService operationLogService) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.operationLogService = operationLogService;
    }

    public Map<String, Object> login(Requests.LoginRequest request) {
        User user = userMapper.findByUsername(request.username());
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(401, "账号或密码错误");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(403, "账号已被禁用");
        }
        Role role = roleMapper.selectById(user.getRoleId());
        CurrentUser currentUser = new CurrentUser(user.getId(), user.getUsername(), user.getName(), role.getCode());
        String token = tokenService.createToken(currentUser);
        operationLogService.logAs(user.getId(), user.getUsername(), role.getCode(), "LOGIN", "USER", user.getId(), "用户登录");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("user", safeUser(user, role));
        return result;
    }

    @Transactional
    public Map<String, Object> register(Requests.RegisterRequest request) {
        if (userMapper.findByUsername(request.username()) != null) {
            throw new BusinessException("账号已存在");
        }
        Role role = roleMapper.findByCode(Roles.STUDENT);
        if (role == null) {
            throw new BusinessException("学生角色未初始化");
        }
        long duplicateStudentNo = userMapper.selectCount(new QueryWrapper<User>()
                .eq("student_no", request.studentNo()));
        if (duplicateStudentNo > 0) {
            throw new BusinessException("学号已被注册");
        }
        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setName(request.name());
        user.setStudentNo(request.studentNo());
        user.setPhone(request.phone());
        user.setDormRoomId(request.dormRoomId());
        user.setRoleId(role.getId());
        user.setStatus("ACTIVE");
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        operationLogService.logAs(user.getId(), user.getUsername(), role.getCode(), "REGISTER", "USER", user.getId(), "学生注册");
        return safeUser(user, role);
    }

    public Map<String, Object> safeUser(User user, Role role) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("name", user.getName());
        result.put("studentNo", user.getStudentNo());
        result.put("phone", user.getPhone());
        result.put("roleId", user.getRoleId());
        result.put("roleCode", role.getCode());
        result.put("roleName", role.getName());
        result.put("dormRoomId", user.getDormRoomId());
        result.put("status", user.getStatus());
        return result;
    }
}
