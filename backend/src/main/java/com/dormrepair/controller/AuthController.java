package com.dormrepair.controller;

import com.dormrepair.dto.Requests;
import com.dormrepair.entity.Role;
import com.dormrepair.entity.User;
import com.dormrepair.mapper.RoleMapper;
import com.dormrepair.mapper.UserMapper;
import com.dormrepair.service.AuthService;
import com.dormrepair.support.ApiResult;
import com.dormrepair.support.AuthContext;
import com.dormrepair.support.BusinessException;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.csrf.CsrfToken;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Value("${app.auth.cookie-secure:false}") private boolean cookieSecure;
    @Value("${app.auth.token-expire-hours:12}") private long expireHours;
    private final AuthService authService;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;

    public AuthController(AuthService authService, UserMapper userMapper, RoleMapper roleMapper) {
        this.authService = authService;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
    }

    @PostMapping("/login")
    public ApiResult<Map<String, Object>> login(@Valid @RequestBody Requests.LoginRequest request, HttpServletResponse response) {
        var result=authService.login(request);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie((String)result.remove("token"), expireHours*3600).toString());
        return ApiResult.ok(result);
    }

    @PostMapping("/register")
    public ApiResult<Map<String, Object>> register(@Valid @RequestBody Requests.RegisterRequest request) {
        return ApiResult.ok(authService.register(request));
    }

    private ResponseCookie cookie(String value,long seconds) {
        return ResponseCookie.from("DORM_AUTH",value).httpOnly(true).secure(cookieSecure).sameSite("Lax").path("/api").maxAge(seconds).build();
    }
    @GetMapping("/csrf")
    public ApiResult<Map<String,String>> csrf(CsrfToken token) {
        return ApiResult.ok(Map.of("token",token.getToken(),"headerName",token.getHeaderName()));
    }
    @PostMapping("/logout")
    public ApiResult<Void> logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE,cookie("",0).toString()); return ApiResult.ok();
    }
    @GetMapping("/me")
    public ApiResult<Map<String, Object>> me() {
        User user = userMapper.selectById(AuthContext.user().getId());
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        Role role = roleMapper.selectById(user.getRoleId());
        return ApiResult.ok(authService.safeUser(user, role));
    }
}
