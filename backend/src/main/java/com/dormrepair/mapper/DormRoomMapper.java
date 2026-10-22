package com.dormrepair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dormrepair.entity.DormRoom;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface DormRoomMapper extends BaseMapper<DormRoom> {
    @Select("""
            <script>
            select dr.*, b.name buildingName
            from dorm_room dr
            left join dorm_building b on b.id = dr.building_id
            where 1=1
            <if test='buildingId != null'>and dr.building_id = #{buildingId}</if>
            <if test='roomNo != null and roomNo != ""'>and dr.room_no like concat('%', #{roomNo}, '%')</if>
            order by b.name asc, dr.floor asc, dr.room_no asc
            </script>
            """)
    IPage<Map<String, Object>> selectRoomPage(Page<Map<String, Object>> page,
                                              @Param("buildingId") Long buildingId,
                                              @Param("roomNo") String roomNo);
}
