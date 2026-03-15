import {
  BarChartOutlined,
  LaptopOutlined,
  RadarChartOutlined,
  RocketOutlined,
  TeamOutlined,
  ProfileOutlined,
  AlertOutlined
} from '@ant-design/icons';
import { Button, Layout, Menu, Space, Typography } from 'antd';
import type { MenuProps } from 'antd';
import { useMemo } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const { Header, Sider, Content } = Layout;

function MainLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const { currentUser, logout } = useAuth();

  const menuItems: MenuProps['items'] = useMemo(() => {
    const base: MenuProps['items'] = [
      { key: '/dashboard', icon: <RadarChartOutlined />, label: '运行总览' },
      { key: '/fleet', icon: <LaptopOutlined />, label: '无人机管理' },
      { key: '/missions', icon: <RocketOutlined />, label: '任务指挥' },
      { key: '/monitoring', icon: <RadarChartOutlined />, label: '实时监控' },
      { key: '/analytics', icon: <BarChartOutlined />, label: '数据分析' },
      { key: '/alerts', icon: <AlertOutlined />, label: currentUser?.role === 'superadmin' ? '报警规则管理' : '报警记录' }
    ];
    if (currentUser?.role === 'superadmin') {
      base.push({ key: '/personnel', icon: <TeamOutlined />, label: '人员管理' });
      base.push({ key: '/config-center', icon: <ProfileOutlined />, label: '任务与指标配置' });
    }
    return base;
  }, [currentUser?.role]);

  const selectedKeys = useMemo(() => {
    const match = menuItems?.find(item => item && location.pathname.startsWith(item.key as string));
    return match ? [match.key as string] : ['/dashboard'];
  }, [menuItems, location.pathname]);

  return (
    <Layout className="app-shell">
      <Sider breakpoint="lg" collapsedWidth="0" width={256} className="app-sidebar">
        <div className="app-brand">
          <Space direction="vertical" size={10} style={{ width: '100%' }}>
            <Space size={12} align="center">
              <div className="app-brand-mark">U</div>
              <Space direction="vertical" size={0}>
                <Typography.Text style={{ color: '#f4f8ff', fontSize: 18, fontWeight: 800 }}>
                  UAV Monitor
                </Typography.Text>
                <Typography.Text style={{ color: 'rgba(221, 233, 255, 0.74)', fontSize: 12 }}>
                  智能调度与环境监测平台
                </Typography.Text>
              </Space>
            </Space>
          </Space>
        </div>
        <Menu
          theme="dark"
          mode="inline"
          items={menuItems}
          selectedKeys={selectedKeys}
          onClick={({ key }) => navigate(key)}
          style={{ fontSize: 16 }}
        />
      </Sider>
      <Layout style={{ height: '100vh', background: 'transparent' }}>
        <Header
          className="app-header"
          style={{
            zIndex: 9,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            minHeight: 86,
            height: 'auto',
            gap: 16,
            flexWrap: 'wrap',
            paddingBlock: 14
          }}
        >
          <Space direction="vertical" size={0} style={{ flex: 1, minWidth: 260 }}>
            <Typography.Title level={4} className="page-title">
              无人机环境监测控制台
            </Typography.Title>
            <Typography.Text className="page-subtitle">
              调度、监控、分析与告警联动统一工作台
            </Typography.Text>
          </Space>
          {currentUser ? (
            <div className="app-userbar">
              <div className="app-usermeta">
                <Typography.Text className="app-username">{currentUser.name}</Typography.Text>
                <Typography.Text className="app-userrole">
                  {currentUser.role === 'superadmin' ? '系统管理员' : '业务操作员'}
                </Typography.Text>
              </div>
              <Button size="small" onClick={logout}>
                退出
              </Button>
            </div>
          ) : null}
        </Header>
        <Content className="page-surface">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}

export default MainLayout;
