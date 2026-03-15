import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { Button, Card, Form, Input, Space, Typography, message } from 'antd';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

interface LoginValues {
  username: string;
  password: string;
}

function Login() {
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const onFinish = async (values: LoginValues) => {
    setLoading(true);
    try {
      await login(values.username.trim(), values.password.trim());
      message.success('登录成功');
      navigate('/dashboard', { replace: true });
    } catch (error) {
      message.error((error as Error).message || '登录失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-shell">
      <div className="login-panel">
        <div className="login-hero">
          <div className="login-badge">UAV MONITORING PLATFORM</div>
          <Typography.Title
            level={1}
            style={{ color: '#f4f8ff', marginTop: 28, marginBottom: 14, fontSize: 42, lineHeight: 1.08 }}
          >
            面向任务执行与环境感知的
            <br />
            一体化无人机监测平台
          </Typography.Title>
          <Typography.Paragraph
            style={{
              color: 'rgba(236, 243, 255, 0.82)',
              fontSize: 16,
              lineHeight: 1.8,
              maxWidth: 560,
              marginBottom: 28
            }}
          >
            统一接入无人机设备、任务调度、告警联动与数据分析能力，为巡检、监测与应急业务提供更稳定的运行控制界面。
          </Typography.Paragraph>
          <Space direction="vertical" size={10}>
            <Typography.Text style={{ color: '#e9f1ff' }}>实时监控与健康感知</Typography.Text>
            <Typography.Text style={{ color: '#e9f1ff' }}>任务调度、抢占与联动处置</Typography.Text>
            <Typography.Text style={{ color: '#e9f1ff' }}>全流程分析看板与任务复盘</Typography.Text>
          </Space>
        </div>
        <Card className="login-form-card" style={{ width: '100%' }}>
          <Typography.Title level={3} style={{ textAlign: 'center', marginBottom: 8 }}>
            平台登录
          </Typography.Title>
          <Typography.Paragraph style={{ textAlign: 'center', color: '#6a7890', marginBottom: 28 }}>
            请输入账号与密码进入控制台
          </Typography.Paragraph>
          <Form<LoginValues> layout="vertical" onFinish={onFinish}>
            <Form.Item
              name="username"
              label="账号"
              rules={[{ required: true, message: '请输入账号' }]}
            >
              <Input prefix={<UserOutlined />} placeholder="请输入账号" size="large" />
            </Form.Item>
            <Form.Item
              name="password"
              label="密码"
              rules={[{ required: true, message: '请输入密码' }]}
            >
              <Input.Password prefix={<LockOutlined />} placeholder="请输入密码" size="large" />
            </Form.Item>
            <Button type="primary" htmlType="submit" block loading={loading} size="large">
              登录
            </Button>
          </Form>
        </Card>
      </div>
    </div>
  );
}

export default Login;
