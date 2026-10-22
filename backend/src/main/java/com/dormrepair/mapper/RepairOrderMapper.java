package com.dormrepair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dormrepair.entity.RepairOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface RepairOrderMapper extends BaseMapper<RepairOrder> {
    @Select("""
            <script>
            select ro.id, ro.order_no orderNo, ro.student_id studentId, ro.dorm_room_id dormRoomId,
                   ro.category_id categoryId, ro.description, ro.contact_phone contactPhone,
                   ro.expected_time expectedTime, ro.image_url imageUrl, ro.status,
                   ro.reject_reason rejectReason, ro.audit_user_id auditUserId, ro.audit_time auditTime,
                   ro.repairer_id repairerId, ro.submit_time submitTime, ro.start_time startTime,
                   ro.finish_time finishTime, ro.complete_time completeTime,
                   ro.fault_reason faultReason, ro.repair_method repairMethod,
                   ro.replaced_parts replacedParts, ro.result_summary resultSummary,
                   ro.result_image_url resultImageUrl, ro.reopen_reason reopenReason,
                   student.name studentName, student.phone studentPhone,
                   repairer.name repairerName, repairer.phone repairerPhone,
                   rc.name categoryName, b.id buildingId, b.name buildingName,
                   dr.room_no roomNo, dr.floor roomFloor
            from repair_order ro
            left join user student on student.id = ro.student_id
            left join user repairer on repairer.id = ro.repairer_id
            left join repair_category rc on rc.id = ro.category_id
            left join dorm_room dr on dr.id = ro.dorm_room_id
            left join dorm_building b on b.id = dr.building_id
            where 1=1
            <if test='roleCode == "STUDENT"'>and ro.student_id = #{userId}</if>
            <if test='roleCode == "REPAIRER"'>and ro.repairer_id = #{userId}</if>
            <if test='categoryId != null'>and ro.category_id = #{categoryId}</if>
            <if test='buildingId != null'>and b.id = #{buildingId}</if>
            <if test='status != null and status != ""'>and ro.status = #{status}</if>
            <if test='studentName != null and studentName != ""'>and student.name like concat('%', #{studentName}, '%')</if>
            <if test='repairerId != null'>and ro.repairer_id = #{repairerId}</if>
            <if test='startDate != null and startDate != ""'>and ro.submit_time &gt;= concat(#{startDate}, ' 00:00:00')</if>
            <if test='endDate != null and endDate != ""'>and ro.submit_time &lt;= concat(#{endDate}, ' 23:59:59')</if>
            order by ro.submit_time desc
            </script>
            """)
    IPage<Map<String, Object>> selectOrderPage(Page<Map<String, Object>> page,
                                               @Param("roleCode") String roleCode,
                                               @Param("userId") Long userId,
                                               @Param("categoryId") Long categoryId,
                                               @Param("buildingId") Long buildingId,
                                               @Param("status") String status,
                                               @Param("studentName") String studentName,
                                               @Param("repairerId") Long repairerId,
                                               @Param("startDate") String startDate,
                                               @Param("endDate") String endDate);

    @Select("""
            select ro.id, ro.order_no orderNo, ro.status, ro.submit_time submitTime,
                   ro.audit_time auditTime, ro.start_time startTime, ro.finish_time finishTime,
                   ro.complete_time completeTime, ro.result_summary resultSummary,
                   ro.reject_reason rejectReason, ro.reopen_reason reopenReason,
                   student.name studentName, repairer.name repairerName,
                   rc.name categoryName, b.name buildingName, dr.room_no roomNo
            from repair_order ro
            left join user student on student.id = ro.student_id
            left join user repairer on repairer.id = ro.repairer_id
            left join repair_category rc on rc.id = ro.category_id
            left join dorm_room dr on dr.id = ro.dorm_room_id
            left join dorm_building b on b.id = dr.building_id
            where ro.id = #{id}
            """)
    Map<String, Object> selectOrderDetailHeader(Long id);

    @Select("""
            select count(*) total,
                   sum(case when status in ('PENDING_REVIEW','PENDING_ASSIGN','REPAIRING','PENDING_CONFIRM') then 1 else 0 end) pending,
                   sum(case when status = 'COMPLETED' then 1 else 0 end) completed,
                   avg(case when status = 'COMPLETED' and complete_time is not null then timestampdiff(hour, submit_time, complete_time) end) avgHours
            from repair_order
            """)
    Map<String, Object> selectOverview();

    @Select("""
            select rc.name name, count(*) value
            from repair_order ro
            left join repair_category rc on rc.id = ro.category_id
            group by rc.id, rc.name
            order by value desc
            """)
    List<Map<String, Object>> countByCategory();

    @Select("""
            select date_format(submit_time, '%Y-%m') month, count(*) value
            from repair_order
            group by date_format(submit_time, '%Y-%m')
            order by month asc
            """)
    List<Map<String, Object>> countByMonth();

    @Select("""
            select u.name repairerName, count(*) completed
            from repair_order ro
            left join user u on u.id = ro.repairer_id
            where ro.status = 'COMPLETED'
            group by u.id, u.name
            order by completed desc
            """)
    List<Map<String, Object>> countCompletedByRepairer();
}
