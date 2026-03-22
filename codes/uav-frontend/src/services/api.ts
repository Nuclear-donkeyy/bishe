import { http, unwrapPage } from './http';

// helper to unwrap biz errors (后端返回 success=false 时抛出)
function ensureSuccess<T>(data: any, defaultMessage = '请求失败'): T {
  if (data && data.success === false) {
    throw new Error(data.message || defaultMessage);
  }
  return data as T;
}

// Auth
export interface LoginPayload {
  username: string;
  password: string;
}
export interface UserInfo {
  id: number;
  username: string;
  name: string;
  role: 'SUPERADMIN' | 'DEPT_LEAD' | 'EXECUTOR' | string;
  departmentId?: number;
  departmentName?: string;
}
export interface LoginResponse {
  token: string;
  user: UserInfo;
}

export const authApi = {
  login: (data: LoginPayload) =>
    http.post<LoginResponse>('/auth/login', data).then(r => ensureSuccess<LoginResponse>(r.data, '登录失败')),
  profile: () => http.get<UserInfo>('/auth/profile').then(r => ensureSuccess<UserInfo>(r.data, '获取用户信息失败')),
  logout: () => http.post('/auth/logout').then(() => void 0)
};

// Fleet
export interface FleetSummary {
  online: number;
  warning: number;
  alerts: number;
  avgRtt: number;
}

export type UavStatus = 'ONLINE' | 'OFFLINE' | 'WARNING' | 'CRITICAL' | 'PENDING_CONNECT';

export interface UavDevice {
  id: number;
  uavCode: string;
  model: string;
  pilotName: string;
  departmentName?: string;
  ownerUsername?: string;
  sensors?: string[];
}

export const fleetApi = {
  summary: () => http.get<FleetSummary>('/fleet/summary').then(r => r.data),
  list: (params?: { status?: UavStatus[]; page?: number; pageSize?: number }) =>
    http
      .get('/fleet', { params })
      .then(r => unwrapPage<UavDevice>(r.data as any))
      .catch(() => ({ items: [], total: 0 })),
  register: (payload: any) => http.post<UavDevice>('/fleet', payload).then(r => ensureSuccess<UavDevice>(r.data, '接入失败')),
  available: () => http.get<UavDevice[]>('/fleet/available').then(r => r.data)
};

// Missions
export interface MissionTypeDefinition {
  id: number;
  typeCode: string;
  displayName: string;
  description?: string;
  recommendedSensors?: string;
  metrics?: string;
}

export interface MissionDto {
  id: number;
  missionCode: string;
  name: string;
  missionType: string;
  pilotName: string;
  status: string;
  priority: string;
  progress: number;
  colorHex?: string;
  route: number[][];
  milestones?: string[];
  metrics?: string[];
  assignedUavs?: string[];
   ruleId?: number;
}

export interface MissionStatusPayload {
  missionCode: string;
  status: string;
}

export const missionApi = {
  types: () => http.get<MissionTypeDefinition[]>('/mission-types').then(r => r.data),
  list: (params?: { status?: string[] }) => http.get<MissionDto[]>('/missions', { params }).then(r => r.data),
  create: (payload: {
    name: string;
    missionType: string;
    pilotUsername: string;
    priority: string;
    milestones?: string[];
    route: number[][];
    assignedUavs?: string[];
    ruleId?: number;
  }) => http.post<MissionDto>('/missions', payload).then(r => ensureSuccess<MissionDto>(r.data, '创建任务失败')),
  updateProgress: (code: string, payload: { progress: number }) =>
    http.patch<MissionDto>(`/missions/${code}`, payload).then(r => r.data),
  interrupt: (code: string) => http.post<void>(`/missions/${code}/interrupt`).then(() => void 0)
};

// Monitoring
export interface MonitoringTaskDto {
  id: number;
  taskCode: string;
  missionName: string;
  missionType: string;
  ownerName: string;
  status: string;
  location: string;
  devices: number;
  rules: { id: number; name: string; metric: string; threshold: string; level: string }[];
}

