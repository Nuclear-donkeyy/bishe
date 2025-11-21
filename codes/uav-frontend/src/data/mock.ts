import type { LatLngTuple } from 'leaflet';

export type UavStatus = 'online' | 'warning' | 'critical';
export type UserRole = 'superadmin' | 'operator';
export type MissionTypeKey =
  | '森林火情巡查'
  | '林业健康监测'
  | '城市热岛监测'
  | '空气质量剖面'
  | '土地利用变化复拍';

export interface MetricDefinition {
  key: string;
  label: string;
  unit?: string;
  desc: string;
}

export interface MissionTypeDefinition {
  name: MissionTypeKey;
  description: string;
  recommendedSensors: string[];
  metrics: MetricDefinition[];
}

export const missionTypeDefinitions: Record<MissionTypeKey, MissionTypeDefinition> = {
  森林火情巡查: {
    name: '森林火情巡查',
    description: '热成像 + 烟雾识别，关注早期火点与气象',
    recommendedSensors: ['热成像', '可见光', '气象套件'],
    metrics: [
      { key: 'surfaceTemp', label: '地表温度', unit: '°C', desc: '热成像最高温度/范围' },
      { key: 'smokeProbability', label: '烟雾识别概率', desc: 'AI 模型输出' },
      { key: 'firePoint', label: '火点坐标', desc: '识别到的火点/热点经纬度' },
      { key: 'temperatureTrend', label: '温度趋势', desc: '任务过程温度变化' }
    ]
  },
  林业健康监测: {
    name: '林业健康监测',
    description: '多光谱/可见光，筛查植被健康与病害',
    recommendedSensors: ['多光谱', '可见光'],
    metrics: [
      { key: 'ndvi', label: 'NDVI', desc: '植被指数' },
      { key: 'evi', label: 'EVI', desc: '增强植被指数' },
      { key: 'disease', label: '病害疑似面积', unit: 'ha', desc: 'AI 病害识别结果' }
    ]
  },
  城市热岛监测: {
    name: '城市热岛监测',
    description: '城市地表温度 + 气象联动',
    recommendedSensors: ['热成像', '气象套件'],
    metrics: [
      { key: 'surfaceTemp', label: '地表温度', unit: '°C', desc: '区域温度分布' },
      { key: 'tempDifference', label: '昼夜温差', unit: '°C', desc: '对比指标' },
      { key: 'stationDiff', label: '地面站差值', unit: '°C', desc: '与地面站对比' }
    ]
  },
  空气质量剖面: {
    name: '空气质量剖面',
    description: '垂直剖面采集颗粒物/气体',
    recommendedSensors: ['PM2.5/PM10', '臭氧', '气象'],
    metrics: [
      { key: 'pm25', label: 'PM2.5', unit: 'µg/m³', desc: '颗粒物浓度' },
      { key: 'pm10', label: 'PM10', unit: 'µg/m³', desc: '颗粒物浓度' },
      { key: 'oxygen', label: '氧气浓度', unit: 'mg/m³', desc: '空气氧含量' },
      { key: 'co2', label: '二氧化碳', unit: 'mg/m³', desc: 'CO₂ 浓度' }
    ]
  },
  土地利用变化复拍: {
    name: '土地利用变化复拍',
    description: '对比前后影像，识别地类变化',
    recommendedSensors: ['多光谱', '可见光'],
    metrics: [
      { key: 'changeArea', label: '变化面积', unit: 'km²', desc: '变化区域' },
      { key: 'changeType', label: '变化类型', desc: '林地→建设等' },
      { key: 'differenceImage', label: '差值影像', desc: '影像链接或编号' }
    ]
  }
};

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
  missionType: MissionTypeKey;
  pilot: string;
  status: '执行中' | '排队' | '完成' | '异常中止';
  priority: '高' | '中' | '低';
  progress: number;
  route: LatLngTuple[];
  color: string;
  milestones: string[];
  metrics: string[];
}

export interface MetricSeriesPoint {
  timestamp: string;
  value: number;
}

export interface MetricSummary {
  max?: number;
  min?: number;
  avg?: number;
  count?: number;
  value?: number;
}

export interface Rule {
  id: string;
  name: string;
  metric: string;
  threshold: string;
  level: '提示' | '预警' | '警报';
}

