import { BarChartOutlined } from '@ant-design/icons';
import { Card, Col, Empty, Progress, Row, Space, Table, Tabs, Typography, Select } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useMemo, useRef, useState } from 'react';
import * as echarts from 'echarts';
import {
  missionTypeDefinitions,
  taskExecutions,
  type MissionTypeKey,
  type TaskExecution,
  analyticsDefinitions
} from '../data/mock';

interface ChartSeriesPoint {
  timestamp: string;
  value: number | null;
}

interface ChartSeries {
  name: string;
  data: ChartSeriesPoint[];
}

interface ChartConfig {
  title: string;
  description?: string;
  series: ChartSeries[];
}

function useTaskSelection(tasks: TaskExecution[]) {
  const defaultSelection = useMemo(
    () => tasks.slice(0, Math.min(tasks.length, 50)).map(task => task.id),
    [tasks]
  );
  const [selectedIds, setSelectedIds] = useState<string[]>(defaultSelection);

  useEffect(() => {
    setSelectedIds(defaultSelection);
  }, [defaultSelection]);

  const selectedTasks = useMemo(
    () => tasks.filter(task => selectedIds.includes(task.id)),
    [selectedIds, tasks]
  );

  return { selectedIds, setSelectedIds, selectedTasks };
}

const MetricLineChart = ({ config }: { config: ChartConfig }) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const chartInstanceRef = useRef<echarts.ECharts | null>(null);

  const timestamps = useMemo(() => {
    return Array.from(
      new Set(config.series.flatMap(series => series.data.map(point => point.timestamp)))
    );
  }, [config]);

  useEffect(() => {
    if (!containerRef.current) return;
    chartInstanceRef.current = echarts.init(containerRef.current);
    const resize = () => chartInstanceRef.current?.resize();
    window.addEventListener('resize', resize);
    return () => {
      window.removeEventListener('resize', resize);
      chartInstanceRef.current?.dispose();
    };
  }, []);

  useEffect(() => {
    if (!chartInstanceRef.current) return;
    chartInstanceRef.current.setOption({
      tooltip: { trigger: 'axis' },
      legend: { top: 0 },
      xAxis: {
        type: 'category',
        data: timestamps,
        boundaryGap: false
      },
      yAxis: {
        type: 'value'
      },
      series: config.series.map(series => ({
        name: series.name,
        type: 'line',
        smooth: true,
        data: timestamps.map(timestamp => {
          const point = series.data.find(item => item.timestamp === timestamp);
          return point ? point.value : null;
        })
      }))
    });
  }, [config, timestamps]);

  return <div ref={containerRef} style={{ height: 260 }} />;
};

