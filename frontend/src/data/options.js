export const ROLES = {
  STUDENT: 'STUDENT',
  DORM_ADMIN: 'DORM_ADMIN',
  REPAIRER: 'REPAIRER',
  SYS_ADMIN: 'SYS_ADMIN'
};

export const ROLE_LABELS = {
  [ROLES.STUDENT]: '学生',
  [ROLES.DORM_ADMIN]: '宿舍管理员',
  [ROLES.REPAIRER]: '维修人员',
  [ROLES.SYS_ADMIN]: '系统管理员'
};

export const STATUS = {
  PENDING_REVIEW: 'PENDING_REVIEW',
  REJECTED: 'REJECTED',
  PENDING_ASSIGN: 'PENDING_ASSIGN',
  REPAIRING: 'REPAIRING',
  PENDING_CONFIRM: 'PENDING_CONFIRM',
  COMPLETED: 'COMPLETED',
  CANCELED: 'CANCELED'
};

export const STATUS_LABELS = {
  [STATUS.PENDING_REVIEW]: '待审核',
  [STATUS.REJECTED]: '已驳回',
  [STATUS.PENDING_ASSIGN]: '待派单',
  [STATUS.REPAIRING]: '维修中',
  [STATUS.PENDING_CONFIRM]: '待确认',
  [STATUS.COMPLETED]: '已完成',
  [STATUS.CANCELED]: '已取消'
};

export const STATUS_OPTIONS = Object.entries(STATUS_LABELS).map(([value, label]) => ({ value, label }));

export function roleLabel(role) {
  return ROLE_LABELS[role] || role || '未登录';
}

export function statusLabel(status) {
  return STATUS_LABELS[status] || status || '-';
}

export function canStudentCancel(status) {
  return status === STATUS.PENDING_REVIEW || status === STATUS.PENDING_ASSIGN;
}