export const monitoringApi = {
  list: (params?: { status?: string }) =>
    http.get<MonitoringTaskDto[]>('/monitoring/tasks', { params }).then(r => r.data),
  addRule: (taskCode: string, rule: { name: string; metric: string; threshold: string; level: string }) =>
    http.post(`/monitoring/tasks/${taskCode}/rules`, rule).then(r => ensureSuccess(r.data, '新增规则失败')),
  deleteRule: (taskCode: string, ruleId: number) =>
    http.delete(`/monitoring/tasks/${taskCode}/rules/${ruleId}`).then(() => void 0)
};

// Analytics
export interface AnalyticsDefinitionDto {
  id: number;
  missionType: string;
  title: string;
  description?: string;
  seriesConfig: string;
}

export interface TaskExecutionDto {
  id: number;
  executionCode: string;
  missionName: string;
  missionType: string;
  departmentId?: number;
  departmentName?: string;
  location?: string;
  ownerName?: string;
  completedAt: string;
  metrics: string;
}

export interface MissionComparisonDto {
  missionCode: string;
  missionName: string;
  missionType: string;
  status?: string;
  pilotName?: string;
  uavCode?: string;
  startTime?: string;
  endTime?: string;
  durationMinutes?: number;
  avgSpeedKmh?: number;
  batteryConsumption?: number;
  alertCount?: number;
  successRate?: number;
  dataAvg: Record<string, any>;
  dataMax: Record<string, any>;
  dataMin: Record<string, any>;
}

export interface AnalyticsMetricOptionDto {
  metricCode: string;
  displayName: string;
  unit?: string;
}

export interface AnalyticsSeriesPointDto {
  timestamp: string;
  value: number;
}

export interface AnalyticsSeriesDto {
  metricCode: string;
  displayName: string;
  unit?: string;
  points: AnalyticsSeriesPointDto[];
}

export interface AnalyticsTimeSeriesDto {
  missionCode: string;
  missionName: string;
  missionType: string;
  uavCode?: string;
  startTime?: string;
  endTime?: string;
  metricOptions: AnalyticsMetricOptionDto[];
  series: AnalyticsSeriesDto[];
}

export interface AnalyticsReplayPointDto {
  seq?: number;
  lat?: number;
  lng?: number;
  altitude?: number;
  source: 'PLANNED' | 'ACTUAL' | string;
  timestamp?: string;
}

export interface AnalyticsReplayEventDto {
  category: string;
  eventType: string;
  title: string;
  description?: string;
  occurredAt?: string;
}

export interface AnalyticsReplaySampleDto {
  reportedAt: string;
  lat?: number;
  lng?: number;
  altitude?: number;
  batteryPercent?: number;
  velocityMs?: number;
  metrics: Record<string, any>;
}

export interface AnalyticsReplayDto {
  missionCode: string;
  missionName: string;
  missionType: string;
  status?: string;
  uavCode?: string;
  startTime?: string;
  endTime?: string;
  durationMinutes?: number;
  distanceKm?: number;
  sampleCount?: number;
  metricOptions: AnalyticsMetricOptionDto[];
  plannedRoute: AnalyticsReplayPointDto[];
  actualTrack: AnalyticsReplayPointDto[];
  timeline: AnalyticsReplayEventDto[];
  samples: AnalyticsReplaySampleDto[];
}

export interface MissionDataRecord {
  id: number;
  missionId: number;
  missionCode: string;
  missionType: string;
  pilotName?: string;
  uavCode?: string;
  operatorName?: string;
  startTime?: string;
  endTime?: string;
  dataMax: Record<string, any>;
  dataMin: Record<string, any>;
  dataAvg: Record<string, any>;
}

