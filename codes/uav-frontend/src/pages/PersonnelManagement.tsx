import { DeleteOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { Button, Card, Form, Input, Modal, Space, Table, Tag, Select, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useState } from 'react';
import { userApi, type UserRow } from '../services/user';

interface NewUserForm {
  username: string;
  password: string;
  name?: string;
  role: string;
}

function PersonnelManagement() {
  const [data, setData] = useState<UserRow[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [form] = Form.useForm<NewUserForm>();

  const load = () => {
    (userApi.list() as Promise<UserRow[] | undefined>)
      .then(users => setData(users ?? []))
      .catch(err => {
        message.error(err.message || '获取用户失败');
        setData([]);
      });
  };

  useEffect(() => {
    load();
  }, []);

  const columns: ColumnsType<UserRow> = [
    { title: '用户名', dataIndex: 'username', key: 'username' },
    { title: '姓名', dataIndex: 'name', key: 'name' },
    {
      title: '角色',
      dataIndex: 'role',
      key: 'role',
      render: value => <Tag color={value === 'superadmin' ? 'red' : 'blue'}>{value === 'superadmin' ? '超级管理员' : '操作员'}</Tag>
    },
    {
      title: '操作',
      key: 'actions',
      render: (_, record) => (
        <Space>
          <Button
            size="small"
            icon={<ReloadOutlined />}
            onClick={() =>
              Modal.confirm({
                title: `重置密码为 123456？`,
                onOk: () =>
                  userApi
                    .resetPassword(record.id)
                    .then(() => message.success('已重置为 123456'))
                    .catch(err => message.error(err.message || '重置失败'))
              })
            }
          >
            重置密码
          </Button>
          <Button
            danger
            size="small"
            icon={<DeleteOutlined />}
            onClick={() =>
              Modal.confirm({
                title: record.role === 'superadmin' ? '注销当前超级管理员？' : `确认删除操作员 ${record.username}？`,
                onOk: () =>
                  userApi
                    .delete(record.id)
                    .then(load)
                    .catch(err => message.error(err.message || '删除失败'))
              })
            }
          >
            {record.role === 'superadmin' ? '注销' : '删除'}
          </Button>
        </Space>
      )
    }
  ];

  const handleSubmit = () => {
    form
      .validateFields()
      .then(values => {
        userApi
          .create(values)
          .then(() => {
            message.success('已新增用户');
            load();
          })
          .catch(err => message.error(err.message || '新增失败'));
        setModalOpen(false);
        form.resetFields();
      })
      .catch(() => undefined);
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <Card
        title="人员管理"
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>
            新增用户
          </Button>
        }
      >
        <Table<UserRow> rowKey="id" dataSource={data} columns={columns} pagination={false} />
      </Card>

      <Modal
        title="新增用户"
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        okText="保存"
      >
        <Form form={form} layout="vertical" initialValues={{ role: 'operator' }}>
          <Form.Item name="username" label="用户名" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="password" label="密码" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password />
          </Form.Item>
          <Form.Item name="name" label="姓名">
            <Input placeholder="可选，默认与用户名相同" />
          </Form.Item>
          <Form.Item name="role" label="角色" rules={[{ required: true }]}>
            <Select
              options={[
                { value: 'operator', label: '操作员' },
                { value: 'superadmin', label: '超级管理员' }
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>

      <Typography.Paragraph type="secondary" style={{ margin: 0 }}>
        说明：超级管理员删除时必须是当前登录账号且需保留至少一名超级管理员；后台会返回明确的失败原因。
      </Typography.Paragraph>
    </div>
  );
}

export default PersonnelManagement;
