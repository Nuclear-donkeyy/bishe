import { EnvironmentOutlined, PlusOutlined } from '@ant-design/icons';
import {
  Badge,
  Button,
  Card,
  Drawer,
  Form,
  Input,
  InputNumber,
  List,
  Modal,
  Row,
  Col,
  Select,
  Space,
  Tag,
  Typography,
  message
} from 'antd';
import { MapContainer, Polyline, TileLayer } from 'react-leaflet';
import type { LeafletMouseEvent } from 'leaflet';
import { useMemo, useState } from 'react';
import { missionList as initialMissionList, type Mission } from '../data/mock';

function MissionCommander() {
  const [missions, setMissions] = useState<Mission[]>(initialMissionList);
  const runningMissions = useMemo(
    () => missions.filter(m => m.status !== '完成'),
    [missions]
  );
  const [selectedMissionIds, setSelectedMissionIds] = useState<string[]>(
    runningMissions.map(m => m.id).slice(0, 2)
  );
  const [monitoringMission, setMonitoringMission] = useState<Mission | null>(null);
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [form] = Form.useForm<Omit<Mission, 'route' | 'color' | 'milestones'>>();

  const selectedMissions = useMemo(
    () => missions.filter(mission => selectedMissionIds.includes(mission.id)),
    [missions, selectedMissionIds]
  );

  const handleLineClick = (mission: Mission) => () => {
    setMonitoringMission(mission);
  };

  const renderStatusTag = (status: Mission['status']) => {
    if (status === '执行中') return <Tag color="green">执行中</Tag>;
    if (status === '排队') return <Tag color="orange">排队</Tag>;
    return <Tag color="blue">完成</Tag>;
  };

  const handleCreateMission = () => {
    form
      .validateFields()
      .then(values => {
        const randomOffset = Math.random() * 0.04;
        const baseLat = 31.2 + randomOffset;
        const baseLng = 121.4 + randomOffset;
        const newMission: Mission = {
          id: values.id || `M-${Date.now()}`,
          name: values.name,
          type: values.type,
          pilot: values.pilot,
          status: values.status,
          priority: values.priority,
          progress: values.progress ?? 0,
          color: values.status === '执行中' ? '#f97316' : '#22c55e',
          route: [
            [baseLat, baseLng],
            [baseLat + 0.05, baseLng],
            [baseLat + 0.02, baseLng + 0.04],
            [baseLat, baseLng]
          ],
          milestones: values.status === '执行中' ? ['起飞 (模拟)', '航线执行中'] : ['排队中']
        };

        setMissions(prev => [newMission, ...prev]);
        setSelectedMissionIds(prev => [...prev, newMission.id]);
        message.success('已新增任务');
        setCreateModalOpen(false);
        form.resetFields();
      })
      .catch(() => undefined);
  };

  const layoutHeight = 'calc(100vh - 220px)';

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <Row gutter={[16, 16]} style={{ minHeight: layoutHeight }}>
        <Col xs={24} lg={7} style={{ height: layoutHeight }}>
          <Card
            title="任务列表"
            style={{ height: '100%' }}
            bodyStyle={{ display: 'flex', flexDirection: 'column', height: '100%', paddingBottom: 0 }}
            extra={
              <Button icon={<PlusOutlined />} type="primary" onClick={() => setCreateModalOpen(true)}>
                新增任务
              </Button>
            }
          >
            <List
              style={{ flex: 1, overflow: 'auto' }}
              dataSource={missions}
              renderItem={mission => (
                <List.Item
                  key={mission.id}
                  style={{ cursor: 'pointer' }}
                  onClick={() => setMonitoringMission(mission)}
                  actions={[renderStatusTag(mission.status)]}
                >
                  <List.Item.Meta
                    title={
                      <Space>
                        <EnvironmentOutlined />
                        {mission.name}
                      </Space>
                    }
                    description={
                      <Space split={<span>·</span>} wrap>
                        <span>类型：{mission.type}</span>
                        <span>优先级：{mission.priority}</span>
                        <span>进度：{mission.progress}%</span>
                      </Space>
                    }
                  />
                </List.Item>
              )}
            />
          </Card>
        </Col>
        <Col xs={24} lg={17} style={{ height: layoutHeight }}>
          <Card
            style={{ height: '100%' }}
            bodyStyle={{ display: 'flex', flexDirection: 'column', height: '100%' }}
          >
            <Space direction="vertical" style={{ width: '100%' }}>
              <Typography.Title level={4} style={{ margin: 0 }}>
                任务航线图
              </Typography.Title>
              <Space wrap>
                <Typography.Text>选择任务：</Typography.Text>
                <Select
                  mode="multiple"
                  value={selectedMissionIds}
                  placeholder="勾选正在执行的任务航线"
                  style={{ minWidth: 260 }}
                  onChange={setSelectedMissionIds}
                  options={runningMissions.map(m => ({
                    label: `${m.name}（${m.status}）`,
                    value: m.id
                  }))}
                  maxTagCount="responsive"
                />
              </Space>
            </Space>
            <div style={{ marginTop: 16, flex: 1 }}>
              <MapContainer
                center={[31.25, 121.45]}
                zoom={11}
                style={{ height: '100%', borderRadius: 12, overflow: 'hidden' }}
              >
                <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
                {selectedMissions.map(mission => (
                  <Polyline
                    key={mission.id}
                    pathOptions={{ color: mission.color, weight: 4 }}
                    positions={mission.route}
                    eventHandlers={{
                      click: handleLineClick(mission),
                      mouseover: (e: LeafletMouseEvent) => {
                        e.target.setStyle({ weight: 6 });
                      },
                      mouseout: (e: LeafletMouseEvent) => {
                        e.target.setStyle({ weight: 4 });
                      }
                    }}
                  />
                ))}
              </MapContainer>
            </div>
          </Card>
        </Col>
      </Row>

      <Drawer
        title={monitoringMission ? `${monitoringMission.name} · 执行监控` : ''}
        open={!!monitoringMission}
        onClose={() => setMonitoringMission(null)}
        width={420}
      >
        {monitoringMission ? (
          <Space direction="vertical" style={{ width: '100%' }}>
            <Badge status="processing" text={`状态：${monitoringMission.status}`} />
            <Badge status="default" text={`责任人：${monitoringMission.pilot}`} />
            <Badge status="default" text={`优先级：${monitoringMission.priority}`} />
            <Typography.Title level={5}>进度</Typography.Title>
            <Typography.Paragraph>
              当前完成 {monitoringMission.progress}% ，剩余航点{' '}
              {Math.max(0, 100 - monitoringMission.progress)}%。
            </Typography.Paragraph>
            <Typography.Title level={5}>里程碑</Typography.Title>
            <List
              dataSource={monitoringMission.milestones}
              renderItem={item => (
                <List.Item>
                  <List.Item.Meta description={item} />
                </List.Item>
              )}
            />
          </Space>
        ) : null}
      </Drawer>

      <Modal
        title="新增任务"
        open={createModalOpen}
        onCancel={() => {
          setCreateModalOpen(false);
          form.resetFields();
        }}
        onOk={handleCreateMission}
        okText="创建"
      >
        <Form form={form} layout="vertical">
          <Form.Item name="id" label="任务编号">
            <Input placeholder="自动生成可留空" />
          </Form.Item>
          <Form.Item
            name="name"
            label="任务名称"
            rules={[{ required: true, message: '请输入任务名称' }]}
          >
            <Input placeholder="如 森林巡查-南区" />
          </Form.Item>
          <Form.Item
            name="type"
            label="任务类型"
            rules={[{ required: true, message: '请输入任务类型' }]}
          >
            <Input placeholder="林业健康 / 火情巡查 / 空气质量等" />
          </Form.Item>
          <Form.Item
            name="pilot"
            label="责任人"
            rules={[{ required: true, message: '请输入责任人' }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="priority"
            label="优先级"
            rules={[{ required: true, message: '请选择优先级' }]}
          >
            <Select
              options={[
                { value: '高', label: '高' },
                { value: '中', label: '中' },
                { value: '低', label: '低' }
              ]}
            />
          </Form.Item>
          <Form.Item
            name="status"
            label="任务状态"
            initialValue="排队"
            rules={[{ required: true, message: '请选择状态' }]}
          >
            <Select
              options={[
                { value: '执行中', label: '执行中' },
                { value: '排队', label: '排队' },
                { value: '完成', label: '完成' }
              ]}
            />
          </Form.Item>
          <Form.Item name="progress" label="初始进度 (%)" initialValue={0}>
            <InputNumber min={0} max={100} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

export default MissionCommander;
