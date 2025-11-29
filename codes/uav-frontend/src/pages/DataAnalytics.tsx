import { Card, Col, DatePicker, Empty, Form, Input, Row, Select, Space, Table, Typography, Button, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useMemo, useState } from 'react';
import dayjs from 'dayjs';
import { analyticsApi, configApi, type MetricItem, type MissionTypeItem, type MissionDataRecord } from '../services/api';

const { RangePicker } = DatePicker;

type FilterForm = {
  missionType?: string;
  uavCode?: string;
  operatorName?: string;
  missionCode?: string;
  range?: [dayjs.Dayjs, dayjs.Dayjs];
};

function DataAnalytics() {
  const [missionTypes, setMissionTypes] = useState<MissionTypeItem[]>([]);
  const [metrics, setMetrics] = useState<MetricItem[]>([]);
  const [data, setData] = useState<MissionDataRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [form] = Form.useForm<FilterForm>();

  useEffect(() => {
    configApi.missionTypes.list().then(res => setMissionTypes((res as MissionTypeItem[]) || [])).catch(() => setMissionTypes([]));
    configApi.metrics.list().then(res => setMetrics((res as MetricItem[]) || [])).catch(() => setMetrics([]));
  }, []);

  const selectedMissionType = Form.useWatch('missionType', form);

  const missionMetricDefs = useMemo(() => {
    const mt = missionTypes.find(t => t.displayName === selectedMissionType || t.typeCode === selectedMissionType || t.id?.toString() === selectedMissionType);
    if (!mt?.metricIds) return [];
    return metrics.filter(m => mt.metricIds?.includes(m.id));
  }, [missionTypes, metrics, selectedMissionType]);

  const fetchData = async () => {
    const values = await form.validateFields();
    if (!values.missionType) {
      setData([]);
      return;
    }
    setLoading(true);
    try {
      const params: any = {
        missionType: values.missionType,
        uavCode: values.uavCode || undefined,
        operatorName: values.operatorName || undefined,
        missionCode: values.missionCode || undefined
      };
      if (values.range) {
        params.from = values.range[0].toISOString();
        params.to = values.range[1].toISOString();
      }
      const res = await analyticsApi.data(params);
      setData(res || []);
    } finally {
      setLoading(false);
    }
  };

  const columns: ColumnsType<MissionDataRecord> = [
    { title: '任务编码', dataIndex: 'missionCode', key: 'missionCode' },
    { title: '任务类型', dataIndex: 'missionType', key: 'missionType' },
    { title: '飞行员', dataIndex: 'pilotName', key: 'pilotName', render: v => v || '--' },
    { title: '无人机', dataIndex: 'uavCode', key: 'uavCode', render: v => v || '--' },
    { title: '开始时间', dataIndex: 'startTime', key: 'startTime', render: v => (v ? dayjs(v).format('YYYY-MM-DD HH:mm:ss') : '--') },
    { title: '结束时间', dataIndex: 'endTime', key: 'endTime', render: v => (v ? dayjs(v).format('YYYY-MM-DD HH:mm:ss') : '--') }
  ];

  missionMetricDefs.forEach(metric => {
    columns.push({
      title: `${metric.name} (最小)`,
      key: `min-${metric.metricCode}`,
      render: (record: MissionDataRecord) => record.dataMin?.[metric.metricCode] ?? '--'
    });
    columns.push({
      title: `${metric.name} (最大)`,
      key: `max-${metric.metricCode}`,
      render: (record: MissionDataRecord) => record.dataMax?.[metric.metricCode] ?? '--'
    });
    columns.push({
      title: `${metric.name} (平均)`,
      key: `avg-${metric.metricCode}`,
      render: (record: MissionDataRecord) => record.dataAvg?.[metric.metricCode] ?? '--'
    });
  });

  return (
    <Card>
      <Typography.Title level={4}>数据采集与分析</Typography.Title>
      <Form<FilterForm> form={form} layout="vertical" initialValues={{}}>
        <Row gutter={12}>
          <Col xs={24} md={6}>
            <Form.Item
              name="missionType"
              label="任务类型（必选）"
              rules={[{ required: true, message: '请选择任务类型' }]}
            >
              <Select
                placeholder="请选择任务类型"
                options={missionTypes.map(mt => ({ label: mt.displayName, value: mt.displayName }))}
              />
            </Form.Item>
          </Col>
          <Col xs={24} md={4}>
            <Form.Item name="uavCode" label="无人机">
              <Input placeholder="按无人机编码筛选" />
            </Form.Item>
          </Col>
          <Col xs={24} md={4}>
            <Form.Item name="operatorName" label="操作人">
              <Input placeholder="操作人" />
            </Form.Item>
          </Col>
          <Col xs={24} md={4}>
            <Form.Item name="missionCode" label="任务ID/编码">
              <Input placeholder="任务编码" />
            </Form.Item>
          </Col>
          <Col xs={24} md={6}>
            <Form.Item name="range" label="时间区间">
              <RangePicker showTime style={{ width: '100%' }} />
            </Form.Item>
          </Col>
        </Row>
        <Space>
          <Button type="primary" onClick={fetchData} loading={loading}>
            查询
          </Button>
          <Tag color="blue">指标列根据任务类型自动生成</Tag>
        </Space>
      </Form>

      <div style={{ marginTop: 16 }}>
        {selectedMissionType ? (
          <Table
            rowKey="id"
            columns={columns}
            dataSource={data}
            loading={loading}
            scroll={{ x: 'max-content' }}
            locale={{ emptyText: '暂无数据' }}
          />
        ) : (
          <Empty description="请先选择任务类型" />
        )}
      </div>
    </Card>
  );
}

export default DataAnalytics;
