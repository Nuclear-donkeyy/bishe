import {
  Alert,
  Card,
  Col,
  Empty,
  List,
  Progress,
  Row,
  Space,
  Statistic,
  Tag,
  Typography
} from 'antd';
import dayjs from 'dayjs';
import { useEffect, useMemo, useState } from 'react';
import {
  alertApi,
  configApi,
  fleetApi,
  missionApi,
  monitoringApi,
  type AlertRecord,
  type AlertRule,
  type FleetSummary,
  type MissionDto,
  type MissionTypeItem,
  type MonitoringTaskDto,
  type SensorTypeItem,
  type MetricItem
} from '../services/api';

type CatalogSummary = {
  missionTypes: MissionTypeItem[];
  metrics: MetricItem[];
  sensors: SensorTypeItem[];
};

function isRunningMission(status?: string) {
  return status?.includes('执') || status === 'RUNNING';
}

function isQueuedMission(status?: string) {
  return (
    status?.includes('队') ||
    status === 'PENDING' ||
    status === 'QUEUE' ||
    status === 'PREEMPTED'
  );
}

function isCompletedMission(status?: string) {
  return status?.includes('完成') || status === 'COMPLETED';
}

function isInterruptedMission(status?: string) {
  return status?.includes('中断') || status === 'INTERRUPTED';
}

function statusLabel(status?: string) {
  if (!status) return '--';
  if (status === 'RUNNING') return '运行中';
  if (status === 'QUEUE') return '排队中';
  if (status === 'PREEMPTED') return '已抢占';
  if (status === 'COMPLETED') return '已完成';
  if (status === 'INTERRUPTED') return '已中断';
  if (status === 'PENDING') return '待执行';
  return status;
}

function statusColor(status?: string) {
  if (!status) return 'default';
  if (status === 'RUNNING') return 'processing';
  if (status === 'COMPLETED') return 'success';
  if (status === 'INTERRUPTED') return 'warning';
  if (status === 'PREEMPTED') return 'purple';
  if (status === 'QUEUE' || status === 'PENDING') return 'blue';
  return 'default';
}

function priorityLabel(priority?: string) {
  if (!priority) return '--';
  if (priority === 'HIGH') return '高优先级';
  if (priority === 'MEDIUM') return '中优先级';
  if (priority === 'LOW') return '低优先级';
  return priority;
}

function metricPercent(value: number, total: number) {
  if (!total) return 0;
  return Number(((value / total) * 100).toFixed(1));
}

