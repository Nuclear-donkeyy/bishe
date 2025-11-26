import { EnvironmentOutlined, PlusOutlined } from '@ant-design/icons';
import {
  Badge,
  Button,
  Card,
  Drawer,
  Form,
  Input,
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
import { CircleMarker, MapContainer, Polyline, TileLayer, useMapEvents } from 'react-leaflet';
import type { LatLngTuple, LeafletMouseEvent } from 'leaflet';
import { useEffect, useMemo, useState } from 'react';
import {
  fleetApi,
  missionApi,
  userApi,
  type MissionDto,
  type MissionTypeDefinition,
  type UserRow,
  type UavDevice
} from '../services/api';

const pointsEqual = (a: LatLngTuple, b: LatLngTuple) =>
  Math.abs(a[0] - b[0]) < 1e-6 && Math.abs(a[1] - b[1]) < 1e-6;

function RouteClickHandler({ onAddPoint }: { onAddPoint: (point: LatLngTuple) => void }) {
  useMapEvents({
    click: event => {
      onAddPoint([event.latlng.lat, event.latlng.lng]);
    }
  });
  return null;
}

function MissionCommander() {
  const [missions, setMissions] = useState<MissionDto[]>([]);
  const runningMissions = useMemo(
    () => missions.filter(m => m.status?.includes('执') || m.status === '执行中'),
    [missions]
  );
  const [selectedMissionIds, setSelectedMissionIds] = useState<number[]>([]);
  const [monitoringMission, setMonitoringMission] = useState<MissionDto | null>(null);
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [routeModalOpen, setRouteModalOpen] = useState(false);
  const [form] = Form.useForm<{
    name: string;
    missionType: string;
    pilotUsername: string;
    priority: string;
    assignedUavs?: string[];
  }>();
  const [routeDraft, setRouteDraft] = useState<LatLngTuple[]>([]);
  const [missionTypes, setMissionTypes] = useState<MissionTypeDefinition[]>([]);
  const [availableUavs, setAvailableUavs] = useState<UavDevice[]>([]);
  const [users, setUsers] = useState<UserRow[]>([]);

  useEffect(() => {
    missionApi.list().then(setMissions).catch(() => setMissions([]));
    missionApi.types().then(setMissionTypes).catch(() => setMissionTypes([]));
    fleetApi.available().then(setAvailableUavs).catch(() => setAvailableUavs([]));
    userApi.list().then(setUsers).catch(() => setUsers([]));
  }, []);

  useEffect(() => {
    if (runningMissions.length && !selectedMissionIds.length) {
      setSelectedMissionIds(runningMissions.map(m => m.id).slice(0, 2));
    }
  }, [runningMissions, selectedMissionIds.length]);

  const availableUavOptions = useMemo(
    () =>
      availableUavs.map(item => ({
        value: item.uavCode,
        label: `${item.uavCode} · ${item.model} · ${item.pilotName ?? ''}`
      })),
    [availableUavs]
  );

  const pilotOptions = useMemo(() => {
    const isOperator = (role?: string) => (role || '').toLowerCase() == 'operator';
    const operators = users.filter(u => isOperator(u.role));
    const base = operators.length ? operators : users;
    return base.map(u => ({
      value: u.username,
      label: `${u.name} (${u.role})`
    }));
  }, [users]);

  const routeIsClosed =
    routeDraft.length > 2 && pointsEqual(routeDraft[0], routeDraft[routeDraft.length - 1]);

  const handleRoutePointAdd = (point: LatLngTuple) => {
    setRouteDraft(prev => [...prev, point]);
  };

  const handleRouteUndo = () => {
    setRouteDraft(prev => prev.slice(0, -1));
  };

  const handleRouteClear = () => {
    setRouteDraft([]);
  };

  const handleRouteClose = () => {
    setRouteDraft(prev => {
      if (prev.length < 3) return prev;
      const lastPoint = prev[prev.length - 1];
      if (pointsEqual(prev[0], lastPoint)) {
        return prev;
      }
      return [...prev, prev[0]];
    });
  };

  const selectedMissions = useMemo(
    () => missions.filter(mission => selectedMissionIds.includes(mission.id)),
    [missions, selectedMissionIds]
  );

  const handleLineClick = (mission: MissionDto) => () => {
    setMonitoringMission(mission);
  };

  const renderStatusTag = (status: MissionDto['status']) => {
    if (status?.includes('执')) return <Tag color="green">{status}</Tag>;
    if (status?.includes('队')) return <Tag color="orange">{status}</Tag>;
    if (status?.includes('异常')) return <Tag color="red">{status}</Tag>;
    return <Tag color="blue">{status}</Tag>;
  };

  const creationMapCenter: LatLngTuple = [31.25, 121.45];

  const handleCreateMission = () => {
    if (routeDraft.length < 3) {
      message.error('请在航线规划中选择至少 3 个航点并闭合航线');
      return;
    }
    form
      .validateFields()
      .then(values => {
        const normalizedRoute = routeIsClosed ? routeDraft : [...routeDraft, routeDraft[0]];
        return missionApi
          .create({
            name: values.name,
            missionType: values.missionType,
            pilotUsername: values.pilotUsername,
            priority: values.priority,
            route: normalizedRoute.map(p => [p[0], p[1]]),
            milestones: [],
            assignedUavs: values.assignedUavs
          })
          .then(created => {
            setMissions(prev => [created, ...prev]);
            setSelectedMissionIds(prev => [...prev, created.id]);
            message.success('已新增任务');
            setCreateModalOpen(false);
            form.resetFields();
            setRouteDraft([]);
          })
          .catch(err => message.error(err.message || '创建任务失败'));
      })
      .catch(() => undefined);
  };

  const layoutHeight = 'calc(100vh - 220px)';

  const handleInterruptMission = (mission: MissionDto) => {
    if (!mission.missionCode) return;
    Modal.confirm({
      title: `确认中断任务「${mission.name}」？`,
      content: '任务将标记为异常中止，无法继续执行。',
      okText: '确认中断',
      okType: 'danger',
      cancelText: '取消',
      onOk: () => {
        missionApi
          .interrupt(mission.missionCode)
          .then(() => {
            setMissions(prev =>
              prev.map(item =>
                item.id == mission.id ? { ...item, status: '异常中止', progress: 0 } : item
              )
            );
            setSelectedMissionIds(prev => prev.filter(id => id !== mission.id));
            message.success('任务已中断');
          })
          .catch(() => message.error('中断失败'));
      }
    });
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <Row gutter={[16, 16]} style={{ minHeight: layoutHeight }}>
        <Col xs={24} lg={7} style={{ height: layoutHeight }}>
          <Card
            title="任务列表"
            style={{ height: '100%', overflow: 'scroll' }}
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
                          <span>进度：{mission.progress ?? 0}%</span>
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
                  placeholder="选择正在执行的任务航线"
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
                    pathOptions={{ color: mission.colorHex ?? '#2563eb', weight: 4 }}
                    positions={mission.route as LatLngTuple[]}
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
            <Badge status="default" text={`负责人：${monitoringMission.pilotName}`} />
            <Badge status="default" text={`优先级：${monitoringMission.priority}`} />
            <Badge status="default" text={`任务类型：${monitoringMission.missionType}`} />
            {monitoringMission.status?.includes('执') ? (
              <Button danger onClick={() => handleInterruptMission(monitoringMission)}>
                中断任务
              </Button>
            ) : null}
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
              dataSource={monitoringMission.milestones ?? []}
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
          setRouteDraft([]);
        }}
        onOk={handleCreateMission}
        okText="创建"
        width={700}
      >
        <Form
          form={form}
          layout="vertical"
          initialValues={{ missionType: missionTypes[0]?.displayName, priority: '中' }}
        >
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
            <Select
              options={missionTypes.map(mt => ({
                value: mt.displayName,
                label: mt.displayName
              }))}
              placeholder="选择任务类型"
            />
          </Form.Item>
          <Form.Item
            name="pilotUsername"
            label="责任人"
            rules={[{ required: true, message: '请选择责任人' }]}
          >
            <Select options={pilotOptions} placeholder="选择责任人" />
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
          <Form.Item name="assignedUavs" label="执行无人机" tooltip="仅显示在线且未占用的无人机">
            <Select
              mode="multiple"
              placeholder={availableUavOptions.length ? '请选择无人机' : '暂无可用无人机'}
              options={availableUavOptions}
              disabled={!availableUavOptions.length}
              maxTagCount="responsive"
            />
          </Form.Item>
          <Form.Item label="航线规划">
            <Button onClick={() => setRouteModalOpen(true)}>打开航线规划</Button>
            <Typography.Text type="secondary" style={{ marginLeft: 8 }}>
              当前航点 {routeDraft.length} 个
            </Typography.Text>
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="航线规划"
        open={routeModalOpen}
        onCancel={() => setRouteModalOpen(false)}
        onOk={() => setRouteModalOpen(false)}
        okText="确认航线"
        width={820}
      >
        <Typography.Paragraph type="secondary" style={{ marginBottom: 8 }}>
          点击地图添加航点，至少 3 个；可“闭合航线”使首尾相连。
        </Typography.Paragraph>
        <div style={{ height: 360, borderRadius: 8, overflow: 'hidden', marginBottom: 12 }}>
          <MapContainer
            center={creationMapCenter}
            zoom={11}
            style={{ height: '100%', width: '100%' }}
            attributionControl={false}
          >
            <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
            <RouteClickHandler onAddPoint={handleRoutePointAdd} />
            {routeDraft.length ? (
              <Polyline
                positions={routeDraft}
                pathOptions={{ color: routeIsClosed ? '#22c55e' : '#f97316', weight: 4 }}
              />
            ) : null}
            {routeDraft.map((point, index) => (
              <CircleMarker
                key={`${point[0]}-${point[1]}-${index}`}
                center={point}
                radius={index === 0 ? 6 : 4}
                pathOptions={{ color: index === 0 ? '#2563eb' : '#111827', weight: 2 }}
                fillOpacity={0.8}
              />
            ))}
          </MapContainer>
        </div>
        <Space wrap>
          <Button onClick={handleRouteClose} disabled={routeDraft.length < 3}>
            闭合航线
          </Button>
          <Button onClick={handleRouteUndo} disabled={!routeDraft.length}>
            撤销航点
          </Button>
          <Button onClick={handleRouteClear} disabled={!routeDraft.length}>
            清除航线
          </Button>
          <Typography.Text type="secondary">已选 {routeDraft.length} 个航点</Typography.Text>
        </Space>
      </Modal>
    </div>
  );
}

export default MissionCommander;
