import { LaptopOutlined, RadarChartOutlined, RocketOutlined, TeamOutlined } from '@ant-design/icons';
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
      {
        key: '/dashboard',
        icon: <RadarChartOutlined />,
        label: '运营驾驶舱'
      },
      {
        key: '/fleet',
        icon: <LaptopOutlined />,
        label: '无人机中心'
      },
      {
        key: '/missions',
        icon: <RocketOutlined />,
        label: '任务指挥'
      },
      {
        key: '/monitoring',
        icon: <RadarChartOutlined />,
        label: '实时监测'
      }
    ];
    if (currentUser?.role === 'superadmin') {
      base.push({
        key: '/personnel',
        icon: <TeamOutlined />,
        label: '人员管理'
      });
    }
    return base;
  }, [currentUser?.role]);

  const selectedKeys = useMemo(() => {
    const match = menuItems?.find(item => item && location.pathname.startsWith(item.key as string));
    return match ? [match.key as string] : ['/dashboard'];
  }, [menuItems, location.pathname]);

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider breakpoint="lg" collapsedWidth="0">
        <div
          style={{
            padding: '16px 24px',
            display: 'flex',
            alignItems: 'center',
            gap: 8,
            color: '#fff',
            fontWeight: 600,
            fontSize: 16
          }}
        >
          UAV · Monitor
        </div>
        <Menu
          theme="dark"
          mode="inline"
          items={menuItems}
          selectedKeys={selectedKeys}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>
      <Layout>
        <Header
          style={{
            background: '#fff',
            boxShadow: '0 1px 4px rgba(0, 0, 0, 0.08)',
            zIndex: 9,
            paddingInline: 24,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between'
          }}
        >
          <Typography.Title level={4} style={{ margin: 0 }}>
            无人机环境监测 · 原型
          </Typography.Title>
          {currentUser ? (
            <Space size="middle">
              <Typography.Text>{currentUser.name}</Typography.Text>
              <Button size="small" onClick={logout}>
                退出
              </Button>
            </Space>
          ) : null}
        </Header>
        <Content style={{ margin: '24px', minHeight: 'calc(100vh - 112px)' }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}

export default MainLayout;
