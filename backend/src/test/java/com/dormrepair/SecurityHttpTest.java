package com.dormrepair;
// AI-assisted: full Spring Security chain tested without an external database.
import com.dormrepair.mapper.*;
import com.dormrepair.entity.*;
import com.dormrepair.support.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.servlet.http.Cookie;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
@SpringBootTest(properties={"app.auth.token-secret=test-only-32-byte-minimum-secret-value"})
@AutoConfigureMockMvc
class SecurityHttpTest {
 @Autowired MockMvc mvc; @Autowired TokenService tokens;
 @MockBean UserMapper users; @MockBean RoleMapper roles;
 private Cookie student;
 @BeforeEach void setup(){
   var u=new User();u.setId(1L);u.setUsername("student");u.setName("学生");u.setStatus("ACTIVE");u.setRoleId(1L);
   var r=new Role();r.setId(1L);r.setCode("STUDENT");r.setName("学生");
   when(users.selectById(1L)).thenReturn(u);when(roles.selectById(1L)).thenReturn(r);when(roles.permissions(1L)).thenReturn(List.of());
   student=new Cookie("DORM_AUTH",tokens.createToken(new CurrentUser(1L,"student","学生","STUDENT")));
 }
 @Test void anonymousDenied() throws Exception {mvc.perform(get("/api/users")).andExpect(status().isUnauthorized());}
 @Test void forgedDenied() throws Exception {mvc.perform(get("/api/auth/me").cookie(new Cookie("DORM_AUTH","bad.jwt.value"))).andExpect(status().isUnauthorized());}
 @Test void currentIdentity() throws Exception {mvc.perform(get("/api/auth/me").cookie(student)).andExpect(status().isOk()).andExpect(jsonPath("$.data.username").value("student"));}
 @Test void studentCannotManageUsers() throws Exception {mvc.perform(get("/api/users").cookie(student)).andExpect(status().isForbidden());}
 @Test void methodRoleGuard() throws Exception {mvc.perform(post("/api/notices").cookie(student).with(csrf()).contentType("application/json").content("{\"title\":\"test\",\"content\":\"test\"}")).andExpect(status().isForbidden());}
 @Test void missingCsrfDenied() throws Exception {mvc.perform(post("/api/auth/login").contentType("application/json").content("{\"username\":\"student\",\"password\":\"password\"}")).andExpect(status().isForbidden());}
 @Test void disabledAccountDenied() throws Exception {var u=users.selectById(1L);u.setStatus("DISABLED");mvc.perform(get("/api/auth/me").cookie(student)).andExpect(status().isUnauthorized());}
 @Test void disallowedOriginDenied() throws Exception {mvc.perform(options("/api/auth/login").header("Origin","https://untrusted.example").header("Access-Control-Request-Method","POST")).andExpect(status().isForbidden());}
 @Test void allowedOrigin() throws Exception {mvc.perform(options("/api/auth/login").header("Origin","http://localhost:5173").header("Access-Control-Request-Method","POST")).andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Credentials","true"));}
 @Test void csrfEndpoint() throws Exception {mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andExpect(jsonPath("$.data.token").isNotEmpty());}
 @Test void openApiAccessible() throws Exception {mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andExpect(jsonPath("$.paths['/api/users']").exists());}
}
