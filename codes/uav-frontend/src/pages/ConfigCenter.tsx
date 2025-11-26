import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons';
import {
  Button,
  Card,
  Col,
  Form,
  Input,
  Modal,
  Row,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  Typography,
  message
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useMemo, useState } from 'react';
import { configApi, type MetricItem, type MissionTypeItem, type SensorTypeItem } from '../services/api';

function ConfigCenter() {
  // Mission Types
  const [missionTypes, setMissionTypes] = useState<MissionTypeItem[]>([]);
  const [metrics, setMetrics] = useState<MetricItem[]>([]);
  const [sensors, setSensors] = useState<SensorTypeItem[]>([]);
  const [mtModal, setMtModal] = useState<{ open: boolean; editing?: MissionTypeItem }>({ open: false });
  const [metricModal, setMetricModal] = useState<{ open: boolean; editing?: MetricItem }>({ open: false });
  const [sensorModal, setSensorModal] = useState<boolean>(false);
  const [mtForm] = Form.useForm<MissionTypeItem>();
  const [metricForm] = Form.useForm<MetricItem>();
  const [sensorForm] = Form.useForm<SensorTypeItem>();

  const loadAll = () => {
    (configApi.missionTypes.list() as Promise<MissionTypeItem[] | undefined>)
      .then(data => setMissionTypes(data ?? []))
      .catch(() => setMissionTypes([]));
    (configApi.metrics.list() as Promise<MetricItem[] | undefined>)
      .then(data => setMetrics(data ?? []))
      .catch(() => setMetrics([]));
    (configApi.sensors.list() as Promise<SensorTypeItem[] | undefined>)
      .then(data => setSensors(data ?? []))
      .catch(() => setSensors([]));
  };

  useEffect(() => {
    loadAll();
  }, []);

  const metricOptions = useMemo(
    () => metrics.map(m => ({ label: `${m.name} (${m.metricCode})`, value: m.id })),
    [metrics]
  );

  const sensorOptions = useMemo(
    () => sensors.map(s => ({ label: `${s.name} (${s.sensorCode})`, value: s.id })),
    [sensors]
  );

  const mtColumns: ColumnsType<MissionTypeItem> = [
    { title: 'Type Code', dataIndex: 'typeCode', key: 'typeCode' },
    { title: '名称', dataIndex: 'displayName', key: 'displayName' },
    { title: '描述', dataIndex: 'description', key: 'description' },
    {
      title: '关注指标',
      key: 'metrics',
      render: (_, record) => (
        <Space wrap>
          {record.metricIds?.map(id => {
            const metric = metrics.find(m => m.id === id);
            return <Tag key={id}>{metric ? metric.name : id}</Tag>;
          })}
        </Space>
      )
    },
    {
      title: '操作',
      key: 'actions',
      render: (_, record) => (
        <Space>
          <Button
            size="small"
            icon={<EditOutlined />}
            onClick={() => {
              setMtModal({ open: true, editing: record });
              mtForm.setFieldsValue(record);
            }}
          >
            编辑
          </Button>
          <Button
            size="small"
            danger
            icon={<DeleteOutlined />}
            onClick={() =>
              Modal.confirm({
                title: `删除任务类型 ${record.displayName}?`,
                onOk: () =>
                  configApi.missionTypes
                    .delete(record.id!)
                    .then(loadAll)
                    .catch(err => message.error(err.message || '删除失败'))
              })
            }
          >
            删除
          </Button>
        </Space>
      )
    }
  ];

  const metricColumns: ColumnsType<MetricItem> = [
    { title: 'Metric Code', dataIndex: 'metricCode', key: 'metricCode' },
    { title: '名称', dataIndex: 'name', key: 'name' },
    { title: '单位', dataIndex: 'unit', key: 'unit' },
    { title: '描述', dataIndex: 'description', key: 'description' },
    {
      title: '传感器类型',
      key: 'sensors',
      render: (_, record) => (
        <Space wrap>
          {record.sensorTypeIds?.map(id => {
            const sensor = sensors.find(s => s.id === id);
            return <Tag key={id}>{sensor ? sensor.name : id}</Tag>;
          })}
        </Space>
      )
    },
    {
      title: '操作',
      key: 'actions',
      render: (_, record) => (
        <Space>
          <Button
            size="small"
            icon={<EditOutlined />}
            onClick={() => {
              setMetricModal({ open: true, editing: record });
              metricForm.setFieldsValue(record);
            }}
          >
            编辑
          </Button>
          <Button
            size="small"
            danger
            icon={<DeleteOutlined />}
            onClick={() =>
              Modal.confirm({
                title: `删除指标 ${record.name}?`,
                onOk: () =>
                  configApi.metrics
                    .delete(record.id!)
                    .then(loadAll)
                    .catch(err => message.error(err.message || '删除失败'))
              })
            }
          >
            删除
          </Button>
        </Space>
      )
    }
  ];

  const sensorColumns: ColumnsType<SensorTypeItem> = [
    { title: 'Sensor Code', dataIndex: 'sensorCode', key: 'sensorCode' },
    { title: '名称', dataIndex: 'name', key: 'name' },
    { title: '描述', dataIndex: 'description', key: 'description' },
    {
      title: '操作',
      key: 'actions',
      render: (_, record) => (
        <Button
          size="small"
          danger
          icon={<DeleteOutlined />}
          onClick={() =>
            Modal.confirm({
              title: `删除传感器 ${record.name}?`,
              onOk: () =>
                configApi.sensors
                  .delete(record.id!)
                  .then(loadAll)
                  .catch(err => message.error(err.message || '删除失败'))
            })
          }
        >
          删除
        </Button>
      )
    }
  ];

  const mtSubmit = () => {
    mtForm
      .validateFields()
      .then(values => {
        const req = { ...values, metricIds: values.metricIds || [] };
        const action = mtModal.editing
          ? configApi.missionTypes.update(mtModal.editing.id!, req)
          : configApi.missionTypes.create(req);
        return action.then(() => {
          setMtModal({ open: false, editing: undefined });
          mtForm.resetFields();
          loadAll();
          message.success('保存成功');
        });
      })
      .catch(() => undefined);
  };

  const metricSubmit = () => {
    metricForm
      .validateFields()
      .then(values => {
        const req = { ...values, sensorTypeIds: values.sensorTypeIds || [] };
        const action = metricModal.editing
          ? configApi.metrics.update(metricModal.editing.id!, req)
          : configApi.metrics.create(req);
        return action.then(() => {
          setMetricModal({ open: false, editing: undefined });
          metricForm.resetFields();
          loadAll();
          message.success('保存成功');
        });
      })
      .catch(() => undefined);
  };

  const sensorSubmit = () => {
    sensorForm
      .validateFields()
      .then(values => {
        return configApi.sensors.create(values).then(() => {
          setSensorModal(false);
          sensorForm.resetFields();
          loadAll();
          message.success('新增成功');
        });
      })
      .catch(() => undefined);
  };

  return (
    <>
      <Tabs
        defaultActiveKey="mission"
        items={[
          {
            key: 'mission',
            label: '任务类型管理',
            children: (
              <Card
                title="任务类型"
                extra={
                  <Button icon={<PlusOutlined />} type="primary" onClick={() => setMtModal({ open: true })}>
                    新增任务类型
                  </Button>
                }
              >
                <Table<MissionTypeItem> rowKey="id" dataSource={missionTypes} columns={mtColumns} pagination={false} />
              </Card>
            )
          },
          {
            key: 'metric',
            label: '指标管理',
            children: (
              <Card
                title="指标"
                extra={
                  <Button icon={<PlusOutlined />} type="primary" onClick={() => setMetricModal({ open: true })}>
                    新增指标
                  </Button>
                }
              >
                <Table<MetricItem> rowKey="id" dataSource={metrics} columns={metricColumns} pagination={false} />
              </Card>
            )
          },
          {
            key: 'sensor',
            label: '传感器管理',
            children: (
              <Card
                title="传感器"
                extra={
                  <Button icon={<PlusOutlined />} type="primary" onClick={() => setSensorModal(true)}>
                    新增传感器
                  </Button>
                }
              >
                <Table<SensorTypeItem> rowKey="id" dataSource={sensors} columns={sensorColumns} pagination={false} />
              </Card>
            )
          }
        ]}
      />

      {/* Mission Type Modal */}
      <Modal
        title={mtModal.editing ? '编辑任务类型' : '新增任务类型'}
        open={mtModal.open}
        onCancel={() => {
          setMtModal({ open: false, editing: undefined });
          mtForm.resetFields();
        }}
        onOk={mtSubmit}
        okText="保存"
      >
        <Form layout="vertical" form={mtForm} initialValues={{ metricIds: [] }}>
          {!mtModal.editing ? (
            <Form.Item
              name="typeCode"
              label="Type Code"
              rules={[{ required: true, message: '请输入类型编码' }]}
            >
              <Input placeholder="唯一编码，如 FOREST_FIRE" />
            </Form.Item>
          ) : null}
          <Form.Item
            name="displayName"
            label="名称"
            rules={[{ required: true, message: '请输入名称' }]}
          >
            <Input placeholder="任务类型名称" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} placeholder="可选" />
          </Form.Item>
          <Form.Item name="metricIds" label="关注指标">
            <Select mode="multiple" options={metricOptions} placeholder="选择关联指标" />
          </Form.Item>
        </Form>
      </Modal>

      {/* Metric Modal */}
      <Modal
        title={metricModal.editing ? '编辑指标' : '新增指标'}
        open={metricModal.open}
        onCancel={() => {
          setMetricModal({ open: false, editing: undefined });
          metricForm.resetFields();
        }}
        onOk={metricSubmit}
        okText="保存"
      >
        <Form layout="vertical" form={metricForm}>
          {!metricModal.editing ? (
            <Form.Item
              name="metricCode"
              label="Metric Code"
              rules={[{ required: true, message: '请输入指标编码' }]}
            >
              <Input placeholder="唯一编码，如 SURFACE_TEMP" />
            </Form.Item>
          ) : null}
          <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
            <Input placeholder="指标名称" />
          </Form.Item>
          <Form.Item name="unit" label="单位">
            <Input placeholder="可选，如 °C / µg/m³" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} placeholder="可选" />
          </Form.Item>
          <Form.Item name="sensorTypeIds" label="传感器类型">
            <Select mode="multiple" options={sensorOptions} placeholder="选择传感器类型" />
          </Form.Item>
        </Form>
      </Modal>

      {/* Sensor Modal */}
      <Modal
        title="新增传感器"
        open={sensorModal}
        onCancel={() => {
          setSensorModal(false);
          sensorForm.resetFields();
        }}
        onOk={sensorSubmit}
        okText="保存"
      >
        <Form layout="vertical" form={sensorForm}>
          <Form.Item
            name="sensorCode"
            label="Sensor Code"
            rules={[{ required: true, message: '请输入传感器编码' }]}
          >
            <Input placeholder="唯一编码，如 THERMAL" />
          </Form.Item>
          <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
            <Input placeholder="传感器名称" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} placeholder="可选" />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
}

export default ConfigCenter;
