package com.dormrepair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dormrepair.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    @Select("select * from user where username = #{username} limit 1")
    User findByUsername(String username);

    @Select("""
            <script>
            select u.id, u.username, u.name, u.student_no studentNo, u.phone, u.status,
                   u.dorm_room_id dormRoomId, r.code roleCode, r.name roleName,
                   b.name buildingName, dr.room_no roomNo
            from user u
            left join role r on r.id = u.role_id
            left join dorm_room dr on dr.id = u.dorm_room_id
            left join dorm_building b on b.id = dr.building_id
            where 1=1
            <if test='keyword != null and keyword != ""'>
              and (u.username like concat('%', #{keyword}, '%') or u.name like concat('%', #{keyword}, '%') or u.phone like concat('%', #{keyword}, '%'))
            </if>
            <if test='roleCode != null and roleCode != ""'>and r.code = #{roleCode}</if>
            <if test='status != null and status != ""'>and u.status = #{status}</if>
            order by u.id desc
            </script>
            """)
    IPage<Map<String, Object>> selectUserPage(Page<Map<String, Object>> page,
                                              @Param("keyword") String keyword,
                                              @Param("roleCode") String roleCode,
                                              @Param("status") String status);

    @Select("""
            select u.id, u.username, u.name, u.phone
            from user u
            join role r on r.id = u.role_id
            where r.code = 'REPAIRER' and u.status = 'ACTIVE'
            order by u.name asc
            """)
    List<Map<String, Object>> selectActiveRepairers();
}
