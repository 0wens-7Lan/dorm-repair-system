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
