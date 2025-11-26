import { Card, Col, Row, Statistic, Typography } from 'antd';
import dayjs from 'dayjs';
import { useEffect, useMemo, useState } from 'react';
import { fleetApi, missionApi, type FleetSummary, type MissionDto } from '../services/api';

function Dashboard() {
  const [summary, setSummary] = useState<FleetSummary | null>(null);
  const [missions, setMissions] = useState<MissionDto[]>([]);

  useEffect(() => {
    fleetApi.summary().then(setSummary).catch(() => setSummary(null));
    missionApi.list().then(setMissions).catch(() => setMissions([]));
  }, []);

  const runningMissions = useMemo(
    () => missions.filter(mission => mission.status?.includes('执') || mission.status === 'RUNNING'),
    [missions]
  );
  const queuedMissions = useMemo(
    () => missions.filter(mission => mission.status?.includes('队') || mission.status === 'PENDING'),
    [missions]
  );
  const nextMilestone = useMemo(
    () => runningMissions.find(mission => (mission.progress ?? 0) < 100),
    [runningMissions]
  );

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <Typography.Title level={3} style={{ margin: 0 }}>
        今日运行概览
      </Typography.Title>
      <Row gutter={[16, 16]}>
        <Col xs={24} md={6}>
          <Card>
            <Statistic title="在线无人机" value={summary?.online ?? 0} suffix="架" />
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card>
            <Statistic title="链路预警" value={summary?.warning ?? 0} suffix="架" />
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card>
            <Statistic title="告警事件" value={summary?.alerts ?? 0} suffix="个" />
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card>
            <Statistic title="平均 RTT" value={summary?.avgRtt ?? 0} suffix="ms" precision={0} />
          </Card>
        </Col>
      </Row>
      <Card title="任务动态">
        <Typography.Paragraph>
          执行中任务 {runningMissions.length} 个，排队任务 {queuedMissions.length} 个。
          {nextMilestone
            ? ` 下一任务：${nextMilestone.name} · ${nextMilestone.progress}% · 负责人 ${nextMilestone.pilotName}`
            : null}
        </Typography.Paragraph>
        <Typography.Text type="secondary">
          更新时间：{dayjs().format('YYYY-MM-DD HH:mm')}
        </Typography.Text>
      </Card>
    </div>
  );
}

export default Dashboard;
