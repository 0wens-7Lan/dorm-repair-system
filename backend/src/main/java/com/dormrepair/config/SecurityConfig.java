package com.dormrepair.config;
// AI-assisted: Spring Security RBAC, HttpOnly JWT and CSRF configuration.
import com.dormrepair.mapper.*;
import com.dormrepair.support.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.*;
import org.springframework.web.cors.*;
import java.util.*;
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain security(HttpSecurity http, TokenService tokens, UserMapper users, RoleMapper roles, ObjectMapper json,
        @Value("${app.auth.cookie-secure:false}") boolean secure) throws Exception {
        var csrf=new CookieCsrfTokenRepository();
        csrf.setCookieCustomizer(c->c.httpOnly(true).secure(secure).sameSite("Lax").path("/"));
        http.cors(c->{}).csrf(c->c.csrfTokenRepository(csrf).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .sessionManagement(c->c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(c->c.disable()).httpBasic(c->c.disable()).logout(c->c.disable())
            .authorizeHttpRequests(a->a
                .requestMatchers("/api/auth/login","/api/auth/register","/api/auth/csrf","/api/auth/logout","/api/public/**","/api/health","/v3/api-docs/**","/swagger-ui/**","/swagger-ui.html","/error","/").permitAll()
                .requestMatchers("/api/users/**").hasAuthority("user:manage")
                .requestMatchers("/api/logs/**").hasAuthority("log:read")
                .requestMatchers("/api/statistics/**").hasAuthority("statistics:read")
                .anyRequest().authenticated())
            .exceptionHandling(e->e
                .authenticationEntryPoint((req,res,ex)->{res.setStatus(401);res.setContentType("application/json;charset=UTF-8");json.writeValue(res.getWriter(),ApiResult.fail(401,"请先登录"));})
                .accessDeniedHandler((req,res,ex)->{res.setStatus(403);res.setContentType("application/json;charset=UTF-8");json.writeValue(res.getWriter(),ApiResult.fail(403,"无权访问或 CSRF 校验失败"));}))
            .addFilterBefore(new JwtAuthenticationFilter(tokens,users,roles,json),UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") String origins) {
        var c=new CorsConfiguration();c.setAllowedOrigins(Arrays.asList(origins.split(",")));
        c.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        c.setAllowedHeaders(List.of("Content-Type","Authorization","X-XSRF-TOKEN"));c.setAllowCredentials(true);
        var source=new UrlBasedCorsConfigurationSource();source.registerCorsConfiguration("/**",c);return source;
    }
}
