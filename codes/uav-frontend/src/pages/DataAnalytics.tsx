import {
  BarChartOutlined,
  DownloadOutlined,
  LineChartOutlined,
  RadarChartOutlined,
  ReloadOutlined
} from '@ant-design/icons';
import {
  Alert,
  Button,
  Card,
  Col,
  DatePicker,
  Empty,
  Form,
  Input,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
  Typography,
  message
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import * as echarts from 'echarts';
import type { EChartsOption } from 'echarts';
import { useEffect, useMemo, useRef, useState } from 'react';
import {
  alertApi,
  analyticsApi,
  type AlertRecord,
  type AnalyticsReplayDto,
  type AnalyticsReplayEventDto,
  type AnalyticsReplaySampleDto,
  type AnalyticsTimeSeriesDto,
  configApi,
  type AnalyticsDefinitionDto,
  type MetricItem,
  type MissionComparisonDto,
  type MissionDataRecord,
  missionApi,
  type MissionTypeItem,
  type TaskExecutionDto
} from '../services/api';
import { useAuth } from '../context/AuthContext';

const { RangePicker } = DatePicker;

const DERIVED_DURATION = 'derived::durationMinutes';
const DERIVED_SPEED = 'derived::avgSpeedKmh';
const DERIVED_BATTERY = 'derived::batteryConsumption';
const DERIVED_ALERT = 'derived::alertCount';
const DERIVED_SUCCESS = 'derived::successRate';

type FilterForm = {
  missionType?: string;
  uavCode?: string;
  operatorName?: string;
  missionCode?: string;
  range?: [dayjs.Dayjs, dayjs.Dayjs];
};

type SeriesConfig = {
  name: string;
  dataKey: string;
};

type WidgetConfig = {
  chartType?: 'line' | 'bar';
  series?: SeriesConfig[];
};

type ParsedTaskExecution = TaskExecutionDto & {
  metricsMap: Record<string, any>;
};

function formatMetric(value?: number | null, suffix = '', digits = 2) {
  if (value == null || Number.isNaN(value)) {
    return '--';
  }
  return `${value.toFixed(digits)}${suffix}`;
}

function formatDateTime(value?: string) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '--';
}

function formatMissionStatus(status?: string) {
  if (!status) {
    return '--';
  }
  if (status === 'COMPLETED') {
    return '已完成';
  }
  if (status === 'RUNNING') {
    return '运行中';
  }
  if (status === 'INTERRUPTED') {
    return '已中断';
  }
  if (status === 'PREEMPTED') {
    return '已抢占';
  }
  return status;
}

function renderProcessTag(processed: boolean) {
  return processed ? <Tag color="default">已处理</Tag> : <Tag color="red">未处理</Tag>;
}

function renderLinkageTag(value?: string) {
  if (!value) return <Tag>--</Tag>;
  if (value === 'SUCCESS') return <Tag color="green">成功</Tag>;
  if (value === 'PARTIAL') return <Tag color="gold">部分成功</Tag>;
  if (value === 'FAILED') return <Tag color="red">失败</Tag>;
  if (value === 'SKIPPED') return <Tag>跳过</Tag>;
  if (value === 'PLACEHOLDER') return <Tag color="blue">通知预留</Tag>;
  if (value === 'PENDING') return <Tag color="processing">处理中</Tag>;
  return <Tag>{value}</Tag>;
}

function toNumber(value: any): number | null {
  if (value == null || value === '') {
    return null;
  }
  if (typeof value === 'number') {
    return Number.isFinite(value) ? value : null;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

function parseMetrics(metrics: string): Record<string, any> {
  if (!metrics) {
    return {};
  }
  try {
    const parsed = JSON.parse(metrics);
    return parsed && typeof parsed === 'object' ? parsed : {};
  } catch {
    return {};
  }
}

function parseWidgetConfig(seriesConfig: string): WidgetConfig {
  if (!seriesConfig) {
    return {};
  }
  try {
    const parsed = JSON.parse(seriesConfig);
    return parsed && typeof parsed === 'object' ? parsed : {};
  } catch {
    return {};
  }
}

function downloadFile(filename: string, content: BlobPart, type: string) {
  const blob = new Blob([content], { type });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();
  window.URL.revokeObjectURL(url);
}

function escapeCsvCell(value: any) {
  const text = value == null ? '' : String(value);
  if (/[",\n]/.test(text)) {
    return `"${text.replace(/"/g, '""')}"`;
  }
  return text;
}

function AnalyticsChart({
  option,
  height = 320
}: {
  option: EChartsOption;
  height?: number;
}) {
  const ref = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!ref.current) {
      return;
    }
    const chart = echarts.getInstanceByDom(ref.current) || echarts.init(ref.current);
    chart.setOption(option, true);
    const handleResize = () => chart.resize();
    window.addEventListener('resize', handleResize);
    return () => {
      window.removeEventListener('resize', handleResize);
      chart.dispose();
    };
  }, [option]);

  return <div ref={ref} style={{ height, width: '100%' }} />;
}