function Dashboard() {
  const [summary, setSummary] = useState<FleetSummary | null>(null);
  const [missions, setMissions] = useState<MissionDto[]>([]);
  const [alerts, setAlerts] = useState<AlertRecord[]>([]);
  const [rules, setRules] = useState<AlertRule[]>([]);
  const [monitoringTasks, setMonitoringTasks] = useState<MonitoringTaskDto[]>([]);
  const [catalog, setCatalog] = useState<CatalogSummary>({ missionTypes: [], metrics: [], sensors: [] });
  const [fleetTotal, setFleetTotal] = useState(0);

  useEffect(() => {
    let cancelled = false;

    Promise.allSettled([
      fleetApi.summary(),
      missionApi.list(),
      fleetApi.list({ page: 1, pageSize: 200 }),
      alertApi.records.list(),
      alertApi.rules.list(),
      monitoringApi.list(),
      configApi.missionTypes.list(),
      configApi.metrics.list(),
      configApi.sensors.list()
    ]).then(results => {
      if (cancelled) {
        return;
      }

      const [
        summaryResult,
        missionResult,
        fleetResult,
        alertResult,
        ruleResult,
        monitoringResult,
        missionTypesResult,
        metricsResult,
        sensorsResult
      ] = results;

      setSummary(summaryResult.status === 'fulfilled' ? summaryResult.value : null);
      setMissions(missionResult.status === 'fulfilled' ? missionResult.value : []);
      setFleetTotal(fleetResult.status === 'fulfilled' ? fleetResult.value.total : 0);
      setAlerts(alertResult.status === 'fulfilled' ? alertResult.value : []);
      setRules(ruleResult.status === 'fulfilled' ? ruleResult.value : []);
      setMonitoringTasks(monitoringResult.status === 'fulfilled' ? monitoringResult.value : []);
      setCatalog({
        missionTypes:
          missionTypesResult.status === 'fulfilled' ? (missionTypesResult.value as MissionTypeItem[]) : [],
        metrics: metricsResult.status === 'fulfilled' ? (metricsResult.value as MetricItem[]) : [],
        sensors: sensorsResult.status === 'fulfilled' ? (sensorsResult.value as SensorTypeItem[]) : []
      });
    });

    return () => {
      cancelled = true;
    };
  }, []);

  const runningMissions = useMemo(
    () => missions.filter(mission => isRunningMission(mission.status)),
    [missions]
  );
  const queuedMissions = useMemo(
    () => missions.filter(mission => isQueuedMission(mission.status)),
    [missions]
  );
  const completedMissions = useMemo(
    () => missions.filter(mission => isCompletedMission(mission.status)),
    [missions]
  );
  const interruptedMissions = useMemo(
    () => missions.filter(mission => isInterruptedMission(mission.status)),
    [missions]
  );
  const protectedMissions = useMemo(
    () => missions.filter(mission => mission.ruleId != null).length,
    [missions]
  );
  const activeMonitoringTasks = useMemo(
    () => monitoringTasks.filter(task => task.status === 'RUNNING' || task.status === 'ACTIVE').length,
    [monitoringTasks]
  );
  const missionSuccessRate = useMemo(
    () => metricPercent(completedMissions.length, completedMissions.length + interruptedMissions.length),
    [completedMissions.length, interruptedMissions.length]
  );
  const ruleCoverage = useMemo(
    () => metricPercent(protectedMissions, missions.length),
    [missions.length, protectedMissions]
  );

  const missionStatusDistribution = useMemo(
    () => [
      { label: '运行中', value: runningMissions.length, color: '#1565f5' },
      { label: '排队中', value: queuedMissions.length, color: '#5b8def' },
      { label: '已完成', value: completedMissions.length, color: '#12a594' },
      { label: '已中断', value: interruptedMissions.length, color: '#d9961a' }
    ],
    [completedMissions.length, interruptedMissions.length, queuedMissions.length, runningMissions.length]
  );

  const missionTypeDistribution = useMemo(() => {
    const grouped = missions.reduce<Record<string, number>>((acc, mission) => {
      const key = mission.missionType || '未分类任务';
      acc[key] = (acc[key] || 0) + 1;
      return acc;
    }, {});
    return Object.entries(grouped)
      .map(([label, value]) => ({ label, value }))
      .sort((left, right) => right.value - left.value)
      .slice(0, 5);
  }, [missions]);

  const priorityDistribution = useMemo(() => {
    const grouped = missions.reduce<Record<string, number>>((acc, mission) => {
      const key = priorityLabel(mission.priority);
      acc[key] = (acc[key] || 0) + 1;
      return acc;
    }, {});
    return Object.entries(grouped).map(([label, value]) => ({ label, value }));
  }, [missions]);

  const recentAlerts = useMemo(
    () =>
      [...alerts]
        .sort((left, right) => dayjs(right.triggeredAt).valueOf() - dayjs(left.triggeredAt).valueOf())
        .slice(0, 6),
    [alerts]
  );

  const focusMissions = useMemo(
    () =>
      [...runningMissions, ...queuedMissions]
        .sort((left, right) => (right.progress ?? 0) - (left.progress ?? 0))
        .slice(0, 6),
    [queuedMissions, runningMissions]
  );

  const ruleStats = useMemo(() => {
    const templates = rules.filter(rule => rule.templateEnabled).length;
    const normalRules = rules.filter(rule => !rule.templateEnabled).length;
    const unread = rules.filter(rule => !rule.templateEnabled).reduce((sum, rule) => sum + (rule.unreadCount || 0), 0);
    return { templates, normalRules, unread };
  }, [rules]);

  const monitoringRuleCount = useMemo(
    () => monitoringTasks.reduce((sum, task) => sum + (task.rules?.length || 0), 0),
    [monitoringTasks]
  );

  const overviewStats = [
    { title: '在线无人机', value: summary?.online ?? 0, suffix: '架' },
    { title: '在管任务', value: missions.length, suffix: '个' },
    { title: '告警总量', value: alerts.length || summary?.alerts || 0, suffix: '条' },
    { title: '平均链路 RTT', value: summary?.avgRtt ?? 0, suffix: 'ms', precision: 0 }
  ];

  const derivedStats = [
    { title: '任务成功率', value: missionSuccessRate, suffix: '%', precision: 1 },
    { title: '规则覆盖率', value: ruleCoverage, suffix: '%', precision: 1 },
    { title: '监控中任务', value: activeMonitoringTasks, suffix: '个', precision: 0 },
    { title: '接入机队规模', value: fleetTotal, suffix: '架', precision: 0 }
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <Card className="hero-card">
        <Space direction="vertical" size={10} style={{ width: '100%' }}>
          <Typography.Title level={3} style={{ color: '#f8fafc', margin: 0 }}>
            运行总览
          </Typography.Title>
          <Space wrap size={[8, 8]}>
            <Tag color="blue">运行中任务 {runningMissions.length}</Tag>
            <Tag color="cyan">排队任务 {queuedMissions.length}</Tag>
            <Tag color="green">已完成任务 {completedMissions.length}</Tag>
            <Tag color="gold">活跃监控任务 {activeMonitoringTasks}</Tag>
            <Tag color="red">未处理告警 {ruleStats.unread}</Tag>
          </Space>
        </Space>
      </Card>

      <Row gutter={[16, 16]}>
        {overviewStats.map(item => (
          <Col xs={24} sm={12} xl={6} key={item.title}>
            <Card>
              <Statistic
                title={item.title}
                value={item.value}
                suffix={item.suffix}
                precision={item.precision}
                valueStyle={{ fontWeight: 800 }}
              />
            </Card>
          </Col>
        ))}
      </Row>

      <Row gutter={[16, 16]}>
        {derivedStats.map(item => (
          <Col xs={24} sm={12} xl={6} key={item.title}>
            <Card>
              <Statistic
                title={item.title}
                value={item.value}
                suffix={item.suffix}
                precision={item.precision}
                valueStyle={{ fontWeight: 800 }}
              />
            </Card>
          </Col>
        ))}
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} xl={13}>
          <Card title="系统运行态势">
            <Space direction="vertical" size={18} style={{ width: '100%' }}>
              <div>
                <Typography.Title level={5} style={{ marginTop: 0 }}>
                  任务状态分布
                </Typography.Title>
                <Space direction="vertical" size={12} style={{ width: '100%' }}>
                  {missionStatusDistribution.map(item => (
                    <div key={item.label}>
                      <Space style={{ width: '100%', justifyContent: 'space-between' }}>
                        <Typography.Text>{item.label}</Typography.Text>
                        <Typography.Text strong>{item.value} 个</Typography.Text>
                      </Space>
                      <Progress
                        percent={metricPercent(item.value, missions.length)}
                        showInfo={false}
                        strokeColor={item.color}
                        trailColor="#edf3fb"
                      />
                    </div>
                  ))}
                </Space>
              </div>

              <div>
                <Typography.Title level={5} style={{ marginTop: 0 }}>
                  任务类型覆盖
                </Typography.Title>
                {missionTypeDistribution.length ? (
                  <Space direction="vertical" size={12} style={{ width: '100%' }}>
                    {missionTypeDistribution.map(item => (
                      <div key={item.label}>
                        <Space style={{ width: '100%', justifyContent: 'space-between' }}>
                          <Typography.Text>{item.label}</Typography.Text>
                          <Typography.Text type="secondary">{item.value} 个</Typography.Text>
                        </Space>
                        <Progress
                          percent={metricPercent(item.value, missions.length)}
                          showInfo={false}
                          strokeColor="#5b8def"
                          trailColor="#edf3fb"
                        />
                      </div>
                    ))}
                  </Space>
                ) : (
                  <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无任务类型数据" />
                )}
              </div>

              <div>
                <Typography.Title level={5} style={{ marginTop: 0 }}>
                  优先级结构
                </Typography.Title>
                <Space wrap size={[8, 8]}>
                  {priorityDistribution.map(item => (
                    <Tag key={item.label} color="blue">
                      {item.label} {item.value}
                    </Tag>
                  ))}
                </Space>
              </div>
            </Space>
          </Card>
        </Col>

        <Col xs={24} xl={11}>
          <Card title="重点关注">
            <Row gutter={[12, 12]}>
              <Col span={24}>
                <Card size="small" title="任务动态">
                  {focusMissions.length ? (
                    <List
                      dataSource={focusMissions}
                      renderItem={mission => (
                        <List.Item>
                          <Space direction="vertical" size={4} style={{ width: '100%' }}>
                            <Space style={{ width: '100%', justifyContent: 'space-between' }} align="start">
                              <Space direction="vertical" size={2}>
                                <Typography.Text strong>{mission.name}</Typography.Text>
                                <Typography.Text type="secondary">
                                  {mission.missionCode} · {mission.missionType}
                                </Typography.Text>
                              </Space>
                              <Tag color={statusColor(mission.status)}>{statusLabel(mission.status)}</Tag>
                            </Space>
                            <Space style={{ width: '100%', justifyContent: 'space-between' }}>
                              <Typography.Text type="secondary">负责人 {mission.pilotName || '--'}</Typography.Text>
                              <Typography.Text type="secondary">进度 {mission.progress ?? 0}%</Typography.Text>
                            </Space>
                            <Progress
                              percent={mission.progress ?? 0}
                              size="small"
                              showInfo={false}
                              strokeColor={isRunningMission(mission.status) ? '#1565f5' : '#7ba8ff'}
                              trailColor="#edf3fb"
                            />
                          </Space>
                        </List.Item>
                      )}
                    />
                  ) : (
                    <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="当前没有需要重点关注的任务" />
                  )}
                </Card>
              </Col>

              <Col span={24}>
                <Card size="small" title="近期告警">
                  {recentAlerts.length ? (
                    <List
                      dataSource={recentAlerts}
                      renderItem={record => (
                        <List.Item>
                          <Space direction="vertical" size={4} style={{ width: '100%' }}>
                            <Space style={{ width: '100%', justifyContent: 'space-between' }} align="start">
                              <Typography.Text strong>{record.metricCode || '报警记录'}</Typography.Text>
                              <Tag color={record.processed ? 'default' : 'red'}>
                                {record.processed ? '已处理' : '待处理'}
                              </Tag>
                            </Space>
                            <Typography.Text type="secondary">
                              {record.missionCode || '--'} · {record.uavCode || '--'}
                            </Typography.Text>
                            <Typography.Text type="secondary">
                              {record.metricValue == null ? '--' : `触发值 ${record.metricValue}`} ·{' '}
                              {record.triggeredAt ? dayjs(record.triggeredAt).format('MM-DD HH:mm:ss') : '--'}
                            </Typography.Text>
                          </Space>
                        </List.Item>
                      )}
                    />
                  ) : (
                    <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="当前暂无告警记录" />
                  )}
                </Card>
              </Col>
            </Row>
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} xl={12}>
          <Card title="系统能力覆盖">
            <Row gutter={[12, 12]}>
              <Col xs={12} md={8}>
                <Card size="small">
                  <Statistic title="任务类型" value={catalog.missionTypes.length} suffix="类" />
                </Card>
              </Col>
              <Col xs={12} md={8}>
                <Card size="small">
                  <Statistic title="指标定义" value={catalog.metrics.length} suffix="项" />
                </Card>
              </Col>
              <Col xs={12} md={8}>
                <Card size="small">
                  <Statistic title="传感器类型" value={catalog.sensors.length} suffix="类" />
                </Card>
              </Col>
              <Col xs={12} md={8}>
                <Card size="small">
                  <Statistic title="普通规则" value={ruleStats.normalRules} suffix="条" />
                </Card>
              </Col>
              <Col xs={12} md={8}>
                <Card size="small">
                  <Statistic title="规则模板" value={ruleStats.templates} suffix="个" />
                </Card>
              </Col>
              <Col xs={12} md={8}>
                <Card size="small">
                  <Statistic title="监控规则" value={monitoringRuleCount} suffix="条" />
                </Card>
              </Col>
            </Row>
          </Card>
        </Col>

        <Col xs={24} xl={12}>
          <Card title="运行摘要">
            <Space direction="vertical" size={14} style={{ width: '100%' }}>
              <Alert
                type="success"
                showIcon
                message="任务执行概况"
                description={`当前运行中 ${runningMissions.length} 个，排队中 ${queuedMissions.length} 个，已完成 ${completedMissions.length} 个，中断 ${interruptedMissions.length} 个。`}
              />
              <Alert
                type="info"
                showIcon
                message="监控与联动覆盖"
                description={`当前共有 ${monitoringTasks.length} 个监控任务，${activeMonitoringTasks} 个处于活跃状态；已有 ${protectedMissions} 个任务接入报警规则。`}
              />
              <Alert
                type="warning"
                showIcon
                message="风险提示"
                description={`当前链路预警 ${summary?.warning ?? 0} 架，未处理告警 ${ruleStats.unread} 条，建议优先关注运行中任务与最近告警。`}
              />
            </Space>
          </Card>
        </Col>
      </Row>
    </div>
  );
}

export default Dashboard;
