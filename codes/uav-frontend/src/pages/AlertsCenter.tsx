import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import {
  Badge,
  Button,
  Card,
  Col,
  Empty,
  Form,
  Input,
  Modal,
  Row,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useMemo, useState } from 'react';
import {
  alertApi,
  configApi,
  type AlertCondition,
  type AlertRecord,
  type AlertRule,
  type MetricItem
} from '../services/api';

type RuleForm = {
  name: string;
  description?: string;
  logicOperator: 'AND' | 'OR';
  conditions: AlertCondition[];
};

function AlertsCenter() {
  const [rules, setRules] = useState<AlertRule[]>([]);
  const [records, setRecords] = useState<AlertRecord[]>([]);
  const [metrics, setMetrics] = useState<MetricItem[]>([]);
  const [activeRuleId, setActiveRuleId] = useState<number | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingRule, setEditingRule] = useState<AlertRule | null>(null);
  const [ruleForm] = Form.useForm<RuleForm>();
  const [loadingRules, setLoadingRules] = useState(false);
  const [loadingRecords, setLoadingRecords] = useState(false);

  const loadRules = async () => {
    try {
      setLoadingRules(true);
      const data = await alertApi.rules.list();
      setRules(data);
      if (data.length && activeRuleId == null) {
        setActiveRuleId(data[0].id);
      }
    } catch (e: any) {
      message.error(e?.message || '加载报警规则失败');
    } finally {
      setLoadingRules(false);
    }
  };

  const loadRecords = async (ruleId?: number) => {
    try {
      setLoadingRecords(true);
      const data = await alertApi.records.list(ruleId);
      setRecords(data);
    } catch (e: any) {
      message.error(e?.message || '加载报警记录失败');
    } finally {
      setLoadingRecords(false);
    }
  };

  useEffect(() => {
    configApi.metrics
      .list()
      .then(res => setMetrics((res as MetricItem[]) || []))
      .catch(() => setMetrics([]));
    loadRules();
  }, []);

  useEffect(() => {
    if (activeRuleId != null) {
      loadRecords(activeRuleId);
    } else {
      setRecords([]);
    }
  }, [activeRuleId]);

  const handleSaveRule = () => {
    ruleForm
      .validateFields()
      .then(values => {
        if (editingRule) {
          return alertApi.rules.update(editingRule.id!, {
            name: values.name,
            description: values.description,
            logicOperator: values.logicOperator,
            conditions: values.conditions || []
          });
        }
        return alertApi.rules.create({
          name: values.name,
          description: values.description,
          logicOperator: values.logicOperator,
          conditions: values.conditions || []
        });
      })
      .then(() => {
        message.success(editingRule ? '规则已更新' : '规则已创建');
        setModalOpen(false);
        setEditingRule(null);
        ruleForm.resetFields();
        loadRules();
      })
      .catch(() => undefined);
  };

  const handleProcessRecord = async (id: number) => {
    await alertApi.records.process(id);
    message.success('已标记处理');
    loadRecords(activeRuleId ?? undefined);
    loadRules();
  };

  const metricOptions = metrics.map(m => ({ label: `${m.name} (${m.metricCode})`, value: m.metricCode }));

  const handleDeleteRule = async (id: number) => {
    await alertApi.rules.delete(id);
    message.success('规则已删除');
    if (activeRuleId === id) {
      setActiveRuleId(null);
    }
    loadRules();
  };

  const handleEditRule = (rule: AlertRule) => {
    ruleForm.setFieldsValue({
      name: rule.name,
      description: rule.description,
      logicOperator: rule.logicOperator as 'AND' | 'OR',
      conditions: rule.conditions?.map(c => ({
        metricCode: c.metricCode,
        comparator: c.comparator,
        threshold: c.threshold
      }))
    });
    setModalOpen(true);
    setEditingRule(rule);
  };

  const columns: ColumnsType<AlertRecord> = [
    { title: '任务', dataIndex: 'missionCode', key: 'missionCode', render: v => v || '--' },
    { title: '无人机', dataIndex: 'uavCode', key: 'uavCode', render: v => v || '--' },
    { title: '指标', dataIndex: 'metricCode', key: 'metricCode' },
    { title: '值', dataIndex: 'metricValue', key: 'metricValue', render: v => (v == null ? '--' : v) },
    { title: '时间', dataIndex: 'triggeredAt', key: 'triggeredAt' },
    {
      title: '状态',
      dataIndex: 'processed',
      key: 'processed',
      render: v => (v ? <Tag color="default">已处理</Tag> : <Tag color="red">未处理</Tag>)
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) =>
        record.processed ? null : (
          <Button size="small" type="link" onClick={() => handleProcessRecord(record.id)}>
            标记处理
          </Button>
        )
    }
  ];

  const ruleList = useMemo(
    () =>
      rules.map(rule => ({
        ...rule,
        key: rule.id
      })),
    [rules]
  );

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16, minHeight: '100vh' }}>
      <Row gutter={[16, 16]} style={{ flex: 1, minHeight: '80vh', height: '100%' }}>
        <Col xs={24} lg={8} style={{ height: '100%', display: 'flex' }}>
          <Card
            title="报警规则"
            extra={
              <Button icon={<PlusOutlined />} type="primary" onClick={() => setModalOpen(true)}>
                新建规则
              </Button>
            }
            style={{ width: '100%' }}
            bodyStyle={{ height: '100%', display: 'flex', flexDirection: 'column' }}
            loading={loadingRules}
          >
            <div style={{ flex: 1, overflowY: 'auto' }}>
              {ruleList.length ? (
                <Space direction="vertical" style={{ width: '100%' }}>
                  {ruleList.map(rule => (
                    <Card
                      key={rule.id}
                      size="small"
                      onClick={() => setActiveRuleId(rule.id)}
                      style={{
                        cursor: 'pointer',
                        borderColor: rule.id === activeRuleId ? '#1677ff' : undefined
                      }}
                      bodyStyle={{ padding: 12 }}
                    >
                      <Space style={{ width: '100%', justifyContent: 'space-between' }}>
                        <Space direction="vertical" size={2}>
                          <Typography.Text strong>{rule.name}</Typography.Text>
                          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                            {rule.logicOperator} · {rule.conditions.length} 条条件
                          </Typography.Text>
                        </Space>
                        <Space>
                          <Badge count={rule.unreadCount} overflowCount={99} />
                          <Button
                            size="small"
                            type="text"
                            icon={<EditOutlined />}
                            onClick={e => {
                              e.stopPropagation();
                              handleEditRule(rule);
                            }}
                          />
                          <Button
                            size="small"
                            type="text"
                            danger
                            icon={<DeleteOutlined />}
                            onClick={e => {
                              e.stopPropagation();
                              handleDeleteRule(rule.id);
                            }}
                          />
                        </Space>
                      </Space>
                    </Card>
                  ))}
                </Space>
              ) : (
                <Empty description="暂无规则" />
              )}
            </div>
          </Card>
        </Col>

        <Col xs={24} lg={16} style={{ height: '100%', display: 'flex' }}>
          <Card
            title="报警记录"
            style={{ width: '100%' }}
            bodyStyle={{ height: '100%', display: 'flex', flexDirection: 'column' }}
            loading={loadingRecords}
          >
            {activeRuleId ? (
              <Table
                rowKey="id"
                dataSource={records}
                columns={columns}
                pagination={{ pageSize: 10 }}
                style={{ flex: 1 }}
              />
            ) : (
              <Empty description="请选择左侧报警规则" />
            )}
          </Card>
        </Col>
      </Row>

      <Modal
        title={editingRule ? '编辑报警规则' : '新建报警规则'}
        open={modalOpen}
        onCancel={() => {
          setModalOpen(false);
          setEditingRule(null);
          ruleForm.resetFields();
        }}
        onOk={handleSaveRule}
        width={720}
        okText={editingRule ? '保存' : '创建'}
      >
        <Form<RuleForm> form={ruleForm} layout="vertical" initialValues={{ logicOperator: 'AND' }}>
          <Form.Item name="name" label="规则名称" rules={[{ required: true, message: '请输入规则名称' }]}>
            <Input placeholder="如：温度过高报警" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input placeholder="规则说明（可选）" />
          </Form.Item>
          <Form.Item name="logicOperator" label="条件运算" rules={[{ required: true }]}>
            <Select
              options={[
                { label: 'AND（全部满足）', value: 'AND' },
                { label: 'OR（任一满足）', value: 'OR' }
              ]}
            />
          </Form.Item>
          <Form.List
            name="conditions"
            rules={[
              {
                validator: async (_, value) => {
                  if (!value || value.length === 0) {
                    return Promise.reject(new Error('请至少添加一条条件'));
                  }
                }
              }
            ]}
          >
            {(fields, { add, remove }, { errors }) => (
              <>
                {fields.map(field => (
                  <Space key={field.key} align="baseline" style={{ display: 'flex', marginBottom: 8 }}>
                    <Form.Item
                      {...field}
                      name={[field.name, 'metricCode']}
                      rules={[{ required: true, message: '请选择指标' }]}
                    >
                      <Select placeholder="指标" style={{ width: 200 }} options={metricOptions} />
                    </Form.Item>
                    <Form.Item
                      {...field}
                      name={[field.name, 'comparator']}
                      rules={[{ required: true, message: '选择运算符' }]}
                    >
                      <Select
                        placeholder="运算符"
                        style={{ width: 120 }}
                        options={[
                          { label: '>', value: 'GT' },
                          { label: '>=', value: 'GTE' },
                          { label: '<', value: 'LT' },
                          { label: '<=', value: 'LTE' },
                          { label: '=', value: 'EQ' }
                        ]}
                      />
                    </Form.Item>
                    <Form.Item
                      {...field}
                      name={[field.name, 'threshold']}
                      rules={[{ required: true, message: '输入阈值' }]}
                    >
                      <Input type="number" placeholder="阈值" style={{ width: 120 }} />
                    </Form.Item>
                    <Button type="link" danger onClick={() => remove(field.name)}>
                      删除
                    </Button>
                  </Space>
                ))}
                <Form.ErrorList errors={errors} />
                <Button type="dashed" onClick={() => add()} icon={<PlusOutlined />}>
                  添加条件
                </Button>
              </>
            )}
          </Form.List>
        </Form>
      </Modal>
    </div>
  );
}

export default AlertsCenter;