function DataAnalytics() {
  const { currentUser } = useAuth();
  const [missionTypes, setMissionTypes] = useState<MissionTypeItem[]>([]);
  const [metrics, setMetrics] = useState<MetricItem[]>([]);
  const [definitions, setDefinitions] = useState<AnalyticsDefinitionDto[]>([]);
  const [executions, setExecutions] = useState<TaskExecutionDto[]>([]);
  const [data, setData] = useState<MissionDataRecord[]>([]);
  const [compareSelection, setCompareSelection] = useState<string[]>([]);
  const [compareData, setCompareData] = useState<MissionComparisonDto[]>([]);
  const [selectedReplayMission, setSelectedReplayMission] = useState<string>();
  const [replayMetrics, setReplayMetrics] = useState<string[]>([]);
  const [replayData, setReplayData] = useState<AnalyticsReplayDto | null>(null);
  const [timeseriesData, setTimeseriesData] = useState<AnalyticsTimeSeriesDto | null>(null);
  const [replayAlertRecords, setReplayAlertRecords] = useState<AlertRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [compareLoading, setCompareLoading] = useState(false);
  const [replayLoading, setReplayLoading] = useState(false);
  const [form] = Form.useForm<FilterForm>();
  const replayAlertsRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    configApi.missionTypes
      .list()
      .then(async res => {
        const missionTypeItems = (res as MissionTypeItem[]) || [];
        if (missionTypeItems.length) {
          setMissionTypes(missionTypeItems);
          return;
        }
        const missions = await missionApi.list();
        const fallbackMissionTypes = Array.from(new Set(missions.map(item => item.missionType).filter(Boolean))).map(
          (missionType, index) => ({
            id: -(index + 1),
            typeCode: missionType,
            displayName: missionType,
            description: '根据已有任务自动识别'
          })
        );
        setMissionTypes(fallbackMissionTypes);
      })
      .catch(() => setMissionTypes([]));
    configApi.metrics
      .list()
      .then(res => setMetrics((res as MetricItem[]) || []))
      .catch(() => setMetrics([]));
  }, []);

  useEffect(() => {
    if (currentUser?.role === 'superadmin') {
      return;
    }
    form.setFieldsValue({ operatorName: currentUser?.name });
  }, [currentUser, form]);

  const selectedMissionType = Form.useWatch('missionType', form);

  const missionMetricDefs = useMemo(() => {
    const missionType = missionTypes.find(
      item =>
        item.displayName === selectedMissionType ||
        item.typeCode === selectedMissionType ||
        item.id?.toString() === selectedMissionType
    );
    if (!missionType?.metricIds?.length) {
      return [];
    }
    return metrics.filter(metric => missionType.metricIds?.includes(metric.id));
  }, [missionTypes, metrics, selectedMissionType]);

  const parsedExecutions = useMemo<ParsedTaskExecution[]>(
    () => executions.map(item => ({ ...item, metricsMap: parseMetrics(item.metrics) })),
    [executions]
  );

  const parsedDefinitions = useMemo(
    () =>
      definitions.map(item => ({
        ...item,
        config: parseWidgetConfig(item.seriesConfig)
      })),
    [definitions]
  );

  const compareOptions = useMemo(
    () =>
      parsedExecutions.map(item => ({
        label: `${item.missionName} · ${item.executionCode}`,
        value: item.executionCode
      })),
    [parsedExecutions]
  );

  const replayOptions = compareOptions;

  useEffect(() => {
    const validCodes = new Set(compareOptions.map(item => item.value));
    setCompareSelection(prev => prev.filter(item => validCodes.has(item)));
    setCompareData(prev => prev.filter(item => validCodes.has(item.missionCode)));
  }, [compareOptions]);

  const averageForKey = (dataKey: string) => {
    const values = parsedExecutions
      .map(item => toNumber(item.metricsMap[dataKey]))
      .filter((item): item is number => item != null);
    if (!values.length) {
      return null;
    }
    return values.reduce((sum, item) => sum + item, 0) / values.length;
  };

  const peakExecution = useMemo<{ label: string; value: number } | null>(() => {
    let best: { label: string; value: number } | null = null;
    parsedExecutions.forEach(item => {
      Object.entries(item.metricsMap).forEach(([key, value]) => {
        if (!key.startsWith('max::')) {
          return;
        }
        const numeric = toNumber(value);
        if (numeric == null) {
          return;
        }
        if (!best || numeric > best.value) {
          best = { label: `${item.missionName} · ${key.replace('max::', '')}`, value: numeric };
        }
      });
    });
    return best;
  }, [parsedExecutions]);

  const summaryCards = useMemo(
    () => [
      {
        title: '样本任务',
        value: parsedExecutions.length,
        suffix: '个',
        precision: 0
      },
      {
        title: '平均任务时长',
        value: averageForKey(DERIVED_DURATION),
        suffix: ' 分钟'
      },
      {
        title: '平均飞行速度',
        value: averageForKey(DERIVED_SPEED),
        suffix: ' km/h'
      },
      {
        title: '平均告警次数',
        value: averageForKey(DERIVED_ALERT),
        suffix: ' 次'
      },
      {
        title: '执行成功率',
        value: averageForKey(DERIVED_SUCCESS),
        suffix: '%'
      },
      {
        title: '平均电量消耗',
        value: averageForKey(DERIVED_BATTERY),
        suffix: '%'
      }
    ],
    [parsedExecutions]
  );

  const fetchData = async () => {
    try {
      const values = await form.validateFields();
      if (!values.missionType) {
        setDefinitions([]);
        setExecutions([]);
        setData([]);
        return;
      }
      setLoading(true);
      const from = values.range?.[0]?.toISOString();
      const to = values.range?.[1]?.toISOString();
      const params = {
        missionType: values.missionType,
        uavCode: values.uavCode || undefined,
        operatorName: values.operatorName || undefined,
        missionCode: values.missionCode || undefined,
        from,
        to
      };
      const [definitionResult, executionResult, dataResult] = await Promise.all([
        analyticsApi.definitions(values.missionType),
        analyticsApi.executions(values.missionType, from, to),
        analyticsApi.data(params)
      ]);
      setDefinitions(definitionResult || []);
      setExecutions(executionResult || []);
      setData(dataResult || []);
      setCompareSelection([]);
      setCompareData([]);
      const defaultReplayMission = executionResult?.[0]?.executionCode;
      setSelectedReplayMission(defaultReplayMission);
      setReplayMetrics([]);
      if (!defaultReplayMission) {
        setReplayData(null);
        setTimeseriesData(null);
      }
    } catch (error: any) {
      if (error?.errorFields) {
        return;
      }
      message.error(error?.message || '加载分析数据失败');
    } finally {
      setLoading(false);
    }
  };

  const loadReplay = async (missionCode: string, metrics?: string[]) => {
    try {
      setReplayLoading(true);
      const [replayResult, timeseriesResult] = await Promise.all([
        analyticsApi.replay(missionCode),
        analyticsApi.timeseries(missionCode, metrics?.length ? metrics : undefined)
      ]);
      setReplayData(replayResult || null);
      setTimeseriesData(timeseriesResult || null);
      const alertRecords = await alertApi.records.list(undefined, [missionCode]);
      setReplayAlertRecords(alertRecords || []);
      if ((!metrics || !metrics.length) && timeseriesResult?.series?.length) {
        setReplayMetrics(timeseriesResult.series.map(item => item.metricCode));
      }
    } catch (error: any) {
      message.error(error?.message || '加载任务复盘失败');
    } finally {
      setReplayLoading(false);
    }
  };

  const handleCompare = async () => {
    if (compareSelection.length < 2 || compareSelection.length > 5) {
      message.warning('请选择 2 到 5 个任务进行对比');
      return;
    }
    try {
      setCompareLoading(true);
      const result = await analyticsApi.compare(compareSelection);
      setCompareData(result || []);
    } catch (error: any) {
      message.error(error?.message || '加载任务对比失败');
    } finally {
      setCompareLoading(false);
    }
  };

  useEffect(() => {
    if (!selectedReplayMission) {
      setReplayData(null);
      setTimeseriesData(null);
      setReplayAlertRecords([]);
      return;
    }
    loadReplay(selectedReplayMission).catch(() => undefined);
  }, [selectedReplayMission]);

  const detailColumns = useMemo<ColumnsType<MissionDataRecord>>(() => {
    const columns: ColumnsType<MissionDataRecord> = [
      { title: '任务编码', dataIndex: 'missionCode', key: 'missionCode', fixed: 'left', width: 160 },
      { title: '任务类型', dataIndex: 'missionType', key: 'missionType', width: 140 },
      {
        title: '飞行员',
        dataIndex: 'pilotName',
        key: 'pilotName',
        width: 120,
        render: value => value || '--'
      },
      {
        title: '无人机',
        dataIndex: 'uavCode',
        key: 'uavCode',
        width: 140,
        render: value => value || '--'
      },
      {
        title: '开始时间',
        dataIndex: 'startTime',
        key: 'startTime',
        width: 180,
        render: value => (value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '--')
      },
      {
        title: '结束时间',
        dataIndex: 'endTime',
        key: 'endTime',
        width: 180,
        render: value => (value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '--')
      }
    ];

    missionMetricDefs.forEach(metric => {
      columns.push({
        title: `${metric.name} (最小)`,
        key: `min-${metric.metricCode}`,
        width: 140,
        render: record => record.dataMin?.[metric.metricCode] ?? '--'
      });
      columns.push({
        title: `${metric.name} (最大)`,
        key: `max-${metric.metricCode}`,
        width: 140,
        render: record => record.dataMax?.[metric.metricCode] ?? '--'
      });
      columns.push({
        title: `${metric.name} (平均)`,
        key: `avg-${metric.metricCode}`,
        width: 140,
        render: record => record.dataAvg?.[metric.metricCode] ?? '--'
      });
    });

    return columns;
  }, [missionMetricDefs]);

  const chartCards = useMemo(() => {
    return parsedDefinitions
      .map(item => {
        const config = item.config;
        const series = (config.series || [])
          .map(seriesItem => {
            const values = parsedExecutions.map(execution => toNumber(execution.metricsMap[seriesItem.dataKey]));
            if (!values.some(value => value != null)) {
              return null;
            }
            return {
              name: seriesItem.name,
              type: config.chartType === 'bar' ? 'bar' : 'line',
              smooth: config.chartType !== 'bar',
              data: values.map(value => (value == null ? null : Number(value.toFixed(2))))
            };
          })
          .filter(
            (
              value
            ): value is {
              name: string;
              type: 'line' | 'bar';
              smooth: boolean;
              data: Array<number | null>;
            } => value != null
          );

        if (!series.length) {
          return null;
        }

        const option: EChartsOption = {
          color: ['#0f766e', '#0284c7', '#d97706', '#dc2626'],
          tooltip: { trigger: 'axis' },
          legend: { top: 0 },
          grid: { left: 48, right: 20, top: 52, bottom: 28 },
          xAxis: {
            type: 'category',
            axisLabel: { rotate: 24 },
            data: parsedExecutions.map(execution =>
              `${dayjs(execution.completedAt).format('MM-DD')}\n${execution.missionName}`
            )
          },
          yAxis: { type: 'value', splitLine: { lineStyle: { color: '#dbe4f0' } } },
          series
        };

        return {
          key: item.title,
          title: item.title,
          description: item.description,
          option,
          chartType: config.chartType
        };
      })
      .filter(Boolean) as Array<{
      key: string;
      title: string;
      description?: string;
      option: EChartsOption;
      chartType?: 'line' | 'bar';
    }>;
  }, [parsedDefinitions, parsedExecutions]);

  const compareChartOption = useMemo<EChartsOption | null>(() => {
    if (compareData.length < 2) {
      return null;
    }
    const dimensions = [
      { key: 'durationMinutes', label: '时长', max: 0 },
      { key: 'avgSpeedKmh', label: '速度', max: 0 },
      { key: 'batteryConsumption', label: '电耗', max: 0 },
      { key: 'alertCount', label: '告警', max: 0 },
      { key: 'successRate', label: '成功率', max: 0 }
    ];
    dimensions.forEach(item => {
      item.max =
        compareData.reduce((max, row) => {
          const value = toNumber((row as any)[item.key]);
          return Math.max(max, value ?? 0);
        }, 0) || 1;
    });
    return {
      color: ['#0f766e', '#0284c7', '#d97706', '#dc2626', '#7c3aed'],
      tooltip: { trigger: 'item' },
      legend: { top: 0 },
      radar: {
        radius: '62%',
        indicator: dimensions.map(item => ({ name: item.label, max: 100 })),
        splitArea: { areaStyle: { color: ['#f8fbff', '#eef5fb'] } }
      },
      series: [
        {
          type: 'radar',
          data: compareData.map(item => ({
            name: item.missionName,
            value: dimensions.map(dimension => {
              const value = toNumber((item as any)[dimension.key]);
              if (value == null) {
                return 0;
              }
              return Number(((value / dimension.max) * 100).toFixed(2));
            })
          }))
        }
      ]
    };
  }, [compareData]);

  const compareColumns = useMemo<ColumnsType<MissionComparisonDto>>(
    () => [
      { title: '任务', dataIndex: 'missionName', key: 'missionName', width: 180, fixed: 'left' },
      { title: '编码', dataIndex: 'missionCode', key: 'missionCode', width: 150 },
      {
        title: '状态',
        dataIndex: 'status',
        key: 'status',
        width: 120,
        render: value => {
          if (!value) return '--';
          if (value === 'COMPLETED') return <Tag color="green">已完成</Tag>;
          if (value === 'INTERRUPTED') return <Tag color="orange">已中断</Tag>;
          if (value === 'RUNNING') return <Tag color="processing">运行中</Tag>;
          return <Tag>{value}</Tag>;
        }
      },
      { title: '时长(分钟)', dataIndex: 'durationMinutes', key: 'durationMinutes', width: 130, render: value => formatMetric(value) },
      { title: '平均速度(km/h)', dataIndex: 'avgSpeedKmh', key: 'avgSpeedKmh', width: 140, render: value => formatMetric(value) },
      { title: '电量消耗(%)', dataIndex: 'batteryConsumption', key: 'batteryConsumption', width: 130, render: value => formatMetric(value) },
      { title: '告警次数', dataIndex: 'alertCount', key: 'alertCount', width: 110, render: value => formatMetric(value, '', 0) },
      { title: '执行成功率', dataIndex: 'successRate', key: 'successRate', width: 130, render: value => formatMetric(value, '%') }
    ],
    []
  );

  const replaySummaryCards = useMemo(
    () => [
      {
        title: '复盘时长',
        value: replayData?.durationMinutes ?? null,
        suffix: ' 分钟'
      },
      {
        title: '航线里程',
        value: replayData?.distanceKm ?? null,
        suffix: ' km'
      },
      {
        title: '原始采样',
        value: replayData?.sampleCount ?? null,
        suffix: ' 条',
        precision: 0
      },
      {
        title: '触发报警条数',
        value: replayAlertRecords.length,
        suffix: ' 条',
        precision: 0,
        clickable: true
      }
    ],
    [replayAlertRecords.length, replayData]
  );

  const timeseriesChartOption = useMemo<EChartsOption | null>(() => {
    if (!timeseriesData?.series?.length) {
      return null;
    }
    const xAxis = Array.from(
      new Set(timeseriesData.series.flatMap(series => series.points.map(point => point.timestamp)))
    );
    return {
      color: ['#0f766e', '#0284c7', '#d97706', '#dc2626', '#7c3aed'],
      tooltip: { trigger: 'axis' },
      legend: { top: 0 },
      grid: { left: 48, right: 20, top: 52, bottom: 28 },
      xAxis: {
        type: 'category',
        data: xAxis.map(item => dayjs(item).format('HH:mm:ss'))
      },
      yAxis: { type: 'value', splitLine: { lineStyle: { color: '#dbe4f0' } } },
      series: timeseriesData.series.map(series => {
        const pointMap = new Map(series.points.map(point => [point.timestamp, point.value]));
        return {
          name: `${series.displayName}${series.unit ? ` (${series.unit})` : ''}`,
          type: 'line',
          smooth: true,
          showSymbol: xAxis.length <= 24,
          data: xAxis.map(timestamp => {
            const value = pointMap.get(timestamp);
            return value == null ? null : Number(value.toFixed(2));
          })
        };
      })
    };
  }, [timeseriesData]);

  const rawSampleChartOption = useMemo<EChartsOption | null>(() => {
    if (!replayData?.samples?.length) {
      return null;
    }
    return {
      color: ['#0284c7', '#d97706', '#16a34a'],
      tooltip: { trigger: 'axis' },
      legend: { top: 0 },
      grid: { left: 48, right: 20, top: 52, bottom: 28 },
      xAxis: {
        type: 'category',
        data: replayData.samples.map(sample => dayjs(sample.reportedAt).format('HH:mm:ss'))
      },
      yAxis: { type: 'value', splitLine: { lineStyle: { color: '#dbe4f0' } } },
      series: [
        {
          name: '电量(%)',
          type: 'line',
          smooth: true,
          data: replayData.samples.map(sample => sample.batteryPercent ?? null)
        },
        {
          name: '速度(m/s)',
          type: 'line',
          smooth: true,
          data: replayData.samples.map(sample => sample.velocityMs ?? null)
        },
        {
          name: '高度(m)',
          type: 'line',
          smooth: true,
          data: replayData.samples.map(sample => sample.altitude ?? null)
        }
      ]
    };
  }, [replayData]);

  const replayTimelineColumns = useMemo<ColumnsType<AnalyticsReplayEventDto>>(
    () => [
      {
        title: '时间',
        dataIndex: 'occurredAt',
        key: 'occurredAt',
        width: 170,
        render: value => (value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '--')
      },
      {
        title: '类别',
        dataIndex: 'category',
        key: 'category',
        width: 110,
        render: value => <Tag color={value === 'ALERT' ? 'red' : 'blue'}>{value}</Tag>
      },
      { title: '事件', dataIndex: 'title', key: 'title', width: 160 },
      {
        title: '详情',
        dataIndex: 'description',
        key: 'description',
        render: value => value || '--'
      }
    ],
    []
  );

  const replaySampleColumns = useMemo<ColumnsType<AnalyticsReplaySampleDto>>(
    () => [
      {
        title: '采样时间',
        dataIndex: 'reportedAt',
        key: 'reportedAt',
        width: 170,
        render: value => dayjs(value).format('YYYY-MM-DD HH:mm:ss')
      },
      {
        title: '位置',
        key: 'position',
        width: 220,
        render: record =>
          record.lat != null && record.lng != null
            ? `${record.lat.toFixed(4)}, ${record.lng.toFixed(4)}`
            : '--'
      },
      {
        title: '电量(%)',
        dataIndex: 'batteryPercent',
        key: 'batteryPercent',
        width: 110,
        render: value => formatMetric(value)
      },
      {
        title: '速度(m/s)',
        dataIndex: 'velocityMs',
        key: 'velocityMs',
        width: 120,
        render: value => formatMetric(value)
      },
      {
        title: '原始指标',
        dataIndex: 'metrics',
        key: 'metrics',
        render: value =>
          value && Object.keys(value).length
            ? Object.entries(value)
                .map(([key, metricValue]) => `${key}: ${metricValue}`)
                .join(' | ')
            : '--'
      }
    ],
    []
  );

  const replayAlertColumns = useMemo<ColumnsType<AlertRecord>>(
    () => [
      {
        title: '触发时间',
        dataIndex: 'triggeredAt',
        key: 'triggeredAt',
        width: 170,
        render: value => formatDateTime(value)
      },
      {
        title: '指标',
        dataIndex: 'metricCode',
        key: 'metricCode',
        width: 130,
        render: value => value || '--'
      },
      {
        title: '触发值',
        dataIndex: 'metricValue',
        key: 'metricValue',
        width: 120,
        render: value => (value == null ? '--' : value)
      },
      {
        title: '联动状态',
        dataIndex: 'linkageStatus',
        key: 'linkageStatus',
        width: 120,
        render: value => renderLinkageTag(value)
      },
      {
        title: '通知状态',
        dataIndex: 'notificationStatus',
        key: 'notificationStatus',
        width: 120,
        render: value => renderLinkageTag(value)
      },
      {
        title: '处理状态',
        dataIndex: 'processed',
        key: 'processed',
        width: 100,
        render: value => renderProcessTag(Boolean(value))
      },
      {
        title: '联动摘要',
        dataIndex: 'linkageSummary',
        key: 'linkageSummary',
        render: value => (
          <Typography.Text style={{ maxWidth: 300 }} ellipsis={{ tooltip: value || '--' }}>
            {value || '--'}
          </Typography.Text>
        )
      }
    ],
    []
  );

  const scrollToReplayAlerts = () => {
    replayAlertsRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  const exportReplayCsv = () => {
    if (!replayData) {
      message.info('请先选择要导出的复盘任务');
      return;
    }
    const rows: any[][] = [
      ['任务名称', replayData.missionName],
      ['任务编码', replayData.missionCode],
      ['任务类型', replayData.missionType],
      ['任务状态', formatMissionStatus(replayData.status)],
      ['无人机', replayData.uavCode || '--'],
      ['开始时间', formatDateTime(replayData.startTime)],
      ['结束时间', formatDateTime(replayData.endTime)],
      ['复盘时长(分钟)', replayData.durationMinutes ?? ''],
      ['航线里程(km)', replayData.distanceKm ?? ''],
      ['原始采样数', replayData.sampleCount ?? ''],
      ['触发报警条数', replayAlertRecords.length],
      [],
      ['任务时间轴'],
      ['时间', '类别', '事件', '详情'],
      ...replayData.timeline.map(item => [
        formatDateTime(item.occurredAt),
        item.category,
        item.title,
        item.description || ''
      ]),
      [],
      ['报警记录'],
      ['触发时间', '指标', '触发值', '联动状态', '通知状态', '处理状态', '联动摘要'],
      ...replayAlertRecords.map(item => [
        formatDateTime(item.triggeredAt),
        item.metricCode || '',
        item.metricValue ?? '',
        item.linkageStatus || '',
        item.notificationStatus || '',
        item.processed ? '已处理' : '未处理',
        item.linkageSummary || ''
      ]),
      [],
      ['原始采样'],
      ['采样时间', '纬度', '经度', '高度(m)', '电量(%)', '速度(m/s)', '原始指标']
    ];
    replayData.samples.forEach(sample => {
      rows.push([
        formatDateTime(sample.reportedAt),
        sample.lat ?? '',
        sample.lng ?? '',
        sample.altitude ?? '',
        sample.batteryPercent ?? '',
        sample.velocityMs ?? '',
        sample.metrics && Object.keys(sample.metrics).length
          ? Object.entries(sample.metrics)
              .map(([key, value]) => `${key}: ${value}`)
              .join(' | ')
          : ''
      ]);
    });
    const csv = `\ufeff${rows.map(row => row.map(escapeCsvCell).join(',')).join('\n')}`;
    downloadFile(`replay-${replayData.missionCode}-${dayjs().format('YYYYMMDD-HHmmss')}.csv`, csv, 'text/csv;charset=utf-8;');
  };

  const exportReplayExcel = () => {
    if (!replayData) {
      message.info('请先选择要导出的复盘任务');
      return;
    }
    const timelineRows = replayData.timeline
      .map(
        item => `
          <tr>
            <td>${formatDateTime(item.occurredAt)}</td>
            <td>${item.category}</td>
            <td>${item.title}</td>
            <td>${item.description || ''}</td>
          </tr>`
      )
      .join('');
    const alertRows = replayAlertRecords
      .map(
        item => `
          <tr>
            <td>${formatDateTime(item.triggeredAt)}</td>
            <td>${item.metricCode || ''}</td>
            <td>${item.metricValue ?? ''}</td>
            <td>${item.linkageStatus || ''}</td>
            <td>${item.notificationStatus || ''}</td>
            <td>${item.processed ? '已处理' : '未处理'}</td>
            <td>${item.linkageSummary || ''}</td>
          </tr>`
      )
      .join('');
    const sampleRows = replayData.samples
      .map(
        sample => `
          <tr>
            <td>${formatDateTime(sample.reportedAt)}</td>
            <td>${sample.lat ?? ''}</td>
            <td>${sample.lng ?? ''}</td>
            <td>${sample.altitude ?? ''}</td>
            <td>${sample.batteryPercent ?? ''}</td>
            <td>${sample.velocityMs ?? ''}</td>
            <td>${
              sample.metrics && Object.keys(sample.metrics).length
                ? Object.entries(sample.metrics)
                    .map(([key, value]) => `${key}: ${value}`)
                    .join(' | ')
                : ''
            }</td>
          </tr>`
      )
      .join('');
    const html = `
      <html xmlns:o="urn:schemas-microsoft-com:office:office"
            xmlns:x="urn:schemas-microsoft-com:office:excel"
            xmlns="http://www.w3.org/TR/REC-html40">
      <head>
        <meta charset="utf-8" />
        <style>
          body { font-family: Arial, sans-serif; }
          table { border-collapse: collapse; margin-bottom: 16px; width: 100%; }
          th, td { border: 1px solid #cbd5e1; padding: 6px 8px; }
          th { background: #e2e8f0; }
          h2 { margin: 16px 0 8px; }
        </style>
      </head>
      <body>
        <h2>任务复盘概览</h2>
        <table>
          <tr><th>字段</th><th>内容</th></tr>
          <tr><td>任务名称</td><td>${replayData.missionName}</td></tr>
          <tr><td>任务编码</td><td>${replayData.missionCode}</td></tr>
          <tr><td>任务类型</td><td>${replayData.missionType}</td></tr>
          <tr><td>任务状态</td><td>${formatMissionStatus(replayData.status)}</td></tr>
          <tr><td>无人机</td><td>${replayData.uavCode || '--'}</td></tr>
          <tr><td>开始时间</td><td>${formatDateTime(replayData.startTime)}</td></tr>
          <tr><td>结束时间</td><td>${formatDateTime(replayData.endTime)}</td></tr>
          <tr><td>复盘时长(分钟)</td><td>${replayData.durationMinutes ?? ''}</td></tr>
          <tr><td>航线里程(km)</td><td>${replayData.distanceKm ?? ''}</td></tr>
          <tr><td>原始采样数</td><td>${replayData.sampleCount ?? ''}</td></tr>
          <tr><td>触发报警条数</td><td>${replayAlertRecords.length}</td></tr>
        </table>
        <h2>任务时间轴</h2>
        <table>
          <tr><th>时间</th><th>类别</th><th>事件</th><th>详情</th></tr>
          ${timelineRows}
        </table>
        <h2>报警记录</h2>
        <table>
          <tr><th>触发时间</th><th>指标</th><th>触发值</th><th>联动状态</th><th>通知状态</th><th>处理状态</th><th>联动摘要</th></tr>
          ${alertRows}
        </table>
        <h2>原始采样</h2>
        <table>
          <tr><th>采样时间</th><th>纬度</th><th>经度</th><th>高度(m)</th><th>电量(%)</th><th>速度(m/s)</th><th>原始指标</th></tr>
          ${sampleRows}
        </table>
      </body>
      </html>`;
    downloadFile(
      `replay-${replayData.missionCode}-${dayjs().format('YYYYMMDD-HHmmss')}.xls`,
      html,
      'application/vnd.ms-excel;charset=utf-8;'
    );
  };

  const exportCsv = () => {
    if (!data.length) {
      message.info('当前没有可导出的明细数据');
      return;
    }
    const headers = detailColumns.map(column => String(column.title));
    const rows = data.map(record => {
      const base = [
        record.missionCode,
        record.missionType,
        record.pilotName || '',
        record.uavCode || '',
        record.startTime ? dayjs(record.startTime).format('YYYY-MM-DD HH:mm:ss') : '',
        record.endTime ? dayjs(record.endTime).format('YYYY-MM-DD HH:mm:ss') : ''
      ];
      const metricsCells = missionMetricDefs.flatMap(metric => [
        record.dataMin?.[metric.metricCode] ?? '',
        record.dataMax?.[metric.metricCode] ?? '',
        record.dataAvg?.[metric.metricCode] ?? ''
      ]);
      return [...base, ...metricsCells];
    });
    const csv = `\ufeff${[headers, ...rows].map(row => row.map(escapeCsvCell).join(',')).join('\n')}`;
    downloadFile(`analytics-detail-${dayjs().format('YYYYMMDD-HHmmss')}.csv`, csv, 'text/csv;charset=utf-8;');
  };

  const exportExcel = () => {
    if (!data.length && !parsedExecutions.length) {
      message.info('当前没有可导出的分析报表');
      return;
    }
    const summaryRows = summaryCards
      .map(item => `<tr><td>${item.title}</td><td>${item.value == null ? '--' : Number(item.value).toFixed(item.precision ?? 2)}</td><td>${item.suffix ?? ''}</td></tr>`)
      .join('');
    const compareRows = compareData
      .map(
        item => `
          <tr>
            <td>${item.missionName}</td>
            <td>${item.missionCode}</td>
            <td>${item.status ?? ''}</td>
            <td>${item.durationMinutes ?? ''}</td>
            <td>${item.avgSpeedKmh ?? ''}</td>
            <td>${item.batteryConsumption ?? ''}</td>
            <td>${item.alertCount ?? ''}</td>
            <td>${item.successRate ?? ''}</td>
          </tr>`
      )
      .join('');
    const detailHeaders = detailColumns.map(column => `<th>${column.title}</th>`).join('');
    const detailRows = data
      .map(record => {
        const cells = [
          record.missionCode,
          record.missionType,
          record.pilotName || '',
          record.uavCode || '',
          record.startTime ? dayjs(record.startTime).format('YYYY-MM-DD HH:mm:ss') : '',
          record.endTime ? dayjs(record.endTime).format('YYYY-MM-DD HH:mm:ss') : '',
          ...missionMetricDefs.flatMap(metric => [
            record.dataMin?.[metric.metricCode] ?? '',
            record.dataMax?.[metric.metricCode] ?? '',
            record.dataAvg?.[metric.metricCode] ?? ''
          ])
        ];
        return `<tr>${cells.map(cell => `<td>${cell}</td>`).join('')}</tr>`;
      })
      .join('');
    const html = `
      <html xmlns:o="urn:schemas-microsoft-com:office:office"
            xmlns:x="urn:schemas-microsoft-com:office:excel"
            xmlns="http://www.w3.org/TR/REC-html40">
      <head>
        <meta charset="utf-8" />
        <style>
          body { font-family: Arial, sans-serif; }
          table { border-collapse: collapse; margin-bottom: 16px; width: 100%; }
          th, td { border: 1px solid #cbd5e1; padding: 6px 8px; }
          th { background: #e2e8f0; }
          h2 { margin: 16px 0 8px; }
        </style>
      </head>
      <body>
        <h2>分析概览</h2>
        <table>
          <tr><th>指标</th><th>数值</th><th>单位</th></tr>
          ${summaryRows}
        </table>
        <h2>任务对比</h2>
        <table>
          <tr><th>任务</th><th>编码</th><th>状态</th><th>时长(分钟)</th><th>平均速度(km/h)</th><th>电量消耗(%)</th><th>告警次数</th><th>执行成功率</th></tr>
          ${compareRows}
        </table>
        <h2>明细数据</h2>
        <table>
          <tr>${detailHeaders}</tr>
          ${detailRows}
        </table>
      </body>
      </html>`;
    downloadFile(
      `analytics-report-${dayjs().format('YYYYMMDD-HHmmss')}.xls`,
      html,
      'application/vnd.ms-excel;charset=utf-8;'
    );
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <Card
        className="hero-card"
        style={{
          color: '#f8fafc',
          overflow: 'hidden'
        }}
      >
        <Space direction="vertical" size={8} style={{ width: '100%' }}>
          <Typography.Title level={3} style={{ color: '#f8fafc', margin: 0 }}>
            数据采集与分析看板
          </Typography.Title>
          {peakExecution ? (
            <Alert
              style={{ marginTop: 8, background: 'rgba(255,255,255,0.08)', border: '1px solid rgba(255,255,255,0.16)' }}
              message={`当前样本峰值任务：${peakExecution.label}`}
              description={`峰值数值 ${peakExecution.value.toFixed(2)}`}
              type="info"
              showIcon
            />
          ) : null}
        </Space>
      </Card>

      <Card>
        <Form<FilterForm> form={form} layout="vertical">
          <Row gutter={12}>
            <Col xs={24} md={6}>
              <Form.Item
                name="missionType"
                label="任务类型（必选）"
                rules={[{ required: true, message: '请选择任务类型' }]}
              >
                <Select
                  placeholder="请选择任务类型"
                  options={missionTypes.map(item => ({ label: item.displayName, value: item.displayName }))}
                />
              </Form.Item>
            </Col>
            <Col xs={24} md={4}>
              <Form.Item name="uavCode" label="无人机">
                <Input placeholder="按无人机编码筛选" />
              </Form.Item>
            </Col>
            {currentUser?.role === 'superadmin' ? (
              <Col xs={24} md={4}>
                <Form.Item name="operatorName" label="操作人">
                  <Input placeholder="按操作人筛选" />
                </Form.Item>
              </Col>
            ) : null}
            <Col xs={24} md={4}>
              <Form.Item name="missionCode" label="任务编码">
                <Input placeholder="按任务编码筛选" />
              </Form.Item>
            </Col>
            <Col xs={24} md={6}>
              <Form.Item name="range" label="完成时间区间">
                <RangePicker showTime style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>
          <Space wrap>
            <Button type="primary" icon={<ReloadOutlined />} onClick={fetchData} loading={loading}>
              生成分析
            </Button>
            <Button icon={<DownloadOutlined />} onClick={exportCsv} disabled={!data.length}>
              导出 CSV
            </Button>
            <Button icon={<DownloadOutlined />} onClick={exportExcel} disabled={!data.length && !parsedExecutions.length}>
              导出 Excel
            </Button>
          </Space>
        </Form>
      </Card>

      {selectedMissionType ? (
        <>
          <Row gutter={[16, 16]}>
            {summaryCards.map(item => (
              <Col xs={24} sm={12} xl={8} xxl={4} key={item.title}>
                <Card>
                  <Statistic
                    title={item.title}
                    value={item.value == null ? undefined : Number(item.value.toFixed(item.precision ?? 2))}
                    suffix={item.suffix}
                    precision={item.precision ?? 2}
                    valueStyle={{ color: '#0f172a', fontWeight: 700 }}
                  />
                </Card>
              </Col>
            ))}
          </Row>

          <Card
            title="趋势图与指标峰值"
            extra={
              <Space>
                <Tag icon={<LineChartOutlined />} color="cyan">
                  趋势
                </Tag>
                <Tag icon={<BarChartOutlined />} color="gold">
                  峰值/分布
                </Tag>
              </Space>
            }
          >
            {chartCards.length ? (
              <Row gutter={[16, 16]}>
                {chartCards.map(card => (
                  <Col xs={24} xl={12} key={card.key}>
                    <Card
                      size="small"
                      title={card.title}
                      extra={
                        <Tag color={card.chartType === 'bar' ? 'gold' : 'cyan'}>
                          {card.chartType === 'bar' ? '柱状图' : '折线图'}
                        </Tag>
                      }
                    >
                      {card.description ? (
                        <Typography.Paragraph type="secondary" style={{ marginBottom: 12 }}>
                          {card.description}
                        </Typography.Paragraph>
                      ) : null}
                      <AnalyticsChart option={card.option} />
                    </Card>
                  </Col>
                ))}
              </Row>
            ) : (
              <Empty description="当前任务类型还没有可展示的趋势数据" />
            )}
          </Card>

          <Card
            title="多任务对比"
            extra={
              <Space wrap>
                <Select
                  mode="multiple"
                  allowClear
                  style={{ minWidth: 360 }}
                  maxTagCount={3}
                  placeholder="选择 2 到 5 个任务进行横向对比"
                  value={compareSelection}
                  options={compareOptions}
                  onChange={value => setCompareSelection(value.slice(0, 5))}
                />
                <Button
                  type="primary"
                  icon={<RadarChartOutlined />}
                  onClick={handleCompare}
                  loading={compareLoading}
                  disabled={compareSelection.length < 2}
                >
                  开始对比
                </Button>
              </Space>
            }
          >
            {compareData.length ? (
              <Space direction="vertical" size={16} style={{ width: '100%' }}>
                {compareChartOption ? (
                  <Card size="small" title="对比雷达图">
                    <AnalyticsChart option={compareChartOption} height={360} />
                  </Card>
                ) : null}
                <Table
                  rowKey="missionCode"
                  columns={compareColumns}
                  dataSource={compareData}
                  pagination={false}
                  scroll={{ x: 1080 }}
                  size="middle"
                />
              </Space>
            ) : (
              <Empty description="选择多条任务后即可查看时长、速度、电量、告警和成功率对比" />
            )}
          </Card>

          <Card title="分析明细表">
            <Table
              rowKey="id"
              columns={detailColumns}
              dataSource={data}
              loading={loading}
              scroll={{ x: 'max-content' }}
              locale={{ emptyText: '暂无数据' }}
            />
          </Card>

          <Card
            title="单任务复盘"
            extra={
              <Space wrap>
                <Select
                  allowClear
                  showSearch
                  style={{ minWidth: 320 }}
                  placeholder="选择一个任务查看时序趋势与复盘"
                  value={selectedReplayMission}
                  options={replayOptions}
                  onChange={value => {
                    setSelectedReplayMission(value);
                    setReplayMetrics([]);
                  }}
                />
                <Select
                  mode="multiple"
                  allowClear
                  maxTagCount={3}
                  style={{ minWidth: 320 }}
                  placeholder="选择要回放的指标曲线"
                  value={replayMetrics}
                  options={(timeseriesData?.metricOptions || replayData?.metricOptions || []).map(item => ({
                    label: `${item.displayName}${item.unit ? ` (${item.unit})` : ''}`,
                    value: item.metricCode
                  }))}
                  onChange={value => setReplayMetrics(value)}
                />
                <Button
                  icon={<DownloadOutlined />}
                  disabled={!replayData}
                  onClick={exportReplayCsv}
                >
                  导出任务 CSV
                </Button>
                <Button
                  icon={<DownloadOutlined />}
                  disabled={!replayData}
                  onClick={exportReplayExcel}
                >
                  导出任务 Excel
                </Button>
                <Button
                  icon={<ReloadOutlined />}
                  loading={replayLoading}
                  disabled={!selectedReplayMission}
                  onClick={() => {
                    if (selectedReplayMission) {
                      loadReplay(selectedReplayMission, replayMetrics).catch(() => undefined);
                    }
                  }}
                >
                  刷新复盘
                </Button>
              </Space>
            }
          >
            {replayData ? (
              <Space direction="vertical" size={16} style={{ width: '100%' }}>
                <Alert
                  type="info"
                  showIcon
                  message={`${replayData.missionName} · ${replayData.missionCode}`}
                  description={`任务类型 ${replayData.missionType}，状态 ${formatMissionStatus(replayData.status)}，无人机 ${replayData.uavCode || '--'}，复盘窗口 ${replayData.startTime ? dayjs(replayData.startTime).format('MM-DD HH:mm:ss') : '--'} ~ ${replayData.endTime ? dayjs(replayData.endTime).format('MM-DD HH:mm:ss') : '--'}`}
                />
                <Row gutter={[16, 16]}>
                  {replaySummaryCards.map(item => (
                    <Col xs={24} sm={12} xl={6} key={item.title}>
                      <Card
                        size="small"
                        hoverable={Boolean(item.clickable)}
                        onClick={item.clickable ? scrollToReplayAlerts : undefined}
                        style={item.clickable ? { cursor: 'pointer' } : undefined}
                      >
                        <Statistic
                          title={item.title}
                          value={item.value == null ? undefined : Number(item.value.toFixed(item.precision ?? 2))}
                          suffix={item.suffix}
                          precision={item.precision ?? 2}
                        />
                      </Card>
                    </Col>
                  ))}
                </Row>
                <Row gutter={[16, 16]}>
                  <Col xs={24}>
                    <Card size="small" title="时序趋势回放">
                      {timeseriesChartOption ? (
                        <AnalyticsChart option={timeseriesChartOption} height={340} />
                      ) : (
                        <Empty description="当前任务暂无可绘制的指标曲线" />
                      )}
                    </Card>
                  </Col>
                </Row>
                <Row gutter={[16, 16]}>
                  <Col xs={24} xl={12}>
                    <Card size="small" title="原始采样曲线">
                      {rawSampleChartOption ? (
                        <AnalyticsChart option={rawSampleChartOption} height={300} />
                      ) : (
                        <Empty description="当前任务暂无原始采样数据" />
                      )}
                    </Card>
                  </Col>
                  <Col xs={24} xl={12}>
                    <Card size="small" title="任务时间轴">
                      <Table
                        rowKey={record => `${record.category}-${record.eventType}-${record.occurredAt}`}
                        columns={replayTimelineColumns}
                        dataSource={replayData.timeline}
                        pagination={false}
                        size="small"
                        scroll={{ y: 300 }}
                        locale={{ emptyText: '暂无时间轴事件' }}
                      />
                    </Card>
                  </Col>
                </Row>
                <div ref={replayAlertsRef}>
                  <Card
                    size="small"
                    title="任务报警记录"
                    extra={<Tag color="red">共 {replayAlertRecords.length} 条</Tag>}
                  >
                    <Table
                      rowKey="id"
                      columns={replayAlertColumns}
                      dataSource={replayAlertRecords}
                      pagination={{ pageSize: 6, showSizeChanger: false }}
                      size="small"
                      scroll={{ x: 1080 }}
                      locale={{ emptyText: '当前任务未触发报警记录' }}
                    />
                  </Card>
                </div>
                <Card size="small" title="原始采样明细">
                  <Table
                    rowKey={record => record.reportedAt}
                    columns={replaySampleColumns}
                    dataSource={replayData.samples}
                    pagination={{ pageSize: 8, showSizeChanger: false }}
                    size="small"
                    scroll={{ x: 1080 }}
                    locale={{ emptyText: '暂无原始采样数据' }}
                  />
                </Card>
              </Space>
            ) : (
              <Empty description="选择一个任务后即可查看时序趋势、原始采样曲线与任务复盘" />
            )}
          </Card>
        </>
      ) : (
        <Empty description="请先选择任务类型，再生成分析结果" />
      )}
    </div>
  );
}

export default DataAnalytics;
