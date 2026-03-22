export function normalizeRole(role?: string) {
  return (role || '').toLowerCase();
}

export function isSuperAdmin(role?: string) {
  return normalizeRole(role) === 'superadmin';
}

export function isDeptLead(role?: string) {
  return normalizeRole(role) === 'dept_lead';
}

export function isExecutor(role?: string) {
  return normalizeRole(role) === 'executor';
}

export function canManagePersonnel(role?: string) {
  return isSuperAdmin(role) || isDeptLead(role);
}

export function canManageAlerts(role?: string) {
  return isSuperAdmin(role) || isDeptLead(role);
}

export function roleLabel(role?: string) {
  if (isSuperAdmin(role)) return '系统管理员';
  if (isDeptLead(role)) return '部门负责人';
  return '执行者';
}
