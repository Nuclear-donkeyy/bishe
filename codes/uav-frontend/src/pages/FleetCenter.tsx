import { PlusOutlined } from '@ant-design/icons';
import {
  Badge,
  Button,
  Card,
  Col,
  Form,
  Input,
  Modal,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
  message
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useMemo, useRef, useState } from 'react';
import { fleetApi, configApi, userApi, type FleetSummary, type UavDevice } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { connectTelemetrySocket } from '../services/ws';

type RegisterForm = {
  uavCode: string;
  model: string;
  pilotUsername: string;
  sensors?: string[];
  metadata?: string;
};

type TelemetryMap = Record<
  string,
  {
    batteryPercent?: number;
    status?: string;
    lat?: number;
    lng?: number;
    alt?: number;
  }
>;

const statusTag = (status?: string) => {
  if (!status) return <Tag color="default">--</Tag>;
  if (status.toUpperCase() === 'CRITICAL') return <Tag color="red">离线</Tag>;
  if (status.toUpperCase() === 'WARNING') return <Tag color="orange">链路预警</Tag>;
  if (status.toUpperCase() === 'PENDING_CONNECT') return <Tag color="default">待接入</Tag>;
  return <Tag color="green">{status}</Tag>;
};

function FleetCenter() {
  const [fleet, setFleet] = useState<UavDevice[]>([]);
  const [summary, setSummary] = useState<FleetSummary | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [form] = Form.useForm<RegisterForm>();
  const { currentUser } = useAuth();
  const [sensorOptions, setSensorOptions] = useState<{ value: string; label: string }[]>([]);
  const [userOptions, setUserOptions] = useState<{ value: string; label: string }[]>([]);
  const telemetryRef = useRef<TelemetryMap>({});
  const [, forceRender] = useState(0);
  const [wsConnected, setWsConnected] = useState(false);

  const load = () => {
    fleetApi.list({ page: 1, pageSize: 200 }).then(res => setFleet(res.items)).catch(() => setFleet([]));
    fleetApi.summary().then(setSummary).catch(() => setSummary(null));
    configApi.sensors.list().then((list: any) => setSensorOptions((list as any[] || []).map((s: any) => ({ value: s.sensorCode, label: `${s.sensorCode}·${s.name}` })))).catch(() => setSensorOptions([]));
    userApi.list().then((users: any) => setUserOptions((users as any[] || []).map((u: any) => ({ value: u.username, label: `${u.name} (${u.username})` })))).catch(() => setUserOptions([]));
  };

  useEffect(() => {
    load();
    const client = connectTelemetrySocket({
      onMessage: payload => {
        if (!payload || !payload.uavCode) return;
        const { uavCode, batteryPercent, status, lat, lng, alt } = payload;
        telemetryRef.current[uavCode] = {
          batteryPercent,
          status,
          lat,
          lng,
          alt
        };
        setWsConnected(true);
        forceRender(x => x + 1);
      },
      onConnect: () => setWsConnected(true),
      onDisconnect: () => setWsConnected(false)
    });
    return () => client.deactivate();
  }, []);

  const visibleFleet = useMemo(() => {
    if (currentUser?.role === 'superadmin') return fleet;
    return fleet.filter(item => item.pilotName === currentUser?.name);
  }, [fleet, currentUser]);

  const connectionHealth = useMemo(
    () => ({
      avgBattery: 0,
      slowLinks: visibleFleet.filter(item => {
        const t = telemetryRef.current[item.uavCode];
        return t && t.status && t.status !== 'ONLINE';
      }).length
    }),
    [visibleFleet]
  );

  const columns: ColumnsType<UavDevice> = [
    {
      title: '无人机',
      dataIndex: 'uavCode',
      key: 'uavCode',
      render: (value, record) => (
        <Space direction="vertical" size={0}>
          <strong>{value}</strong>
          <span style={{ color: '#667085', fontSize: 12 }}>{record.model}</span>
        </Space>
      )
    },
    {
      title: '传感器',
      dataIndex: 'sensors',
      key: 'sensors',
      render: value => (value ? value.join('、') : '-')
    },
    {
      title: '飞行员',
      dataIndex: 'pilotName',
      key: 'pilotName'
    },
    {
      title: '电量',
      key: 'battery',
      render: (_, record) => {
        const t = telemetryRef.current[record.uavCode];
        if (!wsConnected) return '连接断开';
        return t?.batteryPercent != null ? `${t.batteryPercent}%` : '--';
      }
    },
    {
      title: '状态',
      key: 'status',
      render: (_, record) =>
        !wsConnected
          ? <Tag color="default">连接断开</Tag>
          : statusTag(telemetryRef.current[record.uavCode]?.status || record.status)
    },
    {
      title: '经纬度',
      key: 'location',
      render: (_, record) => {
        const t = telemetryRef.current[record.uavCode];
        if (!wsConnected) return '连接断开';
        return t && t.lat != null && t.lng != null ? `${t.lat}, ${t.lng}` : '--';
      }
    }
  ];

  const handleSubmit = () => {
    form
      .validateFields()
      .then(values => {
        const payload = {
          uavCode: values.uavCode,
          model: values.model,
          pilotUsername: values.pilotUsername,
          sensors: values.sensors,
          metadata: values.metadata
        };
        return fleetApi
          .register(payload)
          .then(newItem => {
            setFleet(prev => [newItem, ...prev]);
            message.success(`已接入 ${newItem.uavCode}`);
            setModalOpen(false);
            form.resetFields();
          })
          .catch(err => message.error(err.message || '接入失败'));
      })
      .catch(() => undefined);
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <Row gutter={[16, 16]}>
        <Col xs={24} md={6}>
          <Card>
            <Statistic title="可见无人机" value={visibleFleet.length} suffix="架" />
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card>
            <Statistic title="链路异常" value={connectionHealth.slowLinks} suffix="架" />
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card>
            <Statistic title="平均电量" value={connectionHealth.avgBattery} suffix="%" />
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card>
            <Statistic title="注册无人机总数" value={fleet.length} suffix="架" />
          </Card>
        </Col>
      </Row>

      <Card
        title="在线状态监控"
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>
            接入无人机
          </Button>
        }
      >
        <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
          <Col xs={24} md={12}>
            <Badge
              status="processing"
              text={`平均电量 ${connectionHealth.avgBattery}%`}
              style={{ fontSize: 14 }}
            />
          </Col>
          <Col xs={24} md={12}>
            <Badge
              status={connectionHealth.slowLinks ? 'warning' : 'success'}
              text={`链路异常 ${connectionHealth.slowLinks} 架`}
              style={{ fontSize: 14 }}
            />
          </Col>
        </Row>
        <Table<UavDevice>
          rowKey="id"
          dataSource={visibleFleet}
          columns={columns}
          pagination={false}
          locale={{ emptyText: '暂无数据' }}
        />
      </Card>

      <Modal
        title="接入无人机"
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        okText="提交接入"
        destroyOnClose
        width={680}
      >
        <Form<RegisterForm> layout="vertical" form={form}>
          <Form.Item
            name="uavCode"
            label="无人机编号"
            rules={[{ required: true, message: '请输入编号' }]}
          >
            <Input placeholder="唯一编号，如 UAV-2024-021" />
          </Form.Item>
          <Form.Item
            name="model"
            label="机型"
            rules={[{ required: true, message: '请输入机型' }]}
          >
            <Input placeholder="多旋翼 / 固定翼" />
          </Form.Item>
          <Form.Item
            name="pilotUsername"
            label="责任人"
            rules={[{ required: true, message: '请选择责任人' }]}
          >
            <Select
              options={userOptions}
              placeholder="选择责任人（操作员/超级管理员）"
              showSearch
              filterOption={(input, option) =>
                (option?.label as string).toLowerCase().includes(input.toLowerCase())
              }
            />
          </Form.Item>
          <Form.Item name="sensors" label="传感器类型">
            <Select
              mode="multiple"
              options={sensorOptions}
              placeholder="选择传感器类型"
              allowClear
            />
          </Form.Item>
          <Form.Item name="metadata" label="附加元数据 (JSON 字符串)">
            <Input.TextArea rows={3} placeholder='例如 {"group":"森林火情"}' />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

export default FleetCenter;