export const analyticsApi = {
  definitions: (missionType?: string) =>
    http.get<AnalyticsDefinitionDto[]>('/analytics/definitions', { params: { missionType } }).then(r => r.data),
  executions: (missionType: string, from?: string, to?: string) =>
    http
      .get<TaskExecutionDto[]>('/analytics/task-executions', { params: { missionType, from, to } })
      .then(r => r.data),
  compare: (missionCodes: string[]) =>
    http.post<MissionComparisonDto[]>('/analytics/compare', { missionCodes }).then(r => r.data),
  replay: (missionCode: string) =>
    http.get<AnalyticsReplayDto>('/analytics/replay', { params: { missionCode } }).then(r => r.data),
  timeseries: (missionCode: string, metrics?: string[]) =>
    http.get<AnalyticsTimeSeriesDto>('/analytics/timeseries', { params: { missionCode, metrics } }).then(r => r.data),
  data: (params: {
    missionType: string;
    uavCode?: string;
    operatorName?: string;
    missionCode?: string;
    from?: string;
    to?: string;
  }) => http.get<MissionDataRecord[]>('/analytics/data', { params }).then(r => r.data)
};

// Users
export interface UserRow {
  id: number;
  username: string;
  name: string;
  role: string;
  departmentId?: number;
  departmentName?: string;
}

export interface DepartmentRow {
  id: number;
  deptCode: string;
  deptName: string;
  description?: string;
  status: 'ACTIVE' | 'DISABLED' | string;
  memberCount: number;
  leadCount: number;
  executorCount: number;
}
export const userApi = {
  list: (departmentId?: number) =>
    http.get<UserRow[]>('/users', { params: { departmentId } }).then(r => ensureSuccess<UserRow[]>(r.data, '获取用户失败')),
  create: (payload: { username: string; password: string; name?: string; role: string; departmentId?: number }) =>
    http.post<UserRow>('/users', payload).then(r => ensureSuccess<UserRow>(r.data, '新增用户失败')),
  delete: (id: number) =>
    http.delete(`/users/${id}`).then(r => {
      if (r.data && (r.data as any).success === false) {
        throw new Error((r.data as any).message || '删除用户失败');
      }
      return;
    }),
  resetPassword: (id: number) => http.post(`/users/${id}/reset-password`).then(() => void 0)
};

export const departmentApi = {
  list: () => http.get<DepartmentRow[]>('/departments').then(r => ensureSuccess<DepartmentRow[]>(r.data, '获取部门失败')),
  create: (payload: { deptCode: string; deptName: string; description?: string; status?: string }) =>
    http.post<DepartmentRow>('/departments', payload).then(r => ensureSuccess<DepartmentRow>(r.data, '新增部门失败')),
  update: (id: number, payload: { deptCode: string; deptName: string; description?: string; status?: string }) =>
    http.put<DepartmentRow>(`/departments/${id}`, payload).then(r => ensureSuccess<DepartmentRow>(r.data, '更新部门失败')),
  delete: (id: number) =>
    http.delete(`/departments/${id}`).then(r => {
      if (r.data && (r.data as any).success === false) {
        throw new Error((r.data as any).message || '删除部门失败');
      }
      return;
    })
};

// Config Center (任务类型/指标/传感器)
export interface MissionTypeItem {
  id: number;
  typeCode: string;
  displayName: string;
  description?: string;
  metricIds?: number[];
}

export interface MetricItem {
  id: number;
  metricCode: string;
  name: string;
  unit?: string;
  description?: string;
  sensorTypeIds?: number[];
}

export interface SensorTypeItem {
  id: number;
  sensorCode: string;
  name: string;
  description?: string;
}

