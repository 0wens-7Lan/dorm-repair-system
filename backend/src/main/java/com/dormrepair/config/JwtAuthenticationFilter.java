package com.dormrepair.config;
// AI-assisted: loads live account and permission data on each authenticated request.
import com.dormrepair.mapper.*;
import com.dormrepair.support.*;
import com.dormrepair.entity.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final TokenService tokens; private final UserMapper users; private final RoleMapper roles; private final ObjectMapper json;
    public JwtAuthenticationFilter(TokenService tokens, UserMapper users, RoleMapper roles, ObjectMapper json) {
        this.tokens=tokens; this.users=users; this.roles=roles; this.json=json;
    }
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        try {
            String token=null;
            if(req.getCookies()!=null) for(var c:req.getCookies()) if(c.getName().equals("DORM_AUTH")) token=c.getValue();
            String header=req.getHeader("Authorization");
            if(header!=null && header.startsWith("Bearer ")) token=header.substring(7);
            // Public login/registration must remain usable when a browser carries an expired cookie.
            boolean publicAuth=Set.of("/api/auth/login","/api/auth/register","/api/auth/csrf","/api/auth/logout").contains(req.getServletPath());
            if(token!=null && !publicAuth) {
                try {
                    var payload=tokens.parse(token); var user=users.selectById(payload.userId());
                    if(user==null || !"ACTIVE".equals(user.getStatus())) throw new BusinessException(401,"账号不存在或已禁用");
                    var role=roles.selectById(user.getRoleId());
                    if(role==null || !role.getCode().equals(payload.roleCode())) throw new BusinessException(401,"角色已变更，请重新登录");
                    var principal=new CurrentUser(user.getId(),user.getUsername(),user.getName(),role.getCode());
                    var authorities=new ArrayList<SimpleGrantedAuthority>();
                    authorities.add(new SimpleGrantedAuthority("ROLE_"+role.getCode()));
                    roles.permissions(role.getId()).forEach(p->authorities.add(new SimpleGrantedAuthority(p)));
                    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,null,authorities));
                    AuthContext.set(principal);
                } catch(BusinessException ex) {
                    res.setStatus(401);res.setContentType("application/json;charset=UTF-8");json.writeValue(res.getWriter(),ApiResult.fail(401,ex.getMessage()));return;
                }
            }
            chain.doFilter(req,res);
        } finally { AuthContext.clear(); SecurityContextHolder.clearContext(); }
    }
}
