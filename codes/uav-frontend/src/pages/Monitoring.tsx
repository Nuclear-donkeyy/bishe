import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons';
import {
  Badge,
  Button,
  Card,
  Col,
  Descriptions,
  Form,
  Input,
  Modal,
  Row,
  Space,
  Statistic,
  Table,
  Tag,
  Typography,
  Select,
  message
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMemo, useState } from 'react';
import type { MonitoringTask, Rule } from '../data/mock';
import { monitoringTasks as initialTasks } from '../data/mock';

function Monitoring() {
  const [tasks, setTasks] = useState<MonitoringTask[]>(initialTasks);
  const [activeTaskId, setActiveTaskId] = useState(initialTasks[0]?.id ?? '');
  const [ruleModalOpen, setRuleModalOpen] = useState(false);
  const [editingRule, setEditingRule] = useState<{ taskId: string; rule?: Rule } | null>(null);
  const [ruleForm] = Form.useForm<Rule>();

  const activeTask = useMemo(
    () => tasks.find(task => task.id === activeTaskId),
    [tasks, activeTaskId]
  );

  const dataColumns: ColumnsType<{ label: string; value: string; unit?: string }> = [
    { title: '指标', dataIndex: 'label', key: 'label' },
    {
      title: '数值',
      dataIndex: 'value',
      key: 'value',
      render: (value, record) => `${value}${record.unit ? ` ${record.unit}` : ''}`
    }
  ];

  const handleEditRule = (taskId: string, rule?: Rule) => {
    setEditingRule({ taskId, rule });
    setRuleModalOpen(true);
    if (rule) {
      ruleForm.setFieldsValue(rule);
    } else {
      ruleForm.resetFields();
    }
  };

  const handleDeleteRule = (taskId: string, ruleId: string) => {
    setTasks(prev =>
      prev.map(task =>
        task.id === taskId
          ? { ...task, rules: task.rules.filter(rule => rule.id !== ruleId) }
          : task
      )
    );
    message.success('已删除规则');
  };

  const submitRule = () => {
    if (!editingRule) return;
    ruleForm
      .validateFields()
      .then(values => {
        setTasks(prev =>
          prev.map(task => {
            if (task.id !== editingRule.taskId) return task;
            const ruleId = editingRule.rule?.id ?? `RULE-${Date.now()}`;
            const newRule: Rule = { ...values, id: ruleId };
            const rules = editingRule.rule
              ? task.rules.map(rule => (rule.id === ruleId ? newRule : rule))
              : [...task.rules, newRule];
            return { ...task, rules };
          })
        );
        message.success(editingRule.rule ? '已更新规则' : '已新增规则');
        setRuleModalOpen(false);
        setEditingRule(null);
        ruleForm.resetFields();
      })
      .catch(() => undefined);
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <Row gutter={[16, 16]}>
        <Col xs={24} md={8}>
          <Card title="任务条目">
            <Space direction="vertical" style={{ width: '100%' }}>
              {tasks.map(task => (
                <Card
                  key={task.id}
                  size="small"
                  onClick={() => setActiveTaskId(task.id)}
                  style={{
                    cursor: 'pointer',
                    borderColor: task.id === activeTaskId ? '#1677ff' : undefined
                  }}
                >
                  <Space direction="vertical" size={0}>
                    <Space>
                      <Typography.Text strong>{task.missionName}</Typography.Text>
                      <Tag color={task.status === '执行中' ? 'green' : task.status === '计划中' ? 'blue' : 'default'}>
                        {task.status}
                      </Tag>
                    </Space>
                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                      执行人：{task.owner} · 区域：{task.location} · 设备 {task.devices} 架
                    </Typography.Text>
                  </Space>
                </Card>
              ))}
            </Space>
          </Card>
        </Col>
        <Col xs={24} md={16}>
          {activeTask ? (
            <Space direction="vertical" style={{ width: '100%' }}>
              <Card title={`${activeTask.missionName} · 数据展示`}>
                <Row gutter={[16, 16]}>
                  {activeTask.data.map(item => (
                    <Col xs={24} md={8} key={item.label}>
                      <Statistic
                        title={item.label}
                        value={item.value}
                        suffix={item.unit}
                        valueStyle={{ fontSize: 28 }}
                      />
                    </Col>
                  ))}
                </Row>
                <Descriptions
                  bordered
                  column={2}
                  style={{ marginTop: 16 }}
                  size="small"
                  items={[
                    { key: 'owner', label: '责任人', children: activeTask.owner },
                    { key: 'status', label: '状态', children: activeTask.status },
                    { key: 'location', label: '区域', children: activeTask.location },
                    { key: 'devices', label: '设备数', children: `${activeTask.devices} 架` }
                  ]}
                />
              </Card>
              <Card
                title="告警规则"
                extra={
                  <Button
                    type="primary"
                    icon={<PlusOutlined />}
                    onClick={() => handleEditRule(activeTask.id)}
                  >
                    新增
                  </Button>
                }
              >
                <Table<Rule>
                  rowKey="id"
                  dataSource={activeTask.rules}
                  pagination={false}
                  columns={[
                    { title: '规则名称', dataIndex: 'name', key: 'name' },
                    { title: '监测指标', dataIndex: 'metric', key: 'metric' },
                    { title: '阈值', dataIndex: 'threshold', key: 'threshold' },
                    {
                      title: '等级',
                      dataIndex: 'level',
                      key: 'level',
                      render: value => (
                        <Badge
                          status={
                            value === '警报' ? 'error' : value === '预警' ? 'warning' : 'processing'
                          }
                          text={value}
                        />
                      )
                    },
                    {
                      title: '操作',
                      key: 'actions',
                      render: (_, record) => (
                        <Space>
                          <Button
                            type="text"
                            icon={<EditOutlined />}
                            onClick={() => handleEditRule(activeTask.id, record)}
                          />
                          <Button
                            type="text"
                            icon={<DeleteOutlined />}
                            danger
                            onClick={() => handleDeleteRule(activeTask.id, record.id)}
                          />
                        </Space>
                      )
                    }
                  ]}
                  locale={{
                    emptyText: '暂无规则'
                  }}
                />
              </Card>
            </Space>
          ) : (
            <Card>请选择任务以查看数据</Card>
          )}
        </Col>
      </Row>

      <Modal
        title={editingRule?.rule ? '编辑规则' : '新增规则'}
        open={ruleModalOpen}
        onCancel={() => {
          setRuleModalOpen(false);
          setEditingRule(null);
          ruleForm.resetFields();
        }}
        onOk={submitRule}
        okText="保存"
      >
        <Form<Rule> layout="vertical" form={ruleForm}>
          <Form.Item
            name="name"
            label="规则名称"
            rules={[{ required: true, message: '请输入规则名称' }]}
          >
            <Input placeholder="如 高温告警" />
          </Form.Item>
          <Form.Item
            name="metric"
            label="监测指标"
            rules={[{ required: true, message: '请输入指标' }]}
          >
            <Input placeholder="如 地表温度" />
          </Form.Item>
          <Form.Item
            name="threshold"
            label="阈值"
            rules={[{ required: true, message: '请输入阈值说明' }]}
          >
            <Input placeholder="> 60°C & 5min" />
          </Form.Item>
          <Form.Item name="level" label="等级" rules={[{ required: true }]}>
            <Select
              options={[
                { value: '提示', label: '提示' },
                { value: '预警', label: '预警' },
                { value: '警报', label: '警报' }
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

export default Monitoring;