export interface MonitoringTask {
  id: string;
  missionName: string;
  missionType: MissionTypeKey;
  owner: string;
  status: '执行中' | '计划中' | '完成';
  location: string;
  devices: number;
  data: { label: string; value: string; unit?: string }[];
  metricTrend: Record<string, MetricSeriesPoint[]>;
  executionTimeline: MetricSeriesPoint[];
  rules: Rule[];
  videoUrl?: string;
}

export interface TaskExecution {
  id: string;
  missionName: string;
  missionType: MissionTypeKey;
  completedAt: string;
  location: string;
  owner: string;
  metrics: Record<string, MetricSummary>;
}

export interface AnalyticsSeriesDefinition {
  name: string;
  metric: string;
  field: keyof MetricSummary;
}

export interface AnalyticsDefinition {
  title: string;
  description?: string;
  series: AnalyticsSeriesDefinition[];
}

export const userAccounts: UserAccount[] = [
  { username: 'superadmin', password: '123456', name: '超级管理员', role: 'superadmin' },
  { username: '张三', password: '123456', name: '张三', role: 'operator' },
  { username: '李四', password: '123456', name: '李四', role: 'operator' },
  { username: '王五', password: '123456', name: '王五', role: 'operator' }
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
    missionType: '森林火情巡查',
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
    milestones: ['起飞 08:00', '抵达作业区 08:12', '采集完成 60%', '返航 09:00'],
    metrics: missionTypeDefinitions['森林火情巡查'].metrics.map(metric => metric.label)
  },
  {
    id: 'M-20240605',
    name: '空气质量剖面-城区',
    missionType: '空气质量剖面',
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
    milestones: ['起飞 09:20', '剖面采样 3/6', '告警：PM2.5>120', '返航 10:30'],
    metrics: missionTypeDefinitions['空气质量剖面'].metrics.map(metric => metric.label)
  },
  {
    id: 'M-20240528',
    name: '土地变化-西部',
    missionType: '土地利用变化复拍',
    pilot: '张华',
    status: '执行中',
    priority: '低',
    progress: 0,
    color: '#22c55e',
    route: [
      [31.18, 121.35],
      [31.22, 121.33],
      [31.24, 121.38],
      [31.18, 121.35]
    ],
    milestones: ['排队中'],
    metrics: missionTypeDefinitions['土地利用变化复拍'].metrics.map(metric => metric.label)
  },
  {
    id: 'M-20240610',
    name: '林业健康-江北',
    missionType: '林业健康监测',
    pilot: '张三',
    status: '执行中',
    priority: '中',
    progress: 30,
    color: '#22c55e',
    route: [
      [31.32, 121.47],
      [31.35, 121.5],
      [31.33, 121.55],
      [31.29, 121.5]
    ],
    milestones: ['起飞', '巡检进行中'],
    metrics: missionTypeDefinitions['林业健康监测'].metrics.map(metric => metric.label)
  },
  {
    id: 'M-20240615',
    name: '城市热岛-南区',
    missionType: '城市热岛监测',
    pilot: '王五',
    status: '执行中',
    priority: '高',
    progress: 55,
    color: '#f97316',
    route: [
      [31.12, 121.35],
      [31.18, 121.4],
      [31.15, 121.45],
      [31.09, 121.42]
    ],
    milestones: ['起飞', '采样进行中'],
    metrics: missionTypeDefinitions['城市热岛监测'].metrics.map(metric => metric.label)
  }
];

