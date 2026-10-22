package com.dormrepair;
// AI-assisted: meaningful CRUD/password and last-admin safeguards.
import com.dormrepair.service.*;
import com.dormrepair.mapper.*;
import com.dormrepair.entity.*;
import com.dormrepair.dto.Requests;
import com.dormrepair.support.*;
import org.junit.jupiter.api.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class UserAdminServiceTest {
 UserMapper users;RoleMapper roles;UserAdminService service;
 @BeforeEach void setup(){users=mock(UserMapper.class);roles=mock(RoleMapper.class);service=new UserAdminService(users,roles,new BCryptPasswordEncoder(),mock(OperationLogService.class));}
 @Test void createHashesPasswordAndHidesHash(){
  var role=new Role();role.setId(1L);when(roles.selectById(1L)).thenReturn(role);
  doAnswer(inv->{User u=inv.getArgument(0);assertTrue(new BCryptPasswordEncoder().matches("password",u.getPasswordHash()));u.setId(7L);return 1;}).when(users).insert(any(User.class));
  var u=service.create(new Requests.UserCreateRequest("newstudent","password","测试",null,null,1L,null,"ACTIVE"));assertNull(u.getPasswordHash());verify(users).insert(any(User.class));
 }
 @Test void missingUserRejected(){assertThrows(BusinessException.class,()->service.delete(999L));}
 @Test void systemAdminDeletionRejected(){var u=new User();u.setId(1L);u.setRoleId(4L);var r=new Role();r.setCode("SYS_ADMIN");when(users.selectById(1L)).thenReturn(u);when(roles.selectById(4L)).thenReturn(r);assertThrows(BusinessException.class,()->service.delete(1L));verify(users,never()).deleteById(anyLong());}
}
