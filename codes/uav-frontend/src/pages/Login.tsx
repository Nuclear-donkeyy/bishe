import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { Button, Card, Form, Input, Typography, message } from 'antd';
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
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'linear-gradient(120deg, #051840 0%, #0066ff 60%)',
        padding: 16
      }}
    >
      <Card style={{ width: 360 }}>
        <Typography.Title level={4} style={{ textAlign: 'center' }}>
          无人机环境监测平台
        </Typography.Title>
        <Typography.Paragraph style={{ textAlign: 'center', color: '#667085' }}>
          多租户演示 · 请输入 docs/账号密码.md 中的账号
        </Typography.Paragraph>
        <Form<LoginValues> layout="vertical" onFinish={onFinish}>
          <Form.Item
            name="username"
            label="账号"
            rules={[{ required: true, message: '请输入账号' }]}
          >
            <Input prefix={<UserOutlined />} placeholder="如 superadmin / 张三" />
          </Form.Item>
          <Form.Item
            name="password"
            label="密码"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="123456" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={loading}>
            登录
          </Button>
        </Form>
      </Card>
    </div>
  );
}

export default Login;
