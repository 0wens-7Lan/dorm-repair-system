package com.dormrepair.config;
// AI-assisted: generated schemas follow real controller DTOs; HTTP tests inspect the published paths.
import org.springframework.context.annotation.*;
import org.springdoc.core.customizers.OpenApiCustomizer;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
@Configuration
public class OpenApiConfig {
 @Bean OpenAPI openAPI() {
  return new OpenAPI().info(new Info().title("高校宿舍报修 REST API").version("1.1.0")
   .description("先 GET /api/auth/csrf 获取 CSRF token，再在写请求中携带 X-XSRF-TOKEN。登录成功由服务端写入 HttpOnly Cookie，响应不返回 JWT。浏览器请求须 withCredentials=true。"))
   .components(new Components().addSecuritySchemes("cookieAuth",new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.COOKIE).name("DORM_AUTH")));
 }
 @Bean OpenApiCustomizer securityAndErrors() {
  return api->api.getPaths().forEach((path,item)->item.readOperationsMap().forEach((method,operation)->{
   if(!path.startsWith("/api/public/") && !java.util.Set.of("/api/auth/login","/api/auth/register","/api/auth/csrf","/api/auth/logout","/api/health","/").contains(path))
     operation.addSecurityItem(new SecurityRequirement().addList("cookieAuth"));
   if(java.util.Set.of(PathItem.HttpMethod.POST,PathItem.HttpMethod.PUT,PathItem.HttpMethod.DELETE).contains(method))
     operation.addParametersItem(new Parameter().in("header").name("X-XSRF-TOKEN").required(true).description("由 /api/auth/csrf 获取，并与同一客户端 Cookie 配套使用").schema(new StringSchema()));
   operation.getResponses().addApiResponse("400",new ApiResponse().description("参数校验失败或业务状态不允许"));
   operation.getResponses().addApiResponse("401",new ApiResponse().description("未登录、JWT 无效/过期或账号被禁用"));
   operation.getResponses().addApiResponse("403",new ApiResponse().description("角色/权限/数据归属不允许或 CSRF 校验失败"));
   operation.getResponses().addApiResponse("500",new ApiResponse().description("服务器内部异常，不返回内部错误细节"));
  }));
 }
}
