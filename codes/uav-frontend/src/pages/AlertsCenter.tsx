import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons';
import {
  Badge,
  Button,
  Card,
  Col,
  Divider,
  Empty,
  Form,
  Input,
  Modal,
  Row,
  Segmented,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  Typography,
  message
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useState } from 'react';
import {
  alertApi,
  configApi,
  type AlertCondition,
  type AlertRecord,
  type AlertRule,
  type MetricItem
} from '../services/api';

type ConfigMode = 'template' | 'rule';

type RuleForm = {
  name: string;
  templateEnabled?: boolean;
  templateId?: number;
  templateCode?: string;
  templateCategory?: string;
  description?: string;
  logicOperator: 'AND' | 'OR';
  autoInterrupt?: boolean;
  notifyEnabled?: boolean;
  notifyChannels?: string;
  notifyTargets?: string;
  notifyTemplate?: string;
  conditions: AlertCondition[];
};

function AlertsCenter() {
  const [templates, setTemplates] = useState<AlertRule[]>([]);
  const [rules, setRules] = useState<AlertRule[]>([]);
  const [records, setRecords] = useState<AlertRecord[]>([]);
  const [metrics, setMetrics] = useState<MetricItem[]>([]);
  const [activeRuleId, setActiveRuleId] = useState<number | null>(null);
  const [configMode, setConfigMode] = useState<ConfigMode>('rule');
  const [editorMode, setEditorMode] = useState<ConfigMode>('rule');
  const [modalOpen, setModalOpen] = useState(false);
  const [editingRule, setEditingRule] = useState<AlertRule | null>(null);
  const [ruleForm] = Form.useForm<RuleForm>();
  const [loadingRules, setLoadingRules] = useState(false);
  const [loadingRecords, setLoadingRecords] = useState(false);
  const notifyEnabled = Form.useWatch('notifyEnabled', ruleForm);

  const loadRules = async () => {
    try {
      setLoadingRules(true);
      const data = await alertApi.rules.list();
      const templateItems = data.filter(item => item.templateEnabled);
      const ruleItems = data.filter(item => !item.templateEnabled);
      setTemplates(templateItems);
      setRules(ruleItems);
      if (ruleItems.length) {
        setActiveRuleId(prev => (prev != null && ruleItems.some(item => item.id === prev) ? prev : ruleItems[0].id));
      } else {
        setActiveRuleId(null);
      }
    } catch (e: any) {
      message.error(e?.message || '加载报警配置失败');
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

  const metricOptions = metrics.map(metric => ({ label: `${metric.name} (${metric.metricCode})`, value: metric.metricCode }));
  const templateOptions = templates.map(template => ({
    label: template.templateCategory ? `${template.name} · ${template.templateCategory}` : template.name,
    value: template.id
  }));

  const resetModal = () => {
    setModalOpen(false);
    setEditingRule(null);
    ruleForm.resetFields();
  };

  const openCreateModal = (mode: ConfigMode) => {
    setEditorMode(mode);
    setEditingRule(null);
    ruleForm.setFieldsValue({
      templateEnabled: mode === 'template',
      templateId: undefined,
      templateCode: undefined,
      templateCategory: undefined,
      logicOperator: 'AND',
      autoInterrupt: false,
      notifyEnabled: false,
      notifyChannels: undefined,
      notifyTargets: undefined,
      notifyTemplate: undefined,
      conditions: []
    });
    setModalOpen(true);
  };

  const applyTemplateToForm = (templateId?: number) => {
    const template = templates.find(item => item.id === templateId);
    if (!template || editorMode !== 'rule') {
      return;
    }
    ruleForm.setFieldsValue({
      templateId: template.id,
      description: template.description,
      logicOperator: template.logicOperator,
      autoInterrupt: template.autoInterrupt,
      notifyEnabled: template.notifyEnabled,
      notifyChannels: template.notifyChannels,
      notifyTargets: template.notifyTargets,
      notifyTemplate: template.notifyTemplate,
      conditions: template.conditions.map(condition => ({
        metricCode: condition.metricCode,
        comparator: condition.comparator,
        threshold: condition.threshold
      }))
    });
  };

  const handleSaveRule = () => {
    ruleForm
      .validateFields()
      .then(values => {
        const payload = {
          name: values.name,
          templateEnabled: editorMode === 'template',
          templateId: editorMode === 'rule' ? values.templateId : undefined,
          templateCode: editorMode === 'template' ? values.templateCode : undefined,
          templateCategory: editorMode === 'template' ? values.templateCategory : undefined,
          description: values.description,
          logicOperator: values.logicOperator,
          autoInterrupt: values.autoInterrupt,
          notifyEnabled: values.notifyEnabled,
          notifyChannels: values.notifyChannels,
          notifyTargets: values.notifyTargets,
          notifyTemplate: values.notifyTemplate,
          conditions: values.conditions || []
        };
        if (editingRule) {
          return alertApi.rules.update(editingRule.id, payload);
        }
        return alertApi.rules.create(payload);
      })
      .then(() => {
        message.success(editingRule ? '配置已更新' : editorMode === 'template' ? '模板已创建' : '普通规则已创建');
        resetModal();
        loadRules();
      })
      .catch(() => undefined);
  };

  const handleDeleteRule = async (rule: AlertRule) => {
    await alertApi.rules.delete(rule.id);
    message.success(rule.templateEnabled ? '模板已删除' : '普通规则已删除');
    loadRules();
  };

  const handleEditRule = (rule: AlertRule) => {
    const mode: ConfigMode = rule.templateEnabled ? 'template' : 'rule';
    setEditorMode(mode);
    setEditingRule(rule);
    ruleForm.setFieldsValue({
      name: rule.name,
      templateEnabled: rule.templateEnabled,
      templateId: rule.templateId,
      templateCode: rule.templateCode,
      templateCategory: rule.templateCategory,
      description: rule.description,
      logicOperator: rule.logicOperator,
      autoInterrupt: rule.autoInterrupt,
      notifyEnabled: rule.notifyEnabled,
      notifyChannels: rule.notifyChannels,
      notifyTargets: rule.notifyTargets,
      notifyTemplate: rule.notifyTemplate,
      conditions: rule.conditions?.map(condition => ({
        metricCode: condition.metricCode,
        comparator: condition.comparator,
        threshold: condition.threshold
      }))
    });
    setModalOpen(true);
  };

  const handleProcessRecord = async (id: number) => {
    await alertApi.records.process(id);
    message.success('已标记处理');
    loadRecords(activeRuleId ?? undefined);
    loadRules();
  };

  const renderStatusTag = (value?: string) => {
    if (!value) return <Tag>--</Tag>;
    if (value === 'SUCCESS') return <Tag color="green">成功</Tag>;
    if (value === 'PARTIAL') return <Tag color="gold">部分成功</Tag>;
    if (value === 'FAILED') return <Tag color="red">失败</Tag>;
    if (value === 'SKIPPED') return <Tag>跳过</Tag>;
    if (value === 'PLACEHOLDER') return <Tag color="blue">占位发送</Tag>;
    if (value === 'PENDING') return <Tag color="processing">处理中</Tag>;
    return <Tag>{value}</Tag>;
  };

  const columns: ColumnsType<AlertRecord> = [
    { title: '任务', dataIndex: 'missionCode', key: 'missionCode', render: value => value || '--' },
    { title: '无人机', dataIndex: 'uavCode', key: 'uavCode', render: value => value || '--' },
    { title: '指标', dataIndex: 'metricCode', key: 'metricCode', render: value => value || '--' },
    { title: '值', dataIndex: 'metricValue', key: 'metricValue', render: value => (value == null ? '--' : value) },
    { title: '时间', dataIndex: 'triggeredAt', key: 'triggeredAt' },
    { title: '联动状态', dataIndex: 'linkageStatus', key: 'linkageStatus', render: value => renderStatusTag(value) },
    {
      title: '通知状态',
      dataIndex: 'notificationStatus',
      key: 'notificationStatus',
      render: value => renderStatusTag(value)
    },
    {
      title: '联动摘要',
      dataIndex: 'linkageSummary',
      key: 'linkageSummary',
      render: value => (
        <Typography.Text style={{ maxWidth: 260 }} ellipsis={{ tooltip: value || '--' }}>
          {value || '--'}
        </Typography.Text>
      )
    },
    {
      title: '状态',
      dataIndex: 'processed',
      key: 'processed',
      render: value => (value ? <Tag color="default">已处理</Tag> : <Tag color="red">未处理</Tag>)
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

  const visibleConfigs = configMode === 'template' ? templates : rules;
  const activeModeLabel = configMode === 'template' ? '模板配置' : '普通规则配置';

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16, minHeight: '100vh' }}>
      <Row gutter={[16, 16]} style={{ flex: 1, minHeight: '80vh', height: '100%' }}>
        <Col xs={24} lg={9} style={{ height: '100%', display: 'flex' }}>
          <Card
            title="告警配置"
            extra={
              <Space wrap>
                <Segmented<ConfigMode>
                  value={configMode}
                  onChange={value => setConfigMode(value)}
                  options={[
                    { label: '模板配置', value: 'template' },
                    { label: '普通规则配置', value: 'rule' }
                  ]}
                />
                <Button icon={<PlusOutlined />} type="primary" onClick={() => openCreateModal(configMode)}>
                  {configMode === 'template' ? '新增模板' : '新增普通规则'}
                </Button>
              </Space>
            }
            style={{ width: '100%' }}
            bodyStyle={{ height: '100%', display: 'flex', flexDirection: 'column', overflow: 'auto' }}
            loading={loadingRules}
          >
            <Typography.Paragraph type="secondary" style={{ marginBottom: 12 }}>
              {configMode === 'template'
                ? '模板用于沉淀基础条件与联动动作，可作为普通规则的基础配置来源。'
                : '普通规则用于任务实际绑定，可直接新增，也可继承模板后再调整。'}
            </Typography.Paragraph>
            {visibleConfigs.length ? (
              <Space direction="vertical" style={{ width: '100%' }}>
                {visibleConfigs.map(item => (
                  <Card
                    key={item.id}
                    size="small"
                    onClick={() => {
                      if (!item.templateEnabled) {
                        setActiveRuleId(item.id);
                      }
                    }}
                    style={{
                      cursor: item.templateEnabled ? 'default' : 'pointer',
                      borderColor: !item.templateEnabled && item.id === activeRuleId ? '#1677ff' : undefined
                    }}
                    bodyStyle={{ padding: 12 }}
                  >
                    <Space style={{ width: '100%', justifyContent: 'space-between' }} align="start">
                      <Space direction="vertical" size={4} style={{ maxWidth: '78%' }}>
                        <Typography.Text strong>{item.name}</Typography.Text>
                        <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                          {item.logicOperator} · {item.conditions.length} 条条件
                        </Typography.Text>
                        <Space size={[4, 4]} wrap>
                          {item.templateEnabled ? <Tag color="purple">模板</Tag> : <Tag>普通规则</Tag>}
                          {item.templateCategory ? <Tag>{item.templateCategory}</Tag> : null}
                          {item.templateCode ? <Tag color="purple">{item.templateCode}</Tag> : null}
                          {!item.templateEnabled && item.templateName ? <Tag color="cyan">继承 {item.templateName}</Tag> : null}
                          {item.autoInterrupt ? <Tag color="volcano">自动中断</Tag> : null}
                          {item.notifyEnabled ? <Tag color="blue">发送通知</Tag> : null}
                          {!item.autoInterrupt && !item.notifyEnabled ? <Tag>仅记录</Tag> : null}
                        </Space>
                      </Space>
                      <Space>
                        {!item.templateEnabled ? <Badge count={item.unreadCount} overflowCount={99} /> : null}
                        <Button
                          size="small"
                          type="text"
                          icon={<EditOutlined />}
                          onClick={event => {
                            event.stopPropagation();
                            handleEditRule(item);
                          }}
                        />
                        <Button
                          size="small"
                          type="text"
                          danger
                          icon={<DeleteOutlined />}
                          onClick={event => {
                            event.stopPropagation();
                            handleDeleteRule(item);
                          }}
                        />
                      </Space>
                    </Space>
                  </Card>
                ))}
              </Space>
            ) : (
              <Empty description={configMode === 'template' ? '暂无模板配置' : '暂无普通规则'} />
            )}
          </Card>
        </Col>

        <Col xs={24} lg={15} style={{ height: '100%', display: 'flex' }}>
          <Card
            title={activeModeLabel}
            extra={
              configMode === 'rule' ? (
                <Typography.Text type="secondary">任务创建时仅可绑定普通规则</Typography.Text>
              ) : (
                <Typography.Text type="secondary">模板用于提供基础配置</Typography.Text>
              )
            }
            style={{ width: '100%' }}
            bodyStyle={{ height: '100%', display: 'flex', flexDirection: 'column', gap: 16 }}
          >
            {configMode === 'rule' ? (
              <Card
                size="small"
                title="报警记录"
                style={{ flex: 1 }}
                bodyStyle={{ height: '100%', display: 'flex', flexDirection: 'column' }}
                loading={loadingRecords}
              >
                {activeRuleId ? (
                  <Table
                    rowKey="id"
                    dataSource={records}
                    columns={columns}
                    pagination={{ pageSize: 10 }}
                    scroll={{ x: 1080 }}
                    style={{ flex: 1 }}
                  />
                ) : (
                  <Empty description="请选择一条普通规则查看报警记录" />
                )}
              </Card>
            ) : (
              <Card size="small" title="模板说明" style={{ flex: 1 }}>
                <Space direction="vertical" size={12}>
                  <Typography.Paragraph style={{ marginBottom: 0 }}>
                    模板用于沉淀告警条件、联动动作和通知基础参数，适合作为团队内可复用的标准配置。
                  </Typography.Paragraph>
                  <Typography.Paragraph style={{ marginBottom: 0 }}>
                    普通规则在创建时可选择模板，系统会自动带入模板基础配置，随后再按业务场景继续调整阈值、通知对象和联动开关。
                  </Typography.Paragraph>
                  <Typography.Paragraph style={{ marginBottom: 0 }}>
                    任务创建页只展示普通规则；即使前端绕过选择，后端也会阻止模板被直接绑定到任务。
                  </Typography.Paragraph>
                </Space>
              </Card>
            )}
          </Card>
        </Col>
      </Row>

      <Modal
        title={
          editingRule
            ? editorMode === 'template'
              ? '编辑模板配置'
              : '编辑普通规则配置'
            : editorMode === 'template'
              ? '新增模板配置'
              : '新增普通规则配置'
        }
        open={modalOpen}
        onCancel={resetModal}
        onOk={handleSaveRule}
        width={780}
        okText={editingRule ? '保存' : '创建'}
      >
        <Form<RuleForm>
          form={ruleForm}
          layout="vertical"
          initialValues={{ logicOperator: 'AND', autoInterrupt: false, notifyEnabled: false }}
        >
          <Form.Item name="name" label={editorMode === 'template' ? '模板名称' : '规则名称'} rules={[{ required: true, message: '请输入名称' }]}>
            <Input placeholder={editorMode === 'template' ? '如：低电量联动模板' : '如：巡检低电量规则'} />
          </Form.Item>

          {editorMode === 'template' ? (
            <Row gutter={12}>
              <Col span={12}>
                <Form.Item name="templateCode" label="模板编码">
                  <Input placeholder="如：BATTERY_LOW_TEMPLATE" />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item name="templateCategory" label="模板分类">
                  <Input placeholder="如：飞行安全 / 设备健康" />
                </Form.Item>
              </Col>
            </Row>
          ) : (
            <Form.Item name="templateId" label="继承模板">
              <Select
                allowClear
                placeholder={templateOptions.length ? '可选，选择模板后自动带入基础配置' : '暂无可选模板'}
                options={templateOptions}
                onChange={value => applyTemplateToForm(value)}
              />
            </Form.Item>
          )}

          <Form.Item name="description" label="描述">
            <Input placeholder="配置说明（可选）" />
          </Form.Item>

          <Form.Item name="logicOperator" label="条件运算" rules={[{ required: true }]}>
            <Select
              options={[
                { label: 'AND（全部满足）', value: 'AND' },
                { label: 'OR（任一满足）', value: 'OR' }
              ]}
            />
          </Form.Item>

          <Divider orientation="left" plain>
            联动动作
          </Divider>
          <Space direction="vertical" size={12} style={{ width: '100%', marginBottom: 16 }}>
            <Form.Item name="autoInterrupt" label="自动中断任务" valuePropName="checked" style={{ marginBottom: 0 }}>
              <Switch checkedChildren="启用" unCheckedChildren="关闭" />
            </Form.Item>
            <Form.Item name="notifyEnabled" label="发送通知" valuePropName="checked" style={{ marginBottom: 0 }}>
              <Switch checkedChildren="启用" unCheckedChildren="关闭" />
            </Form.Item>
          </Space>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item name="notifyChannels" label="通知渠道">
                <Input disabled={!notifyEnabled} placeholder="如：SMS" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="notifyTargets" label="通知目标">
                <Input disabled={!notifyEnabled} placeholder="手机号或接收人，逗号分隔" />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item name="notifyTemplate" label="通知模板">
            <Input.TextArea
              disabled={!notifyEnabled}
              autoSize={{ minRows: 2, maxRows: 4 }}
              placeholder="可选，不填写时后端使用默认通知文案"
            />
          </Form.Item>

          <Divider orientation="left" plain>
            条件配置
          </Divider>
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
                  <Space key={field.key} align="baseline" style={{ display: 'flex', marginBottom: 8 }} wrap>
                    <Form.Item
                      {...field}
                      name={[field.name, 'metricCode']}
                      rules={[{ required: true, message: '请选择指标' }]}
                    >
                      <Select placeholder="指标" style={{ width: 220 }} options={metricOptions} />
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
