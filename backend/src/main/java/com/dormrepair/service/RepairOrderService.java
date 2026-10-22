package com.dormrepair.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dormrepair.dto.Requests;
import com.dormrepair.entity.OperationLog;
import com.dormrepair.entity.RepairAssignment;
import com.dormrepair.entity.RepairFeedback;
import com.dormrepair.entity.RepairOrder;
import com.dormrepair.entity.Role;
import com.dormrepair.entity.User;
import com.dormrepair.mapper.OperationLogMapper;
import com.dormrepair.mapper.RepairAssignmentMapper;
import com.dormrepair.mapper.RepairFeedbackMapper;
import com.dormrepair.mapper.RepairOrderMapper;
import com.dormrepair.mapper.RoleMapper;
import com.dormrepair.mapper.UserMapper;
import com.dormrepair.support.AuthContext;
import com.dormrepair.support.BusinessException;
import com.dormrepair.support.CurrentUser;
import com.dormrepair.support.Roles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class RepairOrderService {
    public static final String PENDING_REVIEW = "PENDING_REVIEW";
    public static final String REJECTED = "REJECTED";
    public static final String PENDING_ASSIGN = "PENDING_ASSIGN";
    public static final String REPAIRING = "REPAIRING";
    public static final String PENDING_CONFIRM = "PENDING_CONFIRM";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELED = "CANCELED";

    private final RepairOrderMapper repairOrderMapper;
    private final RepairAssignmentMapper repairAssignmentMapper;
    private final RepairFeedbackMapper repairFeedbackMapper;
    private final OperationLogMapper operationLogMapper;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final OperationLogService operationLogService;

    public RepairOrderService(RepairOrderMapper repairOrderMapper,
                              RepairAssignmentMapper repairAssignmentMapper,
                              RepairFeedbackMapper repairFeedbackMapper,
                              OperationLogMapper operationLogMapper,
                              UserMapper userMapper,
                              RoleMapper roleMapper,
                              OperationLogService operationLogService) {
        this.repairOrderMapper = repairOrderMapper;
        this.repairAssignmentMapper = repairAssignmentMapper;
        this.repairFeedbackMapper = repairFeedbackMapper;
        this.operationLogMapper = operationLogMapper;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.operationLogService = operationLogService;
    }

    public IPage<Map<String, Object>> page(long current,
                                           long size,
                                           Long categoryId,
                                           Long buildingId,
                                           String status,
                                           String studentName,
                                           Long repairerId,
                                           String startDate,
                                           String endDate) {
        CurrentUser user = AuthContext.user();
        Page<Map<String, Object>> page = new Page<>(current, size);
        return repairOrderMapper.selectOrderPage(page, user.getRoleCode(), user.getId(), categoryId, buildingId,
                status, studentName, repairerId, startDate, endDate);
    }

    @Transactional
    public RepairOrder create(Requests.CreateRepairRequest request) {
        CurrentUser user = AuthContext.user();
        RepairOrder order = new RepairOrder();
        order.setOrderNo(generateOrderNo());
        order.setStudentId(user.getId());
        order.setDormRoomId(request.dormRoomId());
        order.setCategoryId(request.categoryId());
        order.setDescription(request.description());
        order.setContactPhone(request.contactPhone());
        order.setExpectedTime(request.expectedTime());
        order.setImageUrl(request.imageUrl());
        order.setStatus(PENDING_REVIEW);
        LocalDateTime now = LocalDateTime.now();
        order.setSubmitTime(now);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        repairOrderMapper.insert(order);
        operationLogService.log("SUBMIT_REPAIR", "REPAIR_ORDER", order.getId(), "提交报修单：" + order.getOrderNo());
        return order;
    }

    public Map<String, Object> detail(Long id) {
        RepairOrder order = requireOrder(id);
        ensureVisible(order);
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> header = repairOrderMapper.selectOrderDetailHeader(id);
        result.put("order", order);
        result.put("header", header);
        result.put("assignment", repairAssignmentMapper.selectOne(new QueryWrapper<RepairAssignment>()
                .eq("repair_order_id", id).orderByDesc("assigned_at").last("limit 1")));
        result.put("feedback", repairFeedbackMapper.selectOne(new QueryWrapper<RepairFeedback>()
                .eq("repair_order_id", id).orderByDesc("created_at").last("limit 1")));
        result.put("timeline", operationLogMapper.selectList(new QueryWrapper<OperationLog>()
                .eq("biz_type", "REPAIR_ORDER").eq("biz_id", id).orderByAsc("created_at")));
        return result;
    }

    @Transactional
    public RepairOrder cancel(Long id) {
        RepairOrder order = requireOrder(id);
        CurrentUser user = AuthContext.user();
        if (!order.getStudentId().equals(user.getId())) {
            throw new BusinessException(403, "只能取消自己的报修单");
        }
        if (!PENDING_REVIEW.equals(order.getStatus()) && !PENDING_ASSIGN.equals(order.getStatus())) {
            throw new BusinessException("工单已进入维修流程，不能直接取消");
        }
        order.setStatus(CANCELED);
        order.setUpdatedAt(LocalDateTime.now());
        repairOrderMapper.updateById(order);
        operationLogService.log("CANCEL_REPAIR", "REPAIR_ORDER", order.getId(), "学生取消报修");
        return order;
    }

    @Transactional
    public RepairOrder audit(Long id, Requests.AuditRequest request) {
        RepairOrder order = requireOrder(id);
        if (!PENDING_REVIEW.equals(order.getStatus())) {
            throw new BusinessException("只有待审核工单可以审核");
        }
        LocalDateTime now = LocalDateTime.now();
        order.setAuditUserId(AuthContext.user().getId());
        order.setAuditTime(now);
        if (Boolean.TRUE.equals(request.approved())) {
            order.setStatus(PENDING_ASSIGN);
            order.setRejectReason(null);
            operationLogService.log("AUDIT_APPROVE", "REPAIR_ORDER", order.getId(), "审核通过");
        } else {
            if (request.rejectReason() == null || request.rejectReason().isBlank()) {
                throw new BusinessException("驳回报修单必须填写驳回原因");
            }
            order.setStatus(REJECTED);
            order.setRejectReason(request.rejectReason());
            operationLogService.log("AUDIT_REJECT", "REPAIR_ORDER", order.getId(), "审核驳回：" + request.rejectReason());
        }
        order.setUpdatedAt(now);
        repairOrderMapper.updateById(order);
        return order;
    }

    @Transactional
    public RepairOrder assign(Long id, Requests.AssignRequest request) {
        RepairOrder order = requireOrder(id);
        if (!PENDING_ASSIGN.equals(order.getStatus())) {
            throw new BusinessException("只有待派单工单可以派单");
        }
        User repairer = userMapper.selectById(request.repairerId());
        if (repairer == null || !"ACTIVE".equals(repairer.getStatus())) {
            throw new BusinessException("维修人员不存在或已禁用");
        }
        Role role = roleMapper.selectById(repairer.getRoleId());
        if (role == null || !Roles.REPAIRER.equals(role.getCode())) {
            throw new BusinessException("派单对象必须是维修人员");
        }
        LocalDateTime now = LocalDateTime.now();
        order.setRepairerId(request.repairerId());
        order.setUpdatedAt(now);
        repairOrderMapper.updateById(order);

        RepairAssignment assignment = new RepairAssignment();
        assignment.setRepairOrderId(order.getId());
        assignment.setDispatcherId(AuthContext.user().getId());
        assignment.setRepairerId(request.repairerId());
        assignment.setRemark(request.remark());
        assignment.setAssignedAt(now);
        repairAssignmentMapper.insert(assignment);
        operationLogService.log("ASSIGN_REPAIR", "REPAIR_ORDER", order.getId(), "派单给维修人员：" + repairer.getName());
        return order;
    }

    @Transactional
    public RepairOrder start(Long id) {
        RepairOrder order = requireOrder(id);
        CurrentUser user = AuthContext.user();
        ensureRepairerOwns(order, user);
        if (!PENDING_ASSIGN.equals(order.getStatus())) {
            throw new BusinessException("只有待处理任务可以开始维修");
        }
        LocalDateTime now = LocalDateTime.now();
        order.setStatus(REPAIRING);
        order.setStartTime(now);
        order.setUpdatedAt(now);
        repairOrderMapper.updateById(order);
        operationLogService.log("START_REPAIR", "REPAIR_ORDER", order.getId(), "维修人员开始处理");
        return order;
    }

    @Transactional
    public RepairOrder finish(Long id, Requests.FinishRepairRequest request) {
        RepairOrder order = requireOrder(id);
        CurrentUser user = AuthContext.user();
        ensureRepairerOwns(order, user);
        if (!REPAIRING.equals(order.getStatus())) {
            throw new BusinessException("只有维修中的工单可以提交维修完成");
        }
        LocalDateTime now = LocalDateTime.now();
        order.setStatus(PENDING_CONFIRM);
        order.setFinishTime(now);
        order.setFaultReason(request.faultReason());
        order.setRepairMethod(request.repairMethod());
        order.setReplacedParts(request.replacedParts());
        order.setResultSummary(request.resultSummary());
        order.setResultImageUrl(request.resultImageUrl());
        order.setUpdatedAt(now);
        repairOrderMapper.updateById(order);
        operationLogService.log("FINISH_REPAIR", "REPAIR_ORDER", order.getId(), "维修完成，等待学生确认");
        return order;
    }

    @Transactional
    public RepairOrder feedback(Long id, Requests.FeedbackRequest request) {
        RepairOrder order = requireOrder(id);
        CurrentUser user = AuthContext.user();
        if (!order.getStudentId().equals(user.getId())) {
            throw new BusinessException(403, "只能评价自己的报修单");
        }
        if (!PENDING_CONFIRM.equals(order.getStatus())) {
            throw new BusinessException("只有待确认工单可以确认和评价");
        }
        boolean unresolved = Boolean.TRUE.equals(request.unresolved());
        if (!unresolved && (request.speedScore() == null || request.attitudeScore() == null
                || request.qualityScore() == null || request.overallScore() == null)) {
            throw new BusinessException("确认完成时必须填写维修速度、服务态度、维修质量和总体评分");
        }
        if (unresolved && (request.comment() == null || request.comment().isBlank())) {
            throw new BusinessException("反馈未解决问题时必须填写文字说明");
        }

        RepairFeedback feedback = new RepairFeedback();
        feedback.setRepairOrderId(order.getId());
        feedback.setStudentId(user.getId());
        feedback.setSpeedScore(request.speedScore());
        feedback.setAttitudeScore(request.attitudeScore());
        feedback.setQualityScore(request.qualityScore());
        feedback.setOverallScore(request.overallScore());
        feedback.setComment(request.comment());
        feedback.setUnresolved(unresolved ? 1 : 0);
        feedback.setCreatedAt(LocalDateTime.now());
        repairFeedbackMapper.insert(feedback);

        if (unresolved) {
            order.setStatus(PENDING_ASSIGN);
            order.setReopenReason(request.comment());
            operationLogService.log("UNRESOLVED_FEEDBACK", "REPAIR_ORDER", order.getId(), "学生反馈未解决：" + request.comment());
        } else {
            order.setStatus(COMPLETED);
            order.setCompleteTime(LocalDateTime.now());
            operationLogService.log("CONFIRM_AND_EVALUATE", "REPAIR_ORDER", order.getId(), "学生确认完成并评价");
        }
        order.setUpdatedAt(LocalDateTime.now());
        repairOrderMapper.updateById(order);
        return order;
    }

    @Transactional
    public RepairOrder reopen(Long id, Requests.ReopenRequest request) {
        RepairOrder order = requireOrder(id);
        if (!PENDING_ASSIGN.equals(order.getStatus()) && !PENDING_CONFIRM.equals(order.getStatus())) {
            throw new BusinessException("当前状态不需要重新处理");
        }
        order.setStatus(PENDING_ASSIGN);
        order.setReopenReason(request.reason());
        order.setRepairerId(request.repairerId());
        order.setUpdatedAt(LocalDateTime.now());
        repairOrderMapper.updateById(order);
        operationLogService.log("REOPEN_REPAIR", "REPAIR_ORDER", order.getId(), "管理员重新处理：" + request.reason());
        return order;
    }

    public Map<String, Object> statistics() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("overview", repairOrderMapper.selectOverview());
        result.put("category", repairOrderMapper.countByCategory());
        result.put("monthly", repairOrderMapper.countByMonth());
        result.put("repairer", repairOrderMapper.countCompletedByRepairer());
        return result;
    }

    private RepairOrder requireOrder(Long id) {
        RepairOrder order = repairOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("报修单不存在");
        }
        return order;
    }

    private void ensureVisible(RepairOrder order) {
        CurrentUser user = AuthContext.user();
        if (user.hasAnyRole(Roles.DORM_ADMIN, Roles.SYS_ADMIN)) {
            return;
        }
        if (Roles.STUDENT.equals(user.getRoleCode()) && order.getStudentId().equals(user.getId())) {
            return;
        }
        if (Roles.REPAIRER.equals(user.getRoleCode()) && order.getRepairerId() != null
                && order.getRepairerId().equals(user.getId())) {
            return;
        }
        throw new BusinessException(403, "无权查看该报修单");
    }

    private void ensureRepairerOwns(RepairOrder order, CurrentUser user) {
        if (order.getRepairerId() == null || !order.getRepairerId().equals(user.getId())) {
            throw new BusinessException(403, "维修人员只能处理分配给自己的任务");
        }
    }

    private String generateOrderNo() {
        return "BX" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + ThreadLocalRandom.current().nextInt(100000, 999999);
    }
}
