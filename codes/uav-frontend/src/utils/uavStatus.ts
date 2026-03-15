export type UavStatusMeta = {
  label: string;
  color: string;
};

const UAV_STATUS_META: Record<string, UavStatusMeta> = {
  OFFLINE: { label: '离线', color: 'default' },
  ONLINE: { label: '在线', color: 'green' },
  WARNING: { label: '链路预警', color: 'orange' },
  CRITICAL: { label: '严重异常', color: 'red' },
  PENDING_CONNECT: { label: '待接入', color: 'default' },
  IDLE: { label: '空闲待命', color: 'blue' },
  READY: { label: '待命', color: 'blue' },
  EXECUTING: { label: '任务执行中', color: 'processing' },
  RUNNING: { label: '任务执行中', color: 'processing' },
  RETURNING: { label: '返航中', color: 'gold' },
  TAKEOFF: { label: '起飞中', color: 'geekblue' },
  LANDING: { label: '降落中', color: 'cyan' },
  CHARGING: { label: '充电中', color: 'cyan' },
  MAINTENANCE: { label: '维护中', color: 'purple' },
  CONNECTED: { label: '已连接', color: 'green' },
  DISCONNECTED: { label: '已断开', color: 'default' },
};

export function getUavStatusMeta(status?: string): UavStatusMeta {
  if (!status) {
    return { label: '--', color: 'default' };
  }
  const normalized = status.toUpperCase();
  return UAV_STATUS_META[normalized] || { label: status, color: 'default' };
}