export const monitoringTasks: MonitoringTask[] = [
  {
    id: 'T-01',
    missionName: '森林火情-北岭',
    missionType: '森林火情巡查',
    owner: '李四',
    status: '执行中',
    location: '北岭林区',
    devices: 2,
    data: [
      { label: '地表温度', value: '62', unit: '°C' },
      { label: '烟雾概率', value: '0.84' },
      { label: '风速', value: '6.2', unit: 'm/s' }
    ],
    metricTrend: {
      地表温度: [
        { timestamp: '08:00', value: 48 },
        { timestamp: '08:10', value: 55 },
        { timestamp: '08:20', value: 60 },
        { timestamp: '08:30', value: 65 },
        { timestamp: '08:40', value: 62 }
      ],
      烟雾概率: [
        { timestamp: '08:00', value: 0.3 },
        { timestamp: '08:10', value: 0.6 },
        { timestamp: '08:20', value: 0.7 },
        { timestamp: '08:30', value: 0.84 },
        { timestamp: '08:40', value: 0.8 }
      ]
    },
    executionTimeline: [
      { timestamp: '任务 1', value: 1 },
      { timestamp: '任务 2', value: 1 },
      { timestamp: '任务 3', value: 1 }
    ],
    videoUrl: 'https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4',
    rules: [
      { id: 'R-01', name: '高温告警', metric: '地表温度', threshold: '> 60°C & 5min', level: '警报' },
      { id: 'R-02', name: '烟雾识别', metric: 'AI 烟雾概率', threshold: '> 0.8', level: '预警' }
    ]
  },
  {
    id: 'T-02',
    missionName: '空气质量剖面-城区',
    missionType: '空气质量剖面',
    owner: '王敏',
    status: '执行中',
    location: '城区西北',
    devices: 1,
    data: [
      { label: 'PM2.5', value: '130', unit: 'µg/m³' },
      { label: 'PM10', value: '165', unit: 'µg/m³' },
      { label: '氧气浓度', value: '190', unit: 'mg/m³' },
      { label: '二氧化碳', value: '420', unit: 'mg/m³' }
    ],
    metricTrend: {
      'PM2.5': [
        { timestamp: '09:20', value: 95 },
        { timestamp: '09:25', value: 110 },
        { timestamp: '09:30', value: 130 },
        { timestamp: '09:35', value: 125 },
        { timestamp: '09:40', value: 118 }
      ],
      PM10: [
        { timestamp: '09:20', value: 120 },
        { timestamp: '09:25', value: 145 },
        { timestamp: '09:30', value: 165 },
        { timestamp: '09:35', value: 158 },
        { timestamp: '09:40', value: 150 }
      ],
      氧气浓度: [
        { timestamp: '09:20', value: 210 },
        { timestamp: '09:25', value: 205 },
        { timestamp: '09:30', value: 198 },
        { timestamp: '09:35', value: 192 },
        { timestamp: '09:40', value: 190 }
      ],
      二氧化碳: [
        { timestamp: '09:20', value: 390 },
        { timestamp: '09:25', value: 405 },
        { timestamp: '09:30', value: 420 },
        { timestamp: '09:35', value: 415 },
        { timestamp: '09:40', value: 405 }
      ]
    },
    executionTimeline: [
      { timestamp: '任务 1', value: 1 },
      { timestamp: '任务 2', value: 1 },
      { timestamp: '任务 3', value: 1 },
      { timestamp: '任务 4', value: 1 }
    ],
    rules: [
      { id: 'R-03', name: 'PM 超标', metric: 'PM2.5', threshold: '> 115 µg/m³', level: '预警' }
    ]
  },
  {
    id: 'T-03',
    missionName: '土地变化-西部',
    missionType: '土地利用变化复拍',
    owner: '张华',
    status: '执行中',
    location: '西部新区',
    devices: 1,
    data: [
      { label: '变化区域', value: '2.3', unit: 'km²' },
      { label: '疑似类型', value: '林地→建设' }
    ],
    metricTrend: {
      变化区域: [
        { timestamp: '05-01', value: 1.1 },
        { timestamp: '05-10', value: 1.8 },
        { timestamp: '05-20', value: 2.1 },
        { timestamp: '05-30', value: 2.3 },
        { timestamp: '06-05', value: 2.3 }
      ]
    },
    executionTimeline: [
      { timestamp: '任务 1', value: 1 },
      { timestamp: '任务 2', value: 1 },
      { timestamp: '任务 3', value: 1 }
    ],
    videoUrl: 'https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4',
    rules: []
  },
  {
    id: 'T-04',
    missionName: '林业健康-江北',
    missionType: '林业健康监测',
    owner: '张三',
    status: '执行中',
    location: '江北林区',
    devices: 1,
    data: [
      { label: 'NDVI 平均值', value: '0.68' },
      { label: '病害疑似面积', value: '12.5', unit: 'ha' }
    ],
    metricTrend: {
      NDVI: [
        { timestamp: 'Q1', value: 0.72 },
        { timestamp: 'Q2', value: 0.7 },
        { timestamp: 'Q3', value: 0.68 },
        { timestamp: 'Q4', value: 0.66 }
      ],
      病害疑似面积: [
        { timestamp: 'Q1', value: 8 },
        { timestamp: 'Q2', value: 9.7 },
        { timestamp: 'Q3', value: 12.5 },
        { timestamp: 'Q4', value: 10.2 }
      ]
    },
    executionTimeline: [
      { timestamp: '任务 1', value: 1 },
      { timestamp: '任务 2', value: 1 },
      { timestamp: '任务 3', value: 1 },
      { timestamp: '任务 4', value: 1 }
    ],
    videoUrl: 'https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4',
    rules: []
  },
  {
    id: 'T-05',
    missionName: '城市热岛-南区',
    missionType: '城市热岛监测',
    owner: '王五',
    status: '执行中',
    location: '南区',
    devices: 1,
    data: [
      { label: '地表温度', value: '38.2', unit: '°C' },
      { label: '昼夜温差', value: '7.5', unit: '°C' }
    ],
    metricTrend: {
      地表温度: [
        { timestamp: '06:00', value: 30 },
        { timestamp: '12:00', value: 38.2 },
        { timestamp: '18:00', value: 33 },
        { timestamp: '00:00', value: 29 }
      ],
      昼夜温差: [
        { timestamp: '日 1', value: 6.5 },
        { timestamp: '日 2', value: 7.0 },
        { timestamp: '日 3', value: 7.2 },
        { timestamp: '日 4', value: 7.5 }
      ]
    },
    executionTimeline: [
      { timestamp: '巡查 1', value: 1 },
      { timestamp: '巡查 2', value: 1 },
      { timestamp: '巡查 3', value: 1 }
    ],
    rules: [
      { id: 'R-04', name: '热岛告警', metric: '地表温度', threshold: '> 37°C & 10min', level: '预警' }
    ]
  }
];

