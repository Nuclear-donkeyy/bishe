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
  role: 'SUPERADMIN' | 'OPERATOR' | string;
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
  status: UavStatus;
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
  deleteRule: (ruleId: number) => http.delete(`/monitoring/tasks/rules/${ruleId}`).then(() => void 0)
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
  location?: string;
  ownerName?: string;
  completedAt: string;
  metrics: string;
}

export const analyticsApi = {
  definitions: (missionType?: string) =>
    http.get<AnalyticsDefinitionDto[]>('/analytics/definitions', { params: { missionType } }).then(r => r.data),
  executions: (missionType: string, from?: string, to?: string) =>
    http
      .get<TaskExecutionDto[]>('/analytics/task-executions', { params: { missionType, from, to } })
      .then(r => r.data)
};

// Users
export interface UserRow {
  id: number;
  username: string;
  name: string;
  role: string;
}
export const userApi = {
  list: () => http.get<UserRow[]>('/users').then(r => ensureSuccess<UserRow[]>(r.data, '获取用户失败'))
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
