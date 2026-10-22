CREATE DATABASE IF NOT EXISTS dorm_repair
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE dorm_repair;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS operation_log;
DROP TABLE IF EXISTS repair_feedback;
DROP TABLE IF EXISTS repair_assignment;
DROP TABLE IF EXISTS repair_order;
DROP TABLE IF EXISTS notice;
DROP TABLE IF EXISTS repair_category;
DROP TABLE IF EXISTS user;
DROP TABLE IF EXISTS dorm_room;
DROP TABLE IF EXISTS dorm_building;
DROP TABLE IF EXISTS role_permission;
DROP TABLE IF EXISTS permission;
DROP TABLE IF EXISTS role;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(40) NOT NULL UNIQUE COMMENT '角色编码',
  name VARCHAR(50) NOT NULL COMMENT '角色名称',
  description VARCHAR(255) COMMENT '角色说明',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE dorm_building (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL COMMENT '宿舍楼名称',
  floors INT NOT NULL DEFAULT 6 COMMENT '楼层数',
  address VARCHAR(255) COMMENT '位置说明',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_building_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宿舍楼信息表';

CREATE TABLE dorm_room (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  building_id BIGINT NOT NULL COMMENT '宿舍楼ID',
  floor INT NOT NULL COMMENT '楼层',
  room_no VARCHAR(30) NOT NULL COMMENT '房间号',
  capacity INT NOT NULL DEFAULT 4 COMMENT '入住人数上限',
  occupied INT NOT NULL DEFAULT 0 COMMENT '当前入住人数',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_building_room (building_id, room_no),
  KEY idx_room_building (building_id),
  CONSTRAINT fk_room_building FOREIGN KEY (building_id) REFERENCES dorm_building(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宿舍房间信息表';

CREATE TABLE user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(60) NOT NULL UNIQUE COMMENT '登录账号',
  password_hash VARCHAR(120) NOT NULL COMMENT 'BCrypt密码哈希',
  name VARCHAR(80) NOT NULL COMMENT '姓名',
  student_no VARCHAR(40) COMMENT '学号',
  phone VARCHAR(30) COMMENT '手机号',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  dorm_room_id BIGINT COMMENT '绑定宿舍房间ID',
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_user_role (role_id),
  KEY idx_user_status (status),
  KEY idx_user_room (dorm_room_id),
  CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES role(id),
  CONSTRAINT fk_user_room FOREIGN KEY (dorm_room_id) REFERENCES dorm_room(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE repair_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(60) NOT NULL UNIQUE COMMENT '报修类型名称',
  enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报修分类表';

CREATE TABLE notice (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(120) NOT NULL COMMENT '公告标题',
  content TEXT NOT NULL COMMENT '公告内容',
  status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED' COMMENT 'PUBLISHED/DRAFT',
  publisher_id BIGINT COMMENT '发布人ID',
  publish_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_notice_status_time (status, publish_time),
  CONSTRAINT fk_notice_publisher FOREIGN KEY (publisher_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';

CREATE TABLE repair_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(40) NOT NULL UNIQUE COMMENT '报修编号',
  student_id BIGINT NOT NULL COMMENT '学生编号',
  dorm_room_id BIGINT NOT NULL COMMENT '宿舍编号',
  category_id BIGINT NOT NULL COMMENT '报修类型',
  description TEXT NOT NULL COMMENT '故障描述',
  contact_phone VARCHAR(30) NOT NULL COMMENT '联系电话',
  expected_time DATETIME COMMENT '期望维修时间',
  image_url VARCHAR(500) COMMENT '故障图片地址',
  status VARCHAR(30) NOT NULL COMMENT 'PENDING_REVIEW/REJECTED/PENDING_ASSIGN/REPAIRING/PENDING_CONFIRM/COMPLETED/CANCELED',
  reject_reason VARCHAR(500) COMMENT '驳回原因',
  audit_user_id BIGINT COMMENT '审核人',
  audit_time DATETIME COMMENT '审核时间',
  repairer_id BIGINT COMMENT '维修人员',
  submit_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  start_time DATETIME COMMENT '开始维修时间',
  finish_time DATETIME COMMENT '维修完成提交时间',
  complete_time DATETIME COMMENT '学生确认完成时间',
  fault_reason VARCHAR(500) COMMENT '故障原因',
  repair_method VARCHAR(500) COMMENT '维修方式',
  replaced_parts VARCHAR(500) COMMENT '更换配件',
  result_summary TEXT COMMENT '处理结果',
  result_image_url VARCHAR(500) COMMENT '维修后凭证图片地址',
  reopen_reason VARCHAR(500) COMMENT '未解决或重新处理原因',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_order_status (status),
  KEY idx_order_category (category_id),
  KEY idx_order_room (dorm_room_id),
  KEY idx_order_submit_time (submit_time),
  KEY idx_order_repairer (repairer_id),
  KEY idx_order_student (student_id),
  CONSTRAINT fk_order_student FOREIGN KEY (student_id) REFERENCES user(id),
  CONSTRAINT fk_order_room FOREIGN KEY (dorm_room_id) REFERENCES dorm_room(id),
  CONSTRAINT fk_order_category FOREIGN KEY (category_id) REFERENCES repair_category(id),
  CONSTRAINT fk_order_auditor FOREIGN KEY (audit_user_id) REFERENCES user(id),
  CONSTRAINT fk_order_repairer FOREIGN KEY (repairer_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报修单表';

CREATE TABLE repair_assignment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  repair_order_id BIGINT NOT NULL COMMENT '报修单ID',
  dispatcher_id BIGINT NOT NULL COMMENT '派单人ID',
  repairer_id BIGINT NOT NULL COMMENT '接单维修人员ID',
  remark VARCHAR(500) COMMENT '派单备注',
  assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '派单时间',
  KEY idx_assignment_order (repair_order_id),
  KEY idx_assignment_repairer (repairer_id),
  CONSTRAINT fk_assignment_order FOREIGN KEY (repair_order_id) REFERENCES repair_order(id),
  CONSTRAINT fk_assignment_dispatcher FOREIGN KEY (dispatcher_id) REFERENCES user(id),
  CONSTRAINT fk_assignment_repairer FOREIGN KEY (repairer_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='派单记录表';

CREATE TABLE repair_feedback (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  repair_order_id BIGINT NOT NULL COMMENT '报修单ID',
  student_id BIGINT NOT NULL COMMENT '评价学生ID',
  speed_score INT COMMENT '维修速度评分1-5',
  attitude_score INT COMMENT '服务态度评分1-5',
  quality_score INT COMMENT '维修质量评分1-5',
  overall_score INT COMMENT '总体评分1-5',
  comment VARCHAR(1000) COMMENT '文字评价或未解决反馈',
  unresolved TINYINT NOT NULL DEFAULT 0 COMMENT '是否未解决',
  admin_remark VARCHAR(500) COMMENT '管理员处理备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_feedback_order (repair_order_id),
  KEY idx_feedback_student (student_id),
  CONSTRAINT fk_feedback_order FOREIGN KEY (repair_order_id) REFERENCES repair_order(id),
  CONSTRAINT fk_feedback_student FOREIGN KEY (student_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生评价反馈表';

CREATE TABLE operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT COMMENT '操作用户ID',
  username VARCHAR(60) COMMENT '操作账号',
  role_code VARCHAR(40) COMMENT '操作角色',
  action VARCHAR(60) NOT NULL COMMENT '操作类型',
  biz_type VARCHAR(60) COMMENT '业务类型',
  biz_id BIGINT COMMENT '业务ID',
  detail VARCHAR(1000) COMMENT '操作内容',
  ip VARCHAR(80) COMMENT '客户端IP',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_log_user (user_id),
  KEY idx_log_action (action),
  KEY idx_log_biz (biz_type, biz_id),
  KEY idx_log_time (created_at),
  CONSTRAINT fk_log_user FOREIGN KEY (user_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

INSERT INTO role (id, code, name, description) VALUES
(1, 'STUDENT', '学生', '提交报修、查询进度、取消、确认和评价'),
(2, 'DORM_ADMIN', '宿舍管理员', '审核报修、派单、管理宿舍、公告和统计'),
(3, 'REPAIRER', '维修人员', '查看分配任务、更新维修进度、填写维修结果'),
(4, 'SYS_ADMIN', '系统管理员', '管理用户、角色、公告、日志和基础数据');

INSERT INTO dorm_building (id, name, floors, address) VALUES
(1, '东区1号楼', 6, '东区学生公寓'),
(2, '西区2号楼', 8, '西区学生公寓');

INSERT INTO dorm_room (id, building_id, floor, room_no, capacity, occupied) VALUES
(1, 1, 3, '301', 4, 3),
(2, 1, 3, '302', 4, 2),
(3, 2, 5, '508', 4, 4);

INSERT INTO repair_category (id, name, enabled) VALUES
(1, '水电维修', 1),
(2, '门窗维修', 1),
(3, '家具维修', 1),
(4, '网络故障', 1),
(5, '空调维修', 1),
(6, '其他问题', 1);

-- 所有初始化账号密码均为 password，下面为 BCrypt 哈希，系统不会保存明文密码。
INSERT INTO user (id, username, password_hash, name, student_no, phone, role_id, dorm_room_id, status) VALUES
(1, 'student', '$2a$10$vBwgsrk1EE/Z7Miv1AQfU.Sjkz2o6/zn32caCgcR.iGecOB0hjBvS', '张同学', '20260001', '13800000001', 1, 1, 'ACTIVE'),
(2, 'admin', '$2a$10$vBwgsrk1EE/Z7Miv1AQfU.Sjkz2o6/zn32caCgcR.iGecOB0hjBvS', '宿管王老师', NULL, '13800000002', 2, NULL, 'ACTIVE'),
(3, 'repairer', '$2a$10$vBwgsrk1EE/Z7Miv1AQfU.Sjkz2o6/zn32caCgcR.iGecOB0hjBvS', '李维修', NULL, '13800000003', 3, NULL, 'ACTIVE'),
(4, 'sysadmin', '$2a$10$vBwgsrk1EE/Z7Miv1AQfU.Sjkz2o6/zn32caCgcR.iGecOB0hjBvS', '系统管理员', NULL, '13800000004', 4, NULL, 'ACTIVE');

INSERT INTO notice (id, title, content, status, publisher_id, publish_time) VALUES
(1, '东区宿舍集中检修通知', '本周三下午将对东区1号楼公共区域水电设施进行集中检修，请同学们提前做好安排。', 'PUBLISHED', 2, NOW()),
(2, '校园网络维护通知', '今晚23:00至次日01:00校园网络将短时维护，期间可能出现访问不稳定。', 'PUBLISHED', 2, NOW());

INSERT INTO repair_order (
  id, order_no, student_id, dorm_room_id, category_id, description, contact_phone, expected_time,
  image_url, status, audit_user_id, audit_time, repairer_id, submit_time, start_time,
  finish_time, complete_time, fault_reason, repair_method, replaced_parts, result_summary, result_image_url
) VALUES
(1, 'BX202606080001', 1, 1, 1, '宿舍洗手台下方水管滴水，地面有积水。', '13800000001', '2026-06-09 15:00:00',
 NULL, 'COMPLETED', 2, '2026-06-08 10:30:00', 3, '2026-06-08 10:00:00', '2026-06-08 11:00:00',
 '2026-06-08 12:00:00', '2026-06-08 13:00:00', '水管接口老化', '更换接口并重新密封', '密封圈', '已更换密封圈，测试无渗漏。', NULL),
(2, 'BX202606080002', 1, 1, 4, '宿舍网络端口无法联网。', '13800000001', '2026-06-10 10:00:00',
 NULL, 'PENDING_ASSIGN', 2, '2026-06-08 11:30:00', NULL, '2026-06-08 11:20:00', NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(3, 'BX202606080003', 1, 1, 5, '空调制冷效果很差。', '13800000001', '2026-06-11 16:00:00',
 NULL, 'PENDING_REVIEW', NULL, NULL, NULL, '2026-06-08 12:00:00', NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL);

INSERT INTO repair_assignment (repair_order_id, dispatcher_id, repairer_id, remark, assigned_at) VALUES
(1, 2, 3, '请优先处理漏水问题', '2026-06-08 10:40:00');

INSERT INTO repair_feedback (repair_order_id, student_id, speed_score, attitude_score, quality_score, overall_score, comment, unresolved, created_at) VALUES
(1, 1, 5, 5, 5, 5, '维修及时，处理后没有继续漏水。', 0, '2026-06-08 13:05:00');

INSERT INTO operation_log (user_id, username, role_code, action, biz_type, biz_id, detail, ip, created_at) VALUES
(1, 'student', 'STUDENT', 'SUBMIT_REPAIR', 'REPAIR_ORDER', 1, '提交报修单：BX202606080001', '127.0.0.1', '2026-06-08 10:00:00'),
(2, 'admin', 'DORM_ADMIN', 'AUDIT_APPROVE', 'REPAIR_ORDER', 1, '审核通过', '127.0.0.1', '2026-06-08 10:30:00'),
(2, 'admin', 'DORM_ADMIN', 'ASSIGN_REPAIR', 'REPAIR_ORDER', 1, '派单给维修人员：李维修', '127.0.0.1', '2026-06-08 10:40:00'),
(3, 'repairer', 'REPAIRER', 'START_REPAIR', 'REPAIR_ORDER', 1, '维修人员开始处理', '127.0.0.1', '2026-06-08 11:00:00'),
(3, 'repairer', 'REPAIRER', 'FINISH_REPAIR', 'REPAIR_ORDER', 1, '维修完成，等待学生确认', '127.0.0.1', '2026-06-08 12:00:00'),
(1, 'student', 'STUDENT', 'CONFIRM_AND_EVALUATE', 'REPAIR_ORDER', 1, '学生确认完成并评价', '127.0.0.1', '2026-06-08 13:05:00');

-- AI-assisted: permission grants are loaded by Spring Security on each request.
CREATE TABLE IF NOT EXISTS permission (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, code VARCHAR(80) NOT NULL UNIQUE, name VARCHAR(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS role_permission (
 role_id BIGINT NOT NULL, permission_id BIGINT NOT NULL, PRIMARY KEY(role_id,permission_id),
 FOREIGN KEY(role_id) REFERENCES role(id), FOREIGN KEY(permission_id) REFERENCES permission(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT IGNORE INTO permission(code,name) VALUES ('user:manage','用户管理'),('log:read','日志查看'),('statistics:read','统计查看');
INSERT IGNORE INTO role_permission(role_id,permission_id)
 SELECT r.id,p.id FROM role r CROSS JOIN permission p WHERE r.code='SYS_ADMIN' OR (r.code='DORM_ADMIN' AND p.code='statistics:read');
