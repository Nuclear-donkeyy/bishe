import { EnvironmentOutlined, PlusOutlined } from '@ant-design/icons';
import {
  Badge,
  Button,
  Card,
  Checkbox,
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
import {
  fleetItems,
  missionList as initialMissionList,
  missionTypeDefinitions,
  type Mission,
  type MissionTypeKey
} from '../data/mock';

function MissionCommander() {
  const [missions, setMissions] = useState<Mission[]>(initialMissionList);
  const runningMissions = useMemo(
    () => missions.filter(m => m.status === '执行中'),
    [missions]
  );
  const [selectedMissionIds, setSelectedMissionIds] = useState<string[]>(() =>
    runningMissions.map(m => m.id).slice(0, 2)
  );
  const [monitoringMission, setMonitoringMission] = useState<Mission | null>(null);
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [form] = Form.useForm<
    Omit<Mission, 'route' | 'color' | 'milestones' | 'metrics'> & { metrics: string[] }
  >();
  const selectedType = Form.useWatch('missionType', form);
  const availableUavs = useMemo(() => {
    const busyUavIds = new Set<string>();
    missions.forEach(mission => {
      mission.assignedUavs?.forEach(id => busyUavIds.add(id));
    });
    const executingMissionNames = new Set(
      missions.filter(m => m.status === '执行中').map(m => m.name)
    );
    return fleetItems.filter(item => {
      if (item.status !== 'online') return false;
      if (busyUavIds.has(item.id)) return false;
      if (executingMissionNames.has(item.mission)) return false;
      return true;
    });
  }, [missions]);
  const availableUavOptions = useMemo(
    () =>
      availableUavs.map(item => ({
        value: item.id,
        label: `${item.id} · ${item.model} · ${item.pilot} · 剩余 ${item.battery}%`
      })),
    [availableUavs]
  );

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
    if (status === '异常中止') return <Tag color="red">异常中止</Tag>;
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
          missionType: values.missionType,
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
          milestones: values.status === '执行中' ? ['起飞 (模拟)', '航线执行中'] : ['排队中'],
          metrics:
            values.metrics && values.metrics.length > 0
              ? values.metrics
              : missionTypeDefinitions[values.missionType].metrics.map(metric => metric.label),
          assignedUavs: values.assignedUavs && values.assignedUavs.length > 0 ? values.assignedUavs : undefined
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

  const handleInterruptMission = (mission: Mission) => {
    if (mission.status !== '执行中') return;
    Modal.confirm({
      title: `确认中断任务「${mission.name}」?`,
      content: '任务将标记为异常中止，无法继续执行。',
      okText: '确认中断',
      okType: 'danger',
      cancelText: '取消',
      onOk: () => {
        setMissions(prev =>
          prev.map(item =>
            item.id === mission.id ? { ...item, status: '异常中止' } : item
          )
        );
        setSelectedMissionIds(prev => prev.filter(id => id !== mission.id));
        message.success('任务已中断');
      }
    });
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <Row gutter={[16, 16]} style={{ minHeight: layoutHeight }}>
        <Col xs={24} lg={7} style={{ height: layoutHeight }}>
          <Card
            title="任务列表"
            style={{ height: '100%', overflow:'scroll' }}
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
                  style={{ cursor: 'pointer', alignItems: 'flex-start' }}
                  onClick={() => setMonitoringMission(mission)}
                  actions={[renderStatusTag(mission.status)]}
                >
                  <List.Item.Meta
                    style={{ width: '100%' }}
                    title={
                      <Space>
                        <EnvironmentOutlined />
                        {mission.name}
                      </Space>
                    }
                    description={
                      <Space direction="vertical" size={0}>
                        <Space split={<span>·</span>} wrap>
                          <span>类型：{mission.missionType}</span>
                          <span>优先级：{mission.priority}</span>
                          <span>进度：{mission.progress}%</span>
                        </Space>
                        <Space wrap style={{ marginTop: 4 }}>
                          {mission.metrics.map(metric => (
                            <Tag key={metric}>{metric}</Tag>
                          ))}
                        </Space>
                        {mission.assignedUavs?.length ? (
                          <Typography.Text type="secondary">
                            执行无人机：{mission.assignedUavs.join('、')}
                          </Typography.Text>
                        ) : null}
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
            <Badge status="default" text={`任务类型：${monitoringMission.missionType}`} />
            {monitoringMission.status === '执行中' ? (
              <Button danger onClick={() => handleInterruptMission(monitoringMission)}>
                中断任务
              </Button>
            ) : null}
            <Typography.Title level={5}>进度</Typography.Title>
            <Typography.Paragraph>
              当前完成 {monitoringMission.progress}% ，剩余航点{' '}
              {Math.max(0, 100 - monitoringMission.progress)}%。
            </Typography.Paragraph>
            <Typography.Title level={5}>关键指标</Typography.Title>
            <Space wrap>
              {monitoringMission.metrics.map(metric => (
                <Tag key={metric}>{metric}</Tag>
              ))}
            </Space>
            <Typography.Title level={5}>执行无人机</Typography.Title>
            {monitoringMission.assignedUavs?.length ? (
              <Space wrap>
                {monitoringMission.assignedUavs.map(uavId => (
                  <Tag key={uavId}>{uavId}</Tag>
                ))}
              </Space>
            ) : (
              <Typography.Text type="secondary">尚未分配无人机</Typography.Text>
            )}
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
        <Form form={form} layout="vertical" initialValues={{ missionType: '森林火情巡查' }}>
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
            name="missionType"
            label="任务类型"
            rules={[{ required: true, message: '请选择任务类型' }]}
          >
            <Select<MissionTypeKey>
              options={(Object.keys(missionTypeDefinitions) as MissionTypeKey[]).map(key => ({
                value: key,
                label: key
              }))}
            />
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
          <Form.Item
            name="assignedUavs"
            label="执行无人机"
            rules={
              availableUavOptions.length
                ? [{ required: true, message: '请选择执行无人机' }]
                : undefined
            }
            extra={
              availableUavOptions.length
                ? '仅显示在线且未被任务占用的无人机，可多选。'
                : '暂无空闲无人机，请前往机队中心释放或接入无人机。'
            }
          >
            <Select
              mode="multiple"
              placeholder={availableUavOptions.length ? '请选择可用无人机' : '暂无可用无人机'}
              options={availableUavOptions}
              disabled={!availableUavOptions.length}
              maxTagCount="responsive"
            />
          </Form.Item>
          <Form.Item name="progress" label="初始进度 (%)" initialValue={0}>
            <InputNumber min={0} max={100} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="metrics" label="关注指标">
            <Checkbox.Group
              options={
                selectedType
                  ? missionTypeDefinitions[selectedType as MissionTypeKey].metrics.map(metric => ({
                      label: metric.label,
                      value: metric.label
                    }))
                  : []
              }
            />
            {!selectedType ? (
              <Typography.Text type="secondary">请选择任务类型以加载指标</Typography.Text>
            ) : null}
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

export default MissionCommander;
