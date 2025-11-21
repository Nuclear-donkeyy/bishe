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
import { useEffect, useMemo, useState } from 'react';
import type { MonitoringTask, Rule, MissionTypeKey } from '../data/mock';
import { monitoringTasks as initialTasks } from '../data/mock';

const videoMissionTypes = new Set<MissionTypeKey>([
  '森林火情巡查',
  '林业健康监测',
  '土地利用变化复拍'
]);

const keyMetricsMap: Partial<Record<MissionTypeKey, string[]>> = {
  森林火情巡查: ['地表温度', '烟雾概率', '风速'],
  林业健康监测: ['NDVI 平均值', '病害疑似面积'],
  土地利用变化复拍: ['变化区域', '疑似类型'],
  城市热岛监测: ['地表温度', '昼夜温差'],
  空气质量剖面: ['PM2.5', 'PM10', '氧气浓度', '二氧化碳']
};

function Monitoring() {
  const [tasks, setTasks] = useState<MonitoringTask[]>(initialTasks);
  const [activeTaskId, setActiveTaskId] = useState(
    initialTasks.find(task => task.status === '执行中')?.id ?? ''
  );
  const [ruleModalOpen, setRuleModalOpen] = useState(false);
  const [editingRule, setEditingRule] = useState<{ taskId: string; rule?: Rule } | null>(null);
  const [ruleForm] = Form.useForm<Rule>();

  const executingTasks = useMemo(
    () => tasks.filter(task => task.status === '执行中'),
    [tasks]
  );

  const activeTask = useMemo(
    () => executingTasks.find(task => task.id === activeTaskId),
    [executingTasks, activeTaskId]
  );

  useEffect(() => {
    if (!executingTasks.length) {
      setActiveTaskId('');
      return;
    }
    if (!executingTasks.some(task => task.id === activeTaskId)) {
      setActiveTaskId(executingTasks[0].id);
    }
  }, [executingTasks, activeTaskId]);

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

  const layoutHeight = 'calc(100vh - 220px)';

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <Row gutter={[16, 16]} style={{ minHeight: layoutHeight }}>
        <Col xs={24} lg={7} style={{ height: layoutHeight }}>
          <Card
            title="执行中任务"
            style={{ height: '100%' }}
            bodyStyle={{ display: 'flex', flexDirection: 'column', height: '100%', paddingBottom: 0 }}
          >
            <div style={{ flex: 1, overflow: 'auto' }}>
              {executingTasks.length ? (
                <Space direction="vertical" style={{ width: '100%' }}>
                  {executingTasks.map(task => (
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
                          <Tag color="green">执行中</Tag>
                        </Space>
                        <Space size="small">
                          <Tag>{task.missionType}</Tag>
                        </Space>
                        <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                          执行人：{task.owner} · 区域：{task.location} · 设备 {task.devices} 架
                        </Typography.Text>
                      </Space>
                    </Card>
                  ))}
                </Space>
              ) : (
                <Card size="small" style={{ textAlign: 'center' }} bordered={false}>
                  暂无执行中任务
                </Card>
              )}
            </div>
          </Card>
        </Col>
        <Col xs={24} lg={17} style={{ height: layoutHeight }}>
          {activeTask ? (
            <div style={{ height: '100%', display: 'flex', flexDirection: 'column', gap: 16 }}>
              <Card style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
                <Typography.Title level={4} style={{ marginBottom: 16 }}>
                  {activeTask.missionName} · 数据展示
                </Typography.Title>
                {(() => {
                  const keyMetricLabels = keyMetricsMap[activeTask.missionType] ?? [];
                  const keyMetricData = activeTask.data.filter(item =>
                    keyMetricLabels.includes(item.label)
                  );
                  const showVideo =
                    videoMissionTypes.has(activeTask.missionType) && activeTask.videoUrl;
                  return showVideo || keyMetricData.length ? (
                    <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
                      {showVideo ? (
                        <Col xs={24} md={keyMetricData.length ? 12 : 24}>
                          <Card size="small" title="实时回传视频">
                            <video
                              src={activeTask.videoUrl}
                              controls
                              autoPlay
                              muted
                              loop
                              style={{ width: '100%', borderRadius: 8, background: '#000' }}
                            />
                          </Card>
                        </Col>
                      ) : null}
                      {keyMetricData.length ? (
                        <Col xs={24} md={showVideo ? 12 : 24}>
                          <Card size="small" title="关键指标">
                            <Row gutter={[12, 12]}>
                              {keyMetricData.map(item => (
                                <Col span={12} key={item.label}>
                                  <Statistic
                                    title={item.label}
                                    value={item.value}
                                    suffix={item.unit}
                                    valueStyle={{ fontSize: 24 }}
                                  />
                                </Col>
                              ))}
                            </Row>
                          </Card>
                        </Col>
                      ) : null}
                    </Row>
                  ) : null;
                })()}
                <Descriptions
                  bordered
                  column={2}
                  style={{ marginTop: 16 }}
                  size="small"
                  items={[
                    { key: 'owner', label: '责任人', children: activeTask.owner },
                    { key: 'status', label: '状态', children: activeTask.status },
                    { key: 'type', label: '任务类型', children: activeTask.missionType },
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
                style={{ flex: 1, overflow: 'auto' }}
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
            </div>
          ) : (
            <Card style={{ height: '100%' }}>暂无执行中任务</Card>
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
