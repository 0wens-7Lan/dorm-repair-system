package com.dormrepair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dormrepair.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {
    @Select("select * from role where code = #{code} limit 1")
    Role findByCode(String code);
    @Select("SELECT p.code FROM permission p JOIN role_permission rp ON rp.permission_id=p.id WHERE rp.role_id=#{roleId}")
    java.util.List<String> permissions(Long roleId);
}