export const taskExecutions: TaskExecution[] = [
  {
    id: 'F-001',
    missionName: '森林火情-北岭-1',
    missionType: '森林火情巡查',
    completedAt: '2024-05-01 09:00',
    location: '北岭林区',
    owner: '李四',
    metrics: {
      地表温度: { max: 60, min: 32, avg: 44 },
      烟雾识别概率: { max: 0.84, min: 0.3, avg: 0.58, count: 8 },
      火点个数: { value: 0 }
    }
  },
  {
    id: 'F-002',
    missionName: '森林火情-北岭-2',
    missionType: '森林火情巡查',
    completedAt: '2024-05-05 09:00',
    location: '北岭林区',
    owner: '李四',
    metrics: {
      地表温度: { max: 64, min: 34, avg: 48 },
      烟雾识别概率: { max: 0.9, min: 0.32, avg: 0.61, count: 11 },
      火点个数: { value: 1 }
    }
  },
  {
    id: 'F-003',
    missionName: '森林火情-北岭-3',
    missionType: '森林火情巡查',
    completedAt: '2024-05-10 09:00',
    location: '北岭林区',
    owner: '李四',
    metrics: {
      地表温度: { max: 66, min: 33, avg: 50 },
      烟雾识别概率: { max: 0.88, min: 0.35, avg: 0.6, count: 10 },
      火点个数: { value: 2 }
    }
  },
  {
    id: 'F-004',
    missionName: '森林火情-北岭-4',
    missionType: '森林火情巡查',
    completedAt: '2024-05-15 09:00',
    location: '北岭林区',
    owner: '李四',
    metrics: {
      地表温度: { max: 63, min: 35, avg: 47 },
      烟雾识别概率: { max: 0.92, min: 0.28, avg: 0.64, count: 12 },
      火点个数: { value: 1 }
    }
  },
  {
    id: 'L-001',
    missionName: '林业健康-江北-1',
    missionType: '林业健康监测',
    completedAt: '2024-04-01',
    location: '江北林区',
    owner: '张三',
    metrics: {
      植被指数: { value: 0.72 },
      增强植被指数: { value: 0.53 },
      病害疑似面积: { value: 6 }
    }
  },
  {
    id: 'L-002',
    missionName: '林业健康-江北-2',
    missionType: '林业健康监测',
    completedAt: '2024-04-15',
    location: '江北林区',
    owner: '张三',
    metrics: {
      植被指数: { value: 0.7 },
      增强植被指数: { value: 0.51 },
      病害疑似面积: { value: 8 }
    }
  },
  {
    id: 'L-003',
    missionName: '林业健康-江北-3',
    missionType: '林业健康监测',
    completedAt: '2024-05-01',
    location: '江北林区',
    owner: '张三',
    metrics: {
      植被指数: { value: 0.68 },
      增强植被指数: { value: 0.49 },
      病害疑似面积: { value: 12.5 }
    }
  },
  {
    id: 'L-004',
    missionName: '林业健康-江北-4',
    missionType: '林业健康监测',
    completedAt: '2024-06-01',
    location: '江北林区',
    owner: '张三',
    metrics: {
      植被指数: { value: 0.64 },
      增强植被指数: { value: 0.46 },
      病害疑似面积: { value: 9.3 }
    }
  },
  {
    id: 'H-001',
    missionName: '城市热岛-南区-1',
    missionType: '城市热岛监测',
    completedAt: '2024-06-01',
    location: '南区',
    owner: '王五',
    metrics: {
      地表温度: { max: 36, min: 24, avg: 30 },
      热岛强度指数: { value: 4.5 }
    }
  },
  {
    id: 'H-002',
    missionName: '城市热岛-南区-2',
    missionType: '城市热岛监测',
    completedAt: '2024-06-05',
    location: '南区',
    owner: '王五',
    metrics: {
      地表温度: { max: 38.2, min: 25, avg: 31.8 },
      热岛强度指数: { value: 5.2 }
    }
  },
  {
    id: 'H-003',
    missionName: '城市热岛-南区-3',
    missionType: '城市热岛监测',
    completedAt: '2024-06-10',
    location: '南区',
    owner: '王五',
    metrics: {
      地表温度: { max: 39, min: 26, avg: 32.5 },
      热岛强度指数: { value: 5.5 }
    }
  },
  {
    id: 'H-004',
    missionName: '城市热岛-南区-4',
    missionType: '城市热岛监测',
    completedAt: '2024-06-15',
    location: '南区',
    owner: '王五',
    metrics: {
      地表温度: { max: 37.5, min: 25.5, avg: 31 },
      热岛强度指数: { value: 4.9 }
    }
  },
  {
    id: 'A-001',
    missionName: '空气质量剖面-城区-1',
    missionType: '空气质量剖面',
    completedAt: '2024-06-01',
    location: '城区西北',
    owner: '王敏',
    metrics: {
      'PM2.5': { max: 118, min: 85, avg: 96 },
      PM10: { max: 140, min: 110, avg: 128 },
      氧气浓度: { max: 215, min: 195, avg: 205 },
      二氧化碳: { max: 430, min: 380, avg: 405 }
    }
  },
  {
    id: 'A-002',
    missionName: '空气质量剖面-城区-2',
    missionType: '空气质量剖面',
    completedAt: '2024-06-05',
    location: '城区西北',
    owner: '王敏',
    metrics: {
      'PM2.5': { max: 130, min: 90, avg: 108 },
      PM10: { max: 165, min: 120, avg: 143 },
      氧气浓度: { max: 210, min: 192, avg: 202 },
      二氧化碳: { max: 420, min: 390, avg: 408 }
    }
  },
  {
    id: 'A-003',
    missionName: '空气质量剖面-城区-3',
    missionType: '空气质量剖面',
    completedAt: '2024-06-10',
    location: '城区西北',
    owner: '王敏',
    metrics: {
      'PM2.5': { max: 125, min: 95, avg: 110 },
      PM10: { max: 158, min: 125, avg: 138 },
      氧气浓度: { max: 208, min: 188, avg: 198 },
      二氧化碳: { max: 415, min: 395, avg: 405 }
    }
  },
  {
    id: 'A-004',
    missionName: '空气质量剖面-城区-4',
    missionType: '空气质量剖面',
    completedAt: '2024-06-15',
    location: '城区西北',
    owner: '王敏',
    metrics: {
      'PM2.5': { max: 120, min: 92, avg: 105 },
      PM10: { max: 150, min: 118, avg: 134 },
      氧气浓度: { max: 205, min: 186, avg: 196 },
      二氧化碳: { max: 410, min: 388, avg: 399 }
    }
  },
  {
    id: 'TCH-001',
    missionName: '土地变化-西部-1',
    missionType: '土地利用变化复拍',
    completedAt: '2024-05-01',
    location: '西部新区',
    owner: '张华',
    metrics: {
      疑似变化: { count: 20 }
    }
  },
  {
    id: 'TCH-002',
    missionName: '土地变化-西部-2',
    missionType: '土地利用变化复拍',
    completedAt: '2024-05-10',
    location: '西部新区',
    owner: '张华',
    metrics: {
      疑似变化: { count: 34 }
    }
  },
  {
    id: 'TCH-003',
    missionName: '土地变化-西部-3',
    missionType: '土地利用变化复拍',
    completedAt: '2024-05-20',
    location: '西部新区',
    owner: '张华',
    metrics: {
      疑似变化: { count: 41 }
    }
  },
  {
    id: 'TCH-004',
    missionName: '土地变化-西部-4',
    missionType: '土地利用变化复拍',
    completedAt: '2024-05-30',
    location: '西部新区',
    owner: '张华',
    metrics: {
      疑似变化: { count: 38 }
    }
  }
];