export const configApi = {
  missionTypes: {
    list: () => http.get<MissionTypeItem[]>('/catalog/mission-types').then(r => ensureSuccess(r.data, '获取任务类型失败')),
    create: (payload: Partial<MissionTypeItem>) =>
      http.post<MissionTypeItem>('/catalog/mission-types', payload).then(r => ensureSuccess(r.data, '新增任务类型失败')),
    update: (id: number, payload: Partial<MissionTypeItem>) =>
      http.put<MissionTypeItem>(`/catalog/mission-types/${id}`, payload).then(r => ensureSuccess(r.data, '更新任务类型失败')),
    delete: (id: number) =>
      http.delete(`/catalog/mission-types/${id}`).then(r => {
        if (r.data && (r.data as any).success === false) {
          throw new Error((r.data as any).message || '删除任务类型失败');
        }
        return;
      })
  },
  metrics: {
    list: () => http.get<MetricItem[]>('/catalog/metrics').then(r => ensureSuccess(r.data, '获取指标失败')),
    create: (payload: Partial<MetricItem>) =>
      http.post<MetricItem>('/catalog/metrics', payload).then(r => ensureSuccess(r.data, '新增指标失败')),
    update: (id: number, payload: Partial<MetricItem>) =>
      http.put<MetricItem>(`/catalog/metrics/${id}`, payload).then(r => ensureSuccess(r.data, '更新指标失败')),
    delete: (id: number) =>
      http.delete(`/catalog/metrics/${id}`).then(r => {
        if (r.data && (r.data as any).success === false) {
          throw new Error((r.data as any).message || '删除指标失败');
        }
        return;
      })
  },
  sensors: {
    list: () => http.get<SensorTypeItem[]>('/catalog/sensors').then(r => ensureSuccess(r.data, '获取传感器失败')),
    create: (payload: Partial<SensorTypeItem>) =>
      http.post<SensorTypeItem>('/catalog/sensors', payload).then(r => ensureSuccess(r.data, '新增传感器失败')),
    delete: (id: number) =>
      http.delete(`/catalog/sensors/${id}`).then(r => {
        if (r.data && (r.data as any).success === false) {
          throw new Error((r.data as any).message || '删除传感器失败');
        }
        return;
      })
  }
};

// Alerts
export interface AlertCondition {
  id?: number;
  metricCode: string;
  comparator: 'GT' | 'GTE' | 'LT' | 'LTE' | 'EQ' | string;
  threshold: number;
}

export interface AlertRule {
  id: number;
  name: string;
  description?: string;
  logicOperator: 'AND' | 'OR';
  templateEnabled: boolean;
  templateId?: number;
  templateName?: string;
  departmentId?: number;
  departmentName?: string;
  templateCode?: string;
  templateCategory?: string;
  autoInterrupt: boolean;
  notifyEnabled: boolean;
  notifyChannels?: string;
  notifyTargets?: string;
  notifyTemplate?: string;
  conditions: AlertCondition[];
  unreadCount: number;
}

export interface AlertRecord {
  id: number;
  ruleId: number;
  missionCode?: string;
  uavCode?: string;
  metricCode?: string;
  metricValue?: number;
  triggeredAt?: string;
  processed: boolean;
  linkageStatus?: string;
  linkageSummary?: string;
  notificationStatus?: string;
}

export interface AlertRulePayload {
  name: string;
  description?: string;
  logicOperator: string;
  templateEnabled?: boolean;
  templateId?: number;
  departmentId?: number;
  templateCode?: string;
  templateCategory?: string;
  autoInterrupt?: boolean;
  notifyEnabled?: boolean;
  notifyChannels?: string;
  notifyTargets?: string;
  notifyTemplate?: string;
  conditions: AlertCondition[];
}

export const alertApi = {
  rules: {
    list: () => http.get<AlertRule[]>('/alerts/rules').then(r => r.data),
    options: () => http.get<AlertRule[]>('/alerts/rule-options').then(r => r.data),
    create: (payload: AlertRulePayload) =>
      http.post<AlertRule>('/alerts/rules', payload).then(r => ensureSuccess<AlertRule>(r.data, '创建报警规则失败')),
    update: (id: number, payload: AlertRulePayload) =>
      http.put<AlertRule>(`/alerts/rules/${id}`, payload).then(r => ensureSuccess<AlertRule>(r.data, '更新报警规则失败')),
    delete: (id: number) => http.delete(`/alerts/rules/${id}`).then(() => void 0)
  },
  records: {
    list: (ruleId?: number, missionCodes?: string[]) =>
      http.get<AlertRecord[]>('/alerts/records', { params: { ruleId, missionCodes } }).then(r => r.data),
    process: (id: number) => http.put(`/alerts/records/${id}/process`).then(() => void 0)
  }
};