const TypeAnalyticsPanel = ({ typeKey }: { typeKey: MissionTypeKey }) => {
  const definition = missionTypeDefinitions[typeKey];
  const tasks = useMemo(() => taskExecutions.filter(task => task.missionType === typeKey), [typeKey]);
  const { selectedIds, setSelectedIds, selectedTasks } = useTaskSelection(tasks);
  const analysisTasks = selectedTasks.length ? selectedTasks : tasks;
  const sortedTasks = useMemo(
    () => [...analysisTasks].sort((a, b) => a.completedAt.localeCompare(b.completedAt)),
    [analysisTasks]
  );

  const chartDefinitions = analyticsDefinitions[typeKey] ?? [];
  const chartConfigs: ChartConfig[] = chartDefinitions.map(def => ({
    title: def.title,
    description: def.description,
    series: def.series.map(seriesDef => ({
      name: seriesDef.name,
      data: sortedTasks.map(task => ({
        timestamp: task.completedAt,
        value: task.metrics[seriesDef.metric]?.[seriesDef.field] ?? null
      }))
    }))
  }));

  const alertTriggered = sortedTasks.filter(task => {
    if (typeKey === '森林火情巡查') {
      return (task.metrics['地表温度']?.max ?? 0) > 63;
    }
    if (typeKey === '空气质量剖面') {
      return (task.metrics['PM2.5']?.max ?? 0) > 125;
    }
    if (typeKey === '城市热岛监测') {
      return (task.metrics['地表温度']?.avg ?? 0) > 32;
    }
    if (typeKey === '土地利用变化复拍') {
      return (task.metrics['疑似变化']?.count ?? 0) > 35;
    }
    if (typeKey === '林业健康监测') {
      return (task.metrics['病害疑似面积']?.value ?? 0) > 10;
    }
    return false;
  }).length;

  const followupSuggestion = sortedTasks.length
    ? `优先复查 ${sortedTasks[0].location} 等高风险区域，累计 ${sortedTasks.length} 条任务。`
    : '暂无任务可分析，可创建任务后再试。';
  const ruleSuggestion = alertTriggered
    ? `检测到 ${alertTriggered} 条任务超出阈值，建议针对 ${definition.metrics[0].label} 降低阈值或缩短采样周期。`
    : '当前未触发告警，可保持现有阈值策略。';

  const resultRows = sortedTasks.map(task => {
    const metrics: Record<string, number | null> = {};
    chartDefinitions.forEach(def =>
      def.series.forEach(seriesDef => {
        const key = `${seriesDef.metric}-${seriesDef.field}`;
        metrics[key] = task.metrics[seriesDef.metric]?.[seriesDef.field] ?? null;
      })
    );
    return {
      key: task.id,
      timestamp: task.completedAt,
      missionName: task.missionName,
      location: task.location,
      metrics
    };
  });

  const resultColumns: ColumnsType<{
    key: string;
    timestamp: string;
    missionName: string;
    location: string;
    metrics: Record<string, number | null>;
  }> = [
    { title: '任务完成时间', dataIndex: 'timestamp', key: 'timestamp' },
    { title: '任务名称', dataIndex: 'missionName', key: 'missionName' },
    { title: '区域', dataIndex: 'location', key: 'location' },
    ...chartDefinitions.flatMap(config =>
      config.series.map(series => {
        const metricKey = `${series.metric}-${series.field}`;
        return {
          title: `${series.name}`,
          key: metricKey,
          render: (_: unknown, row: { metrics: Record<string, number | null> }) =>
            row.metrics[metricKey] ?? '—'
        };
      })
    )
  ];

  return (
    <Space direction="vertical" style={{ width: '100%' }} size="large">
      <Card>
        <Typography.Text>选择任务数据（默认取最近 50 条）参与分析：</Typography.Text>
        <Select
          mode="multiple"
          value={selectedIds}
          onChange={setSelectedIds}
          style={{ minWidth: 260, marginTop: 12 }}
          placeholder="选择任务"
          options={tasks.map(task => ({
            value: task.id,
            label: `${task.missionName} · ${task.completedAt}`
          }))}
          maxTagCount="responsive"
        />
      </Card>

      {chartConfigs.length ? (
        <Row gutter={[16, 16]}>
          {chartConfigs.map(config => (
            <Col xs={24} md={Math.ceil(24 / chartConfigs.length)} key={config.title}>
              <Card title={config.title}>
                {config.description ? (
                  <Typography.Text type="secondary">{config.description}</Typography.Text>
                ) : null}
                <MetricLineChart config={config} />
              </Card>
            </Col>
          ))}
        </Row>
      ) : null}
      
      <Card title="AI 分析建议决策">
        <Space direction="vertical" style={{ width: '100%' }}>
          <Typography.Text type="secondary">
            该区域后续将对接 AI 结果，仅提供占位说明。
          </Typography.Text>
          <Typography.Paragraph>
            · 异常趋势占位：系统检测到 {alertTriggered} 条潜在异常任务。<br />
            · 巡查建议占位：{followupSuggestion}<br />
            · 报警规则建议占位：{ruleSuggestion}
          </Typography.Paragraph>
        </Space>
      </Card>

      <Card title="任务检测结果">
        {resultRows.length ? (
          <Table
            rowKey="key"
            columns={resultColumns}
            dataSource={resultRows}
            pagination={false}
            scroll={{ x: 'max-content' }}
          />
        ) : (
          <Empty description="暂无该类型任务" />
        )}
      </Card>
    </Space>
  );
};

function DataAnalytics() {
  const tabItems = (Object.keys(missionTypeDefinitions) as MissionTypeKey[]).map(typeKey => ({
    key: typeKey,
    label: missionTypeDefinitions[typeKey].name,
    children: <TypeAnalyticsPanel typeKey={typeKey} />
  }));

  return (
    <Card>
      <Typography.Title level={3}>AI 数据分析中心</Typography.Title>
      <Typography.Paragraph type="secondary">
        选择多条任务数据进行智能分析，生成巡查与报警策略建议。
      </Typography.Paragraph>
      <Tabs items={tabItems} />
    </Card>
  );
}

export default DataAnalytics;
