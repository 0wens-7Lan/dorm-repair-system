package com.dormrepair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dormrepair.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
    @Select("""
            <script>
            select *
            from operation_log
            where 1=1
            <if test='action != null and action != ""'>and action = #{action}</if>
            <if test='username != null and username != ""'>and username like concat('%', #{username}, '%')</if>
            order by created_at desc
            </script>
            """)
    IPage<Map<String, Object>> selectLogPage(Page<Map<String, Object>> page,
                                             @Param("action") String action,
                                             @Param("username") String username);
}
