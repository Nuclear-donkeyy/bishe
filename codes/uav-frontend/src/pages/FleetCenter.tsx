import { PlusOutlined } from '@ant-design/icons';
import { Badge, Button, Card, Col, Form, Input, InputNumber, Modal, Row, Select, Space, Statistic, Table, Tag, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMemo, useState } from 'react';
import { fleetItems as initialFleet, type FleetItem } from '../data/mock';
import { useAuth } from '../context/AuthContext';

const statusTag = (status: FleetItem['status']) => {
  if (status === 'critical') {
    return <Tag color="red">离线</Tag>;
  }
  if (status === 'warning') {
    return <Tag color="orange">链路弱</Tag>;
  }
  return <Tag color="green">在线</Tag>;
};

const columns: ColumnsType<FleetItem> = [
  {
    title: '无人机',
    dataIndex: 'id',
    key: 'id',
    render: (value, record) => (
      <Space direction="vertical" size={0}>
        <strong>{value}</strong>
        <span style={{ color: '#667085', fontSize: 12 }}>{record.model}</span>
      </Space>
    )
  },
  {
    title: '任务 / 驾驶员',
    key: 'mission',
    render: (_, record) => (
      <Space direction="vertical" size={0}>
        <span>{record.mission}</span>
        <span style={{ color: '#667085', fontSize: 12 }}>{record.pilot}</span>
      </Space>
    )
  },
  {
    title: '电量',
    dataIndex: 'battery',
    key: 'battery',
    render: value => `${value}%`
  },
  {
    title: '链路质量',
    dataIndex: 'linkQuality',
    key: 'linkQuality',
    render: (value, record) => `${value} · RTT ${record.rtt}ms`
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    render: statusTag
  }
];

function FleetCenter() {
  const [fleet, setFleet] = useState<FleetItem[]>(initialFleet);
  const [modalOpen, setModalOpen] = useState(false);
  const [form] = Form.useForm<FleetItem>();
  const { currentUser } = useAuth();

  const visibleFleet = useMemo(() => {
    if (currentUser?.role === 'superadmin') return fleet;
    return fleet.filter(item => item.pilot === currentUser?.name);
  }, [fleet, currentUser]);

  const connectionHealth = useMemo(
    () => ({
      avgBattery:
        visibleFleet.length > 0
          ? Math.round(visibleFleet.reduce((sum, item) => sum + item.battery, 0) / visibleFleet.length)
          : 0,
      slowLinks: visibleFleet.filter(item => item.status !== 'online').length
    }),
    [visibleFleet]
  );

  const handleSubmit = () => {
    form
      .validateFields()
      .then(values => {
        const newItem: FleetItem = {
          ...values,
          id: values.id || `UAV-${Math.floor(Math.random() * 900 + 100)}`,
          status: values.status || 'online',
          pilot: currentUser?.role === 'superadmin' ? values.pilot : currentUser?.name || values.pilot
        };
        setFleet(prev => [newItem, ...prev]);
        message.success(`已接入 ${newItem.id}`);
        setModalOpen(false);
        form.resetFields();
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
            <Statistic title="链路预警" value={connectionHealth.slowLinks} suffix="架" />
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card>
            <Statistic title="平均电量" value={connectionHealth.avgBattery} suffix="%" />
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card>
            <Statistic title="全部注册无人机" value={fleet.length} suffix="架" />
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
        <Table<FleetItem>
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
        okText="提交审批"
        destroyOnClose
        width={600}
      >
        <Form<FleetItem>
          layout="vertical"
          form={form}
          initialValues={{
            pilot: currentUser?.role === 'superadmin' ? undefined : currentUser?.name
          }}
        >
          <Form.Item name="id" label="编号">
            <Input placeholder="如 UAV-2024-021" />
          </Form.Item>
          <Form.Item
            name="model"
            label="机型"
            rules={[{ required: true, message: '请输入机型' }]}
          >
            <Input placeholder="多旋翼 / 固定翼" />
          </Form.Item>
          <Form.Item
            name="mission"
            label="任务"
            rules={[{ required: true, message: '请输入任务名称' }]}
          >
            <Input placeholder="任务名称" />
          </Form.Item>
          {currentUser?.role === 'superadmin' ? (
            <Form.Item
              name="pilot"
              label="责任人"
              rules={[{ required: true, message: '请输入责任人' }]}
            >
              <Input placeholder="驾驶员/责任人" />
            </Form.Item>
          ) : (
            <Form.Item label="责任人">
              <Input value={currentUser?.name} disabled />
            </Form.Item>
          )}
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item
                name="battery"
                label="当前电量 (%)"
                rules={[{ required: true, message: '请输入电量' }]}
              >
                <InputNumber min={0} max={100} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="rtt"
                label="链路 RTT (ms)"
                rules={[{ required: true, message: '请输入 RTT' }]}
              >
                <InputNumber min={0} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item
            name="linkQuality"
            label="链路质量"
            rules={[{ required: true, message: '请选择链路质量' }]}
          >
            <Select
              options={[
                { value: '优', label: '优' },
                { value: '良', label: '良' },
                { value: '弱', label: '弱' }
              ]}
            />
          </Form.Item>
          <Form.Item name="status" label="状态" initialValue="online">
            <Select
              options={[
                { value: 'online', label: '在线' },
                { value: 'warning', label: '链路弱' },
                { value: 'critical', label: '离线' }
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

export default FleetCenter;