export const analyticsDefinitions: Record<MissionTypeKey, AnalyticsDefinition[]> = {
  森林火情巡查: [
    {
      title: '地表温度',
      description: '最高/最低/平均地表温度',
      series: [
        { name: '最高地表温度', metric: '地表温度', field: 'max' },
        { name: '最低地表温度', metric: '地表温度', field: 'min' },
        { name: '平均地表温度', metric: '地表温度', field: 'avg' }
      ]
    },
    {
      title: '烟雾识别概率',
      description: '最高/最低/平均识别概率与识别次数',
      series: [
        { name: '最高识别概率', metric: '烟雾识别概率', field: 'max' },
        { name: '最低识别概率', metric: '烟雾识别概率', field: 'min' },
        { name: '平均识别概率', metric: '烟雾识别概率', field: 'avg' },
        { name: '识别概率个数', metric: '烟雾识别概率', field: 'count' }
      ]
    },
    {
      title: '火点个数',
      series: [{ name: '火点个数', metric: '火点个数', field: 'value' }]
    }
  ],
  林业健康监测: [
    {
      title: '植被指数 NDVI',
      series: [{ name: '植被指数', metric: '植被指数', field: 'value' }]
    },
    {
      title: '增强植被指数',
      series: [{ name: '增强植被指数', metric: '增强植被指数', field: 'value' }]
    },
    {
      title: '病害疑似面积',
      series: [{ name: '病害面积 (ha)', metric: '病害疑似面积', field: 'value' }]
    }
  ],
  城市热岛监测: [
    {
      title: '地表温度',
      series: [
        { name: '最高温度', metric: '地表温度', field: 'max' },
        { name: '最低温度', metric: '地表温度', field: 'min' },
        { name: '平均温度', metric: '地表温度', field: 'avg' }
      ]
    },
    {
      title: '热岛强度指数',
      series: [{ name: '热岛强度指数', metric: '热岛强度指数', field: 'value' }]
    }
  ],
  空气质量剖面: [
    {
      title: 'PM2.5',
      series: [
        { name: '最大值', metric: 'PM2.5', field: 'max' },
        { name: '最小值', metric: 'PM2.5', field: 'min' },
        { name: '平均值', metric: 'PM2.5', field: 'avg' }
      ]
    },
    {
      title: 'PM10',
      series: [
        { name: '最大值', metric: 'PM10', field: 'max' },
        { name: '最小值', metric: 'PM10', field: 'min' },
        { name: '平均值', metric: 'PM10', field: 'avg' }
      ]
    },
    {
      title: '氧气浓度',
      series: [
        { name: '最大值', metric: '氧气浓度', field: 'max' },
        { name: '最小值', metric: '氧气浓度', field: 'min' },
        { name: '平均值', metric: '氧气浓度', field: 'avg' }
      ]
    },
    {
      title: '二氧化碳',
      series: [
        { name: '最大值', metric: '二氧化碳', field: 'max' },
        { name: '最小值', metric: '二氧化碳', field: 'min' },
        { name: '平均值', metric: '二氧化碳', field: 'avg' }
      ]
    }
  ],
  土地利用变化复拍: [
    {
      title: '疑似变化个数',
      series: [{ name: '疑似变化个数', metric: '疑似变化', field: 'count' }]
    }
  ]
};
