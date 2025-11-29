import { AlertOutlined, ReloadOutlined } from '@ant-design/icons';
import {
  Badge,
  Card,
  Col,
  Empty,
  Row,
  Space,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
  Button
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useMemo, useRef, useState } from 'react';
import { connectTelemetrySocket } from '../services/ws';
import {
  configApi,
  missionApi,
  type MetricItem,
  type MissionDto,
  type MissionTypeItem
} from '../services/api';

type TelemetryByMission = Record<
  string,
  {
    data?: Record<string, any>;
    ts?: number;
  }
>;

function Monitoring() {
  const [missions, setMissions] = useState<MissionDto[]>([]);
  const missionsRef = useRef<MissionDto[]>([]);
  const [activeMissionCode, setActiveMissionCode] = useState<string>('');
  const [missionTypes, setMissionTypes] = useState<MissionTypeItem[]>([]);
  const [metrics, setMetrics] = useState<MetricItem[]>([]);
  const [telemetryMap, setTelemetryMap] = useState<TelemetryByMission>({});
  const wsRef = useRef<{ deactivate: () => void } | null>(null);
  const [loading, setLoading] = useState(false);

  const loadBase = async () => {
    try {
      setLoading(true);
      const [ms, mts, mets] = await Promise.all([
        missionApi.list({ status: ['RUNNING'] }),
        configApi.missionTypes.list(),
        configApi.metrics.list()
      ]);
      setMissions(ms);
      missionsRef.current = ms;
      setMissionTypes((mts as MissionTypeItem[]) || []);
      setMetrics((mets as MetricItem[]) || []);
      if (ms.length && !activeMissionCode) {
        setActiveMissionCode(ms[0].missionCode);
      }
    } catch (e: any) {
      message.error(e?.message || '加载运行中任务失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadBase();
    const client = connectTelemetrySocket({
      onMessage: payload => {
        if (!payload) return;
        const missionId = payload.missionId || payload.missionCode;
        const uavCode = payload.uavCode;
        if (!missionId && !uavCode) return;
        const mission =
          missionsRef.current.find(
            m => m.missionCode === missionId || m.id?.toString() === missionId
          ) ||
          missionsRef.current.find(m => (uavCode ? m.assignedUavs?.includes(uavCode) : false));
        if (!mission) return;
        setTelemetryMap(prev => ({
          ...prev,
          [mission.missionCode]: {
            data: payload.data,
            ts: Date.now()
          }
        }));
      }
    });
    wsRef.current = client;
    return () => client.deactivate();
  }, []);

  const activeMission = useMemo(
    () => missions.find(m => m.missionCode === activeMissionCode),
    [missions, activeMissionCode]
  );

  const metricDefs = useMemo(() => {
    if (!activeMission) return [];
    const type = missionTypes.find(mt => mt.displayName === activeMission.missionType);
    const metricIds = type?.metricIds || [];
    return metrics.filter(m => metricIds.includes(m.id));
  }, [activeMission, missionTypes, metrics]);

  const metricRows: { key: string; label: string; unit?: string; value: string }[] = useMemo(() => {
    if (!activeMission) return [];
    const data = telemetryMap[activeMission.missionCode]?.data || {};
    return metricDefs.map(def => {
      const val = data?.[def.metricCode];
      return {
        key: def.metricCode,
        label: def.name,
        unit: def.unit,
        value: val === undefined || val === null ? '--' : String(val)
      };
    });
  }, [activeMission, metricDefs, telemetryMap]);

  const columns: ColumnsType<(typeof metricRows)[number]> = [
    { title: '指标', dataIndex: 'label', key: 'label' },
    {
      title: '当前值',
      dataIndex: 'value',
      key: 'value',
      render: (value, record) => (
        <Space>
          <span>{value}</span>
          {record.unit ? <span style={{ color: '#999' }}>{record.unit}</span> : null}
        </Space>
      )
    }
  ];

  const runningMissions = useMemo(
    () => missions.filter(m => (m.status || '').toUpperCase() === 'RUNNING'),
    [missions]
  );

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16, minHeight: '100vh' }}>
      <Row gutter={[16, 16]} style={{ flex: 1, minHeight: '80vh', height: '100%' }}>
        <Col xs={24} lg={7} style={{ height: '100%', display: 'flex' }}>
          <Card
            title="执行中任务"
            style={{ width: '100%', height: '100%' }}
          >
            <div style={{ flex: 1, overflowY: 'auto' }}>
              {runningMissions.length ? (
                <Space direction="vertical" style={{ width: '100%' }}>
                  {runningMissions.map(m => {
                    const active = m.missionCode === activeMissionCode;
                    const lastTs = telemetryMap[m.missionCode]?.ts;
                    return (
                      <Card
                        key={m.missionCode}
                        size="small"
                        onClick={() => setActiveMissionCode(m.missionCode)}
                        style={{ cursor: 'pointer', borderColor: active ? '#1677ff' : undefined }}
                      >
                        <Space direction="vertical" size={4} style={{ width: '100%' }}>
                          <Space>
                            <Typography.Text strong>{m.name}</Typography.Text>
                            <Tag color="green">RUNNING</Tag>
                          </Space>
                          <Space size="small">
                            <Tag>{m.missionType}</Tag>
                            <Tag color="blue">优先级 {m.priority}</Tag>
                          </Space>
                          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                            UAV：{m.assignedUavs?.join(', ') || '--'}
                          </Typography.Text>
                          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                            遥测时间：{lastTs ? new Date(lastTs).toLocaleTimeString() : '--'}
                          </Typography.Text>
                        </Space>
                      </Card>
                    );
                  })}
                </Space>
              ) : (
                <Empty description="暂无运行中任务" />
              )}
            </div>
          </Card>
        </Col>

        <Col xs={24} lg={17} style={{ height: '100%', display: 'flex' }}>
          <Card
            title={
              <Space>
                <Typography.Text>实时指标</Typography.Text>
              </Space>
            }
            style={{ height: '100%', width: '100%' }}
          >
            {activeMission ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 12, height: '100%', overflowY: 'auto' }}>
                <Space wrap>
                  <Typography.Text strong>{activeMission.name}</Typography.Text>
                  <Tag>{activeMission.missionType}</Tag>
                  <Badge
                    status="processing"
                    text={`UAV: ${activeMission.assignedUavs?.join(', ') || '--'}`}
                  />
                </Space>
                <Table
                  rowKey="key"
                  dataSource={metricRows}
                  columns={columns}
                  pagination={false}
                  locale={{ emptyText: '未配置指标或暂无遥测' }}
                  size="middle"
                />
              </div>
            ) : (
              <Empty description="请选择运行中的任务" />
            )}
          </Card>
        </Col>
      </Row>
    </div>
  );
}

export default Monitoring;
