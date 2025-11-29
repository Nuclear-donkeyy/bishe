import { EnvironmentOutlined, PlusOutlined } from '@ant-design/icons';
import {
  Badge,
  Button,
  Card,
  Descriptions,
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
import { CircleMarker, MapContainer, Polyline, TileLayer, useMapEvents, Marker, Tooltip } from 'react-leaflet';
import type { LatLngTuple, LeafletMouseEvent, Map as LeafletMap, DivIcon } from 'leaflet';
import L from 'leaflet';
import { useEffect, useMemo, useRef, useState } from 'react';
import 'leaflet/dist/leaflet.css';
import {
  fleetApi,
  missionApi,
  userApi,
  type MissionDto,
  type MissionTypeDefinition,
  type UserRow,
  type UavDevice,
  type MissionStatusPayload
} from '../services/api';
import { connectTelemetrySocket } from '../services/ws';

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
  const [selectedMissionIds, setSelectedMissionIds] = useState<number[]>([]);
  const [monitoringMission, setMonitoringMission] = useState<MissionDto | null>(null);
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [routeModalOpen, setRouteModalOpen] = useState(false);
  const [form] = Form.useForm<{
    name: string;
    missionType: string;
    pilotUsername: string;
    priority: string;
    assignedUav?: string;
  }>();
  const [routeDraft, setRouteDraft] = useState<LatLngTuple[]>([]);
  const routeMapRef = useRef<LeafletMap | null>(null);
  const mainMapRef = useRef<LeafletMap | null>(null);
  const [mainMapCenter, setMainMapCenter] = useState<LatLngTuple>([31.25, 121.45]);
  const [routeMapCenter, setRouteMapCenter] = useState<LatLngTuple>([31.25, 121.45]);
  const [recenterLat, setRecenterLat] = useState<string>('');
  const [recenterLng, setRecenterLng] = useState<string>('');
  const [missionTypes, setMissionTypes] = useState<MissionTypeDefinition[]>([]);
  const [availableUavs, setAvailableUavs] = useState<UavDevice[]>([]);
  const [users, setUsers] = useState<UserRow[]>([]);
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [nameFilter, setNameFilter] = useState<string>('');
  const missionWsRef = useRef<WebSocket | null>(null);
  const [uavTelemetry, setUavTelemetry] = useState<
    Record<string, { missionId?: string; lat?: number; lng?: number }>
  >({});
  const uavIcon = useMemo<DivIcon>(
    () =>
      L.divIcon({
        className: '',
        html: `<div style="width:32px;height:32px;display:flex;align-items:center;justify-content:center;border-radius:10px;background:#0f172a;box-shadow:0 4px 10px rgba(0,0,0,0.25);padding:4px;">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="12" cy="12" r="3.2" stroke="#7dd3fc" stroke-width="1.5" fill="#0ea5e9"/>
            <path d="M4 6h3.5M20 6h-3.5M4 18h3.5M20 18h-3.5" stroke="#22d3ee" stroke-width="1.4" stroke-linecap="round"/>
            <path d="M4.8 6.8 9 11M19.2 6.8 15 11M4.8 17.2 9 13M19.2 17.2 15 13" stroke="#a5b4fc" stroke-width="1.3" stroke-linecap="round"/>
          </svg>
        </div>`,
        iconSize: [32, 32],
        iconAnchor: [16, 16],
        tooltipAnchor: [0, -14]
      }),
    []
  );

  useEffect(() => {
    missionApi.list().then(setMissions).catch(() => setMissions([]));
    missionApi.types().then(setMissionTypes).catch(() => setMissionTypes([]));
    fleetApi.available().then(setAvailableUavs).catch(() => setAvailableUavs([]));
    userApi.list().then(setUsers).catch(() => setUsers([]));
    const telemetryClient = connectTelemetrySocket({
      onMessage: payload => {
        if (!payload || !payload.uavCode) return;
        const key = payload.uavCode as string;
        setUavTelemetry(prev => ({
          ...prev,
          [key]: {
            missionId: payload.missionId || payload.missionCode,
            lat: payload.lat,
            lng: payload.lng
          }
        }));
      }
    });
    // 获取浏览器当前位置作为默认中心
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        pos => {
          const next: LatLngTuple = [pos.coords.latitude, pos.coords.longitude];
          setMainMapCenter(next);
          setRouteMapCenter(next);
          setRecenterLat(String(next[0]));
          setRecenterLng(String(next[1]));
          routeMapRef.current?.setView(next);
        },
        () => void 0,
        { enableHighAccuracy: true, timeout: 3000 }
      );
    }
    return () => {
      telemetryClient.deactivate();
    };
  }, []);

  useEffect(() => {
    const WS_URL = 'ws://localhost:8080/ws/uav-telemetry';
    const socket = new WebSocket(WS_URL);
    missionWsRef.current = socket;

    const sendFrame = (command: string, headers: Record<string, string>) => {
      const lines = [command];
      Object.entries(headers).forEach(([k, v]) => lines.push(`${k}:${v}`));
      lines.push('', '');
      socket.send(`${lines.join('\n')}\u0000`);
    };

    socket.onopen = () => {
      sendFrame('CONNECT', { 'accept-version': '1.2', 'heart-beat': '10000,10000' });
    };

    socket.onmessage = event => {
      const raw = String(event.data);
      const frames = raw.split('\u0000').filter(Boolean);
      frames.forEach(frame => {
        const lines = frame.split('\n');
        const command = lines.shift() || '';
        const headers: Record<string, string> = {};
        while (lines.length) {
          const line = lines.shift();
          if (line === '') break;
          const [k, ...rest] = (line || '').split(':');
          headers[k] = rest.join(':');
        }
        const body = lines.join('\n');
        if (command === 'CONNECTED') {
          sendFrame('SUBSCRIBE', { id: 'mission-updates', destination: '/topic/mission-updates', ack: 'auto' });
        }
        if (command === 'MESSAGE') {
          try {
            const payload = JSON.parse(body) as MissionStatusPayload;
            setMissions(prev =>
              prev.map(m =>
                m.missionCode === payload.missionCode ? { ...m, status: payload.status } : m
              )
            );
          } catch {
            // ignore
          }
        }
      });
    };

    return () => {
      socket.close();
    };
  }, []);
  const filteredMissions = useMemo(
    () =>
      missions.filter(m => {
        const statusOk = statusFilter === 'ALL' || (m.status || '').toUpperCase() === statusFilter;
        const nameOk = !nameFilter || (m.name || '').toLowerCase().includes(nameFilter.toLowerCase());
        return statusOk && nameOk;
      }),
    [missions, statusFilter, nameFilter]
  );
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

  // ensure Leaflet map resizes correctly when modal opens
  useEffect(() => {
    if (routeModalOpen && routeMapRef.current) {
      setTimeout(() => routeMapRef.current?.invalidateSize(), 100);
    }
  }, [routeModalOpen]);

  const selectedMissions = useMemo(
    () => missions.filter(mission => selectedMissionIds.includes(mission.id)),
    [missions, selectedMissionIds]
  );

  useEffect(() => {
    if (selectedMissions.length && selectedMissions[0].route?.length) {
      const [lat, lng] = selectedMissions[0].route[0];
      setMainMapCenter([lat, lng]);
      mainMapRef.current?.setView([lat, lng]);
    }
  }, [selectedMissions]);

  const distinctStatuses = useMemo(
    () => Array.from(new Set(missions.map(m => m.status).filter(Boolean))),
    [missions]
  );

  

  const handleLineClick = (mission: MissionDto) => () => {
    setMonitoringMission(mission);
  };

  const renderStatusTag = (status: MissionDto['status']) => {
    switch ((status || '').toUpperCase()) {
      case 'QUEUE':
        return <Tag color="blue">排队</Tag>;
      case 'RUNNING':
        return <Tag color="green">运行中</Tag>;
      case 'COMPLETED':
        return <Tag color="default">已完成</Tag>;
      case 'INTERRUPTED':
        return <Tag color="red">已中断</Tag>;
      default:
        return <Tag color="default">{status || '??'}</Tag>;
    }
  };

  const handleRecenter = () => {
    const lat = parseFloat(recenterLat);
    const lng = parseFloat(recenterLng);
    if (Number.isNaN(lat) || Number.isNaN(lng)) {
      message.warning('请输入有效的经纬度');
      return;
    }
    const next: LatLngTuple = [lat, lng];
    setRouteMapCenter(next);
    routeMapRef.current?.setView(next);
  };

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
            assignedUavs: values.assignedUav ? [values.assignedUav] : []
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
      content: '任务将标记为已中断，无法继续执行。',
      okText: '确认中断',
      okType: 'danger',
      cancelText: '取消',
      onOk: () => {
        missionApi
          .interrupt(mission.missionCode)
          .then(() => {
            setMissions(prev =>
              prev.map(item =>
                item.id == mission.id ? { ...item, status: 'INTERRUPTED', progress: 0 } : item
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
            <Space direction="horizontal" wrap style={{ width: '100%', marginBottom: 12 }}>
              <Select
                style={{ minWidth: 180 }}
                value={statusFilter}
                onChange={setStatusFilter}
                options={[
                  { label: '全部', value: 'ALL' },
                  { label: '排队', value: 'QUEUE' },
                  { label: '进行中', value: 'RUNNING' },
                  { label: '已结束', value: 'COMPLETED' },
                  { label: '已中断', value: 'INTERRUPTED' }
                ]}
                placeholder="按状态筛选"
              />
              <Input
                placeholder="按名称筛选"
                value={nameFilter}
                onChange={e => setNameFilter(e.target.value)}
                allowClear
                style={{ minWidth: 220 }}
              />
            </Space>
            <List
              style={{ flex: 1, overflow: 'auto' }}
              dataSource={filteredMissions}
              renderItem={mission => (
                <List.Item
                  key={mission.id}
                  style={{ cursor: 'pointer', alignItems: 'flex-start' }}
                  onClick={() => {
                    setMonitoringMission(mission);
                    setSelectedMissionIds([mission.id]);
                  }}
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
                  placeholder="选择任务航线"
                  style={{ minWidth: 260 }}
                  onChange={ids => {
                    setSelectedMissionIds(ids);
                    const first = missions.find(m => m.id === ids[0]);
                    if (first?.route?.length) {
                      const [lat, lng] = first.route[0];
                      setMainMapCenter([lat, lng]);
                    }
                  }}
                  options={filteredMissions.map(m => ({
                    label: `${m.name}（${m.status ?? '未知'}）`,
                    value: m.id
                  }))}
                  maxTagCount="responsive"
                />
              </Space>
            </Space>
            <div style={{ marginTop: 16, flex: 1 }}>
              <MapContainer
                ref={mainMapRef}
                center={mainMapCenter}
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
                {selectedMissions.flatMap(mission => {
                  if ((mission.status || '').toUpperCase() !== 'RUNNING') return [];
                  const missionKey = mission.missionCode || mission.id?.toString() || '';
                  if (!missionKey) return [];
                  return Object.entries(uavTelemetry).flatMap(([code, t]) => {
                    if (t.lat == null || t.lng == null) return [];
                    const mid = (t.missionId || '').toString();
                    if (mid === missionKey) {
                      return (
                        <Marker key={`${mission.id}-${code}`} position={[t.lat, t.lng]} icon={uavIcon}>
                          <Tooltip direction="top" offset={[0, -10]} opacity={0.9}>
                            <span>{`UAV ${code}`}</span>
                          </Tooltip>
                        </Marker>
                      );
                    }
                    return [];
                  });
                })}
              </MapContainer>
            </div>
          </Card>
        </Col>
      </Row>

      <Drawer
        title={monitoringMission ? `${monitoringMission.name} - Details` : ''}
        open={!!monitoringMission}
        onClose={() => setMonitoringMission(null)}
        width={420}
      >
        {monitoringMission ? (
          <Space direction="vertical" style={{ width: '100%' }}>
            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label="Name">{monitoringMission.name}</Descriptions.Item>
              <Descriptions.Item label="Status">{renderStatusTag(monitoringMission.status)}</Descriptions.Item>
              <Descriptions.Item label="Type">{monitoringMission.missionType}</Descriptions.Item>
              <Descriptions.Item label="Pilot">{monitoringMission.pilotName}</Descriptions.Item>
              <Descriptions.Item label="Priority">{monitoringMission.priority}</Descriptions.Item>
              <Descriptions.Item label="Code">{monitoringMission.missionCode}</Descriptions.Item>
              <Descriptions.Item label="UAVs">
                {monitoringMission.assignedUavs?.length
                  ? monitoringMission.assignedUavs.join(', ')
                  : 'Not assigned'}
              </Descriptions.Item>
            </Descriptions>
            {['QUEUE', 'RUNNING'].includes((monitoringMission.status || '').toUpperCase()) ? (
              <Button danger onClick={() => handleInterruptMission(monitoringMission)}>中断任务</Button>
            ) : null}
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
          <Form.Item name="assignedUav" label="执行无人机" tooltip="仅显示在线且未占用的无人机">
            <Select
              placeholder={availableUavOptions.length ? '请选择无人机' : '暂无可用无人机'}
              options={availableUavOptions}
              disabled={!availableUavOptions.length}
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
        <Space style={{ marginBottom: 12 }} wrap>
          <Input
            style={{ width: 160 }}
            placeholder="纬度"
            value={recenterLat}
            onChange={e => setRecenterLat(e.target.value)}
          />
          <Input
            style={{ width: 160 }}
            placeholder="经度"
            value={recenterLng}
            onChange={e => setRecenterLng(e.target.value)}
          />
          <Button onClick={handleRecenter}>定位</Button>
        </Space>
        <div style={{ height: 360, borderRadius: 8, overflow: 'hidden', marginBottom: 12 }}>
          <MapContainer
            center={routeMapCenter}
            zoom={11}
            style={{ height: '100%', width: '100%' }}
            attributionControl={false}
            whenReady={() => {
              setTimeout(() => routeMapRef.current?.invalidateSize(), 50);
            }}
            ref={routeMapRef as any}
            key={routeModalOpen ? 'open' : 'closed'}
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
