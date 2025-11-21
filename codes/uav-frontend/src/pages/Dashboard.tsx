import { Card, Col, Row, Statistic, Typography } from 'antd';
import dayjs from 'dayjs';
import { fleetSummary, missionList } from '../data/mock';

const nextMilestone = missionList.find(mission => mission.status === '执行中');

function Dashboard() {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <Typography.Title level={3} style={{ margin: 0 }}>
        今日运行概览
      </Typography.Title>
      <Row gutter={[16, 16]}>
        <Col xs={24} md={6}>
          <Card>
            <Statistic title="在线无人机" value={fleetSummary.online} suffix="架" />
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card>
            <Statistic title="链路预警" value={fleetSummary.warning} suffix="架" />
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card>
            <Statistic title="告警事件" value={fleetSummary.alerts} suffix="条" />
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card>
            <Statistic title="平均 RTT" value={fleetSummary.avgRtt} suffix="ms" precision={0} />
          </Card>
        </Col>
      </Row>
      <Card title="任务动态">
        <Typography.Paragraph>
          执行中任务 {missionList.filter(m => m.status === '执行中').length} 个，排队任务{' '}
          {missionList.filter(m => m.status === '排队').length} 个。
          {nextMilestone
            ? ` 下一任务：${nextMilestone.name} · ${nextMilestone.progress}% · 负责人 ${nextMilestone.pilot}`
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
