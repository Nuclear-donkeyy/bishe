import type { LatLngTuple } from 'leaflet';

export type UavStatus = 'online' | 'warning' | 'critical';
export type UserRole = 'superadmin' | 'operator';

export interface UserAccount {
  username: string;
  password: string;
  name: string;
  role: UserRole;
}

export interface FleetItem {
  id: string;
  model: string;
  mission: string;
  pilot: string;
  battery: number;
  linkQuality: string;
  rtt: number;
  status: UavStatus;
}

export interface Mission {
  id: string;
  name: string;
  type: string;
  pilot: string;
  status: '执行中' | '排队' | '完成';
  priority: '高' | '中' | '低';
  progress: number;
  route: LatLngTuple[];
  color: string;
  milestones: string[];
}

export interface MonitoringTask {
  id: string;
  missionName: string;
  owner: string;
  status: '执行中' | '计划中' | '完成';
  location: string;
  devices: number;
  data: { label: string; value: string; unit?: string }[];
  rules: Rule[];
}

export interface Rule {
  id: string;
  name: string;
  metric: string;
  threshold: string;
  level: '提示' | '预警' | '警报';
}

export const userAccounts: UserAccount[] = [
  {
    username: 'superadmin',
    password: '123456',
    name: '超级管理员',
    role: 'superadmin'
  },
  {
    username: '张三',
    password: '123456',
    name: '张三',
    role: 'operator'
  },
  {
    username: '李四',
    password: '123456',
    name: '李四',
    role: 'operator'
  },
  {
    username: '王五',
    password: '123456',
    name: '王五',
    role: 'operator'
  }
];

export const fleetSummary = {
  online: 42,
  warning: 4,
  alerts: 2,
  avgRtt: 130
};

export const fleetItems: FleetItem[] = [
  {
    id: 'UAV-21',
    model: '多旋翼 · PX4 1.13',
    mission: '林业健康-江北',
    pilot: '张三',
    battery: 78,
    linkQuality: '优',
    rtt: 120,
    status: 'online'
  },
  {
    id: 'UAV-09',
    model: '固定翼 · CubeOrange',
    mission: '火情巡查-北段',
    pilot: '张三',
    battery: 38,
    linkQuality: '弱',
    rtt: 320,
    status: 'warning'
  },
  {
    id: 'UAV-33',
    model: '多旋翼 · PX4 1.13',
    mission: '空气质量-剖面组',
    pilot: '李四',
    battery: 56,
    linkQuality: '良',
    rtt: 180,
    status: 'online'
  },
  {
    id: 'UAV-12',
    model: '复合翼 · ArduPilot',
    mission: '土地利用-西郊',
    pilot: '王五',
    battery: 64,
    linkQuality: '优',
    rtt: 140,
    status: 'online'
  },
  {
    id: 'UAV-77',
    model: '多旋翼 · PX4 1.13',
    mission: '森林火情-北岭',
    pilot: '超级管理员',
    battery: 58,
    linkQuality: '良',
    rtt: 200,
    status: 'online'
  }
];

export const missionList: Mission[] = [
  {
    id: 'M-20240601',
    name: '森林火情-北岭',
    type: '火情巡查',
    pilot: '李四',
    status: '执行中',
    priority: '高',
    progress: 45,
    color: '#f97316',
    route: [
      [31.27, 121.46],
      [31.31, 121.53],
      [31.36, 121.47],
      [31.27, 121.46]
    ],
    milestones: ['起飞 08:00', '抵达作业区 08:12', '采集完成 60%', '返航 09:00']
  },
  {
    id: 'M-20240605',
    name: '空气质量剖面-城区',
    type: '空气质量',
    pilot: '王敏',
    status: '执行中',
    priority: '中',
    progress: 62,
    color: '#0ea5e9',
    route: [
      [31.23, 121.41],
      [31.28, 121.42],
      [31.32, 121.38],
      [31.23, 121.41]
    ],
    milestones: ['起飞 09:20', '剖面采样 3/6', '告警：PM2.5>120', '返航 10:30']
  },
  {
    id: 'M-20240528',
    name: '土地变化-西部',
    type: '土地利用',
    pilot: '张华',
    status: '排队',
    priority: '低',
    progress: 0,
    color: '#22c55e',
    route: [
      [31.18, 121.35],
      [31.22, 121.33],
      [31.24, 121.38],
      [31.18, 121.35]
    ],
    milestones: ['排队中']
  }
];

export const monitoringTasks: MonitoringTask[] = [
  {
    id: 'T-01',
    missionName: '森林火情-北岭',
    owner: '李四',
    status: '执行中',
    location: '北岭林区',
    devices: 2,
    data: [
      { label: '地表温度', value: '62', unit: '°C' },
      { label: '烟雾概率', value: '0.84' },
      { label: '风速', value: '6.2', unit: 'm/s' }
    ],
    rules: [
      { id: 'R-01', name: '高温告警', metric: '地表温度', threshold: '> 60°C & 5min', level: '警报' },
      { id: 'R-02', name: '烟雾识别', metric: 'AI 烟雾概率', threshold: '> 0.8', level: '预警' }
    ]
  },
  {
    id: 'T-02',
    missionName: '空气质量剖面-城区',
    owner: '王敏',
    status: '执行中',
    location: '城区西北',
    devices: 1,
    data: [
      { label: 'PM2.5 峰值', value: '130', unit: 'µg/m³' },
      { label: 'PM10', value: '165', unit: 'µg/m³' },
      { label: '臭氧', value: '120', unit: 'µg/m³' }
    ],
    rules: [
      { id: 'R-03', name: 'PM 超标', metric: 'PM2.5', threshold: '> 115 µg/m³', level: '预警' }
    ]
  },
  {
    id: 'T-03',
    missionName: '土地变化-西部',
    owner: '张华',
    status: '计划中',
    location: '西部新区',
    devices: 1,
    data: [
      { label: '变化区域', value: '2.3', unit: 'km²' },
      { label: '疑似类型', value: '林地→建设' }
    ],
    rules: []
  }
];
