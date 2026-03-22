import { DeleteOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import {
  Button,
  Card,
  Col,
  Form,
  Input,
  Modal,
  Row,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useMemo, useState } from 'react';
import {
  departmentApi,
  userApi,
  type DepartmentRow,
  type UserRow,
} from '../services/api';
import { useAuth } from '../context/AuthContext';
import { canManagePersonnel, isDeptLead, isSuperAdmin, roleLabel } from '../utils/roles';

type DepartmentForm = {
  deptCode: string;
  deptName: string;
  description?: string;
};

type UserForm = {
  username: string;
  password: string;
  name?: string;
  role: string;
  departmentId?: number;
};

function PersonnelManagement() {
  const { currentUser } = useAuth();
  const [departments, setDepartments] = useState<DepartmentRow[]>([]);
  const [users, setUsers] = useState<UserRow[]>([]);
  const [selectedDepartmentId, setSelectedDepartmentId] = useState<number | undefined>(currentUser?.departmentId);
  const [departmentModalOpen, setDepartmentModalOpen] = useState(false);
  const [userModalOpen, setUserModalOpen] = useState(false);
  const [departmentForm] = Form.useForm<DepartmentForm>();
  const [userForm] = Form.useForm<UserForm>();

  const showDepartmentManager = isSuperAdmin(currentUser?.role);
  const showUserManager = canManagePersonnel(currentUser?.role);

  const loadDepartments = () => {
    departmentApi
      .list()
      .then(items => {
        setDepartments(items);
        if (!isSuperAdmin(currentUser?.role) && currentUser?.departmentId) {
          setSelectedDepartmentId(currentUser.departmentId);
        } else if (!selectedDepartmentId && items.length) {
          setSelectedDepartmentId(items[0].id);
        }
      })
      .catch(err => {
        message.error(err.message || '获取部门失败');
        setDepartments([]);
      });
  };

  const loadUsers = (departmentId?: number) => {
    userApi
      .list(departmentId)
      .then(setUsers)
      .catch(err => {
        message.error(err.message || '获取用户失败');
        setUsers([]);
      });
  };

  useEffect(() => {
    loadDepartments();
  }, []);

  useEffect(() => {
    if (!showUserManager) {
      return;
    }
    if (isSuperAdmin(currentUser?.role)) {
      loadUsers(selectedDepartmentId);
      return;
    }
    loadUsers(currentUser?.departmentId);
  }, [currentUser?.departmentId, currentUser?.role, selectedDepartmentId, showUserManager]);

  const selectedDepartment = useMemo(
    () => departments.find(item => item.id === selectedDepartmentId),
    [departments, selectedDepartmentId]
  );

  const departmentColumns: ColumnsType<DepartmentRow> = [
    { title: '部门编码', dataIndex: 'deptCode', key: 'deptCode' },
    { title: '部门名称', dataIndex: 'deptName', key: 'deptName' },
    { title: '负责人', key: 'leadCount', render: (_, record) => `${record.leadCount} 人` },
    { title: '执行者', key: 'executorCount', render: (_, record) => `${record.executorCount} 人` },
    { title: '成员总数', key: 'memberCount', render: (_, record) => `${record.memberCount} 人` },
  ];

  const userColumns: ColumnsType<UserRow> = [
    { title: '用户名', dataIndex: 'username', key: 'username' },
    { title: '姓名', dataIndex: 'name', key: 'name' },
    {
      title: '角色',
      dataIndex: 'role',
      key: 'role',
      render: value => (
        <Tag color={value === 'superadmin' ? 'red' : value === 'dept_lead' ? 'blue' : 'default'}>
          {roleLabel(value)}
        </Tag>
      ),
    },
    { title: '所属部门', dataIndex: 'departmentName', key: 'departmentName', render: value => value || '--' },
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
                title: '重置密码为 123456？',
                onOk: () =>
                  userApi
                    .resetPassword(record.id)
                    .then(() => message.success('已重置为 123456'))
                    .catch(err => message.error(err.message || '重置失败')),
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
                title: `确认删除用户 ${record.username}？`,
                onOk: () =>
                  userApi
                    .delete(record.id)
                    .then(() => {
                      message.success('用户已删除');
                      loadUsers(isSuperAdmin(currentUser?.role) ? selectedDepartmentId : currentUser?.departmentId);
                      loadDepartments();
                    })
                    .catch(err => message.error(err.message || '删除失败')),
              })
            }
          >
            删除
          </Button>
        </Space>
      ),
    },
  ];

  const handleCreateDepartment = () => {
    departmentForm
      .validateFields()
      .then(values =>
        departmentApi.create({
          deptCode: values.deptCode,
          deptName: values.deptName,
          description: values.description,
          status: 'ACTIVE',
        })
      )
      .then(() => {
        message.success('部门已创建');
        setDepartmentModalOpen(false);
        departmentForm.resetFields();
        loadDepartments();
      })
      .catch(() => undefined);
  };

  const handleCreateUser = () => {
    userForm
      .validateFields()
      .then(values =>
        userApi.create({
          username: values.username,
          password: values.password,
          name: values.name,
          role: values.role,
          departmentId: isSuperAdmin(currentUser?.role)
            ? values.role === 'superadmin'
              ? undefined
              : values.departmentId
            : currentUser?.departmentId,
        })
      )
      .then(() => {
        message.success('用户已创建');
        setUserModalOpen(false);
        userForm.resetFields();
        loadUsers(isSuperAdmin(currentUser?.role) ? selectedDepartmentId : currentUser?.departmentId);
        loadDepartments();
      })
      .catch(() => undefined);
  };

  const roleOptions = useMemo(() => {
    if (isSuperAdmin(currentUser?.role)) {
      return [
        { value: 'superadmin', label: '超级管理员' },
        { value: 'dept_lead', label: '部门负责人' },
        { value: 'executor', label: '执行者' },
      ];
    }
    if (isDeptLead(currentUser?.role)) {
      return [{ value: 'executor', label: '执行者' }];
    }
    return [];
  }, [currentUser?.role]);

  const departmentOptions = departments.map(item => ({
    label: `${item.deptName} (${item.deptCode})`,
    value: item.id,
  }));

  if (!showUserManager) {
    return <Card title="部门与成员管理">当前角色无成员管理权限。</Card>;
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <Row gutter={[16, 16]}>
        {showDepartmentManager ? (
          <Col xs={24} xl={10}>
            <Card
              title="部门管理"
              extra={
                <Button type="primary" icon={<PlusOutlined />} onClick={() => setDepartmentModalOpen(true)}>
                  新增部门
                </Button>
              }
            >
              <Table<DepartmentRow>
                rowKey="id"
                dataSource={departments}
                columns={departmentColumns}
                pagination={false}
                rowSelection={{
                  type: 'radio',
                  selectedRowKeys: selectedDepartmentId ? [selectedDepartmentId] : [],
                  onChange: keys => setSelectedDepartmentId(keys[0] as number | undefined),
                }}
              />
            </Card>
          </Col>
        ) : null}
        <Col xs={24} xl={showDepartmentManager ? 14 : 24}>
          <Card
            title={showDepartmentManager ? '成员管理' : `${currentUser?.departmentName || '当前部门'}成员管理`}
            extra={
              <Button type="primary" icon={<PlusOutlined />} onClick={() => setUserModalOpen(true)}>
                新增成员
              </Button>
            }
          >
            <Typography.Paragraph type="secondary" style={{ marginBottom: 12 }}>
              {showDepartmentManager
                ? `当前选中部门：${selectedDepartment?.deptName || '全部'}。超级管理员可创建部门负责人和执行者。`
                : '部门负责人只能管理本部门执行者，成员创建后默认初始密码为 123456。'}
            </Typography.Paragraph>
            <Table<UserRow> rowKey="id" dataSource={users} columns={userColumns} pagination={false} />
          </Card>
        </Col>
      </Row>

      <Modal
        title="新增部门"
        open={departmentModalOpen}
        onCancel={() => setDepartmentModalOpen(false)}
        onOk={handleCreateDepartment}
        okText="创建部门"
      >
        <Form form={departmentForm} layout="vertical">
          <Form.Item name="deptCode" label="部门编码" rules={[{ required: true, message: '请输入部门编码' }]}>
            <Input placeholder="如 FOREST" />
          </Form.Item>
          <Form.Item name="deptName" label="部门名称" rules={[{ required: true, message: '请输入部门名称' }]}>
            <Input placeholder="如 森林巡查部" />
          </Form.Item>
          <Form.Item name="description" label="部门说明">
            <Input.TextArea rows={3} placeholder="可选" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="新增成员"
        open={userModalOpen}
        onCancel={() => setUserModalOpen(false)}
        onOk={handleCreateUser}
        okText="创建成员"
      >
        <Form
          form={userForm}
          layout="vertical"
          initialValues={{
            role: isDeptLead(currentUser?.role) ? 'executor' : 'executor',
            departmentId: isSuperAdmin(currentUser?.role) ? selectedDepartmentId : currentUser?.departmentId,
          }}
        >
          {isSuperAdmin(currentUser?.role) ? (
            <Form.Item name="departmentId" label="所属部门">
              <Select allowClear options={departmentOptions} placeholder="超级管理员可不选部门直接创建超管" />
            </Form.Item>
          ) : null}
          <Form.Item name="username" label="用户名" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="password" label="密码" rules={[{ required: true, message: '请输入初始密码' }]}>
            <Input.Password />
          </Form.Item>
          <Form.Item name="name" label="姓名">
            <Input placeholder="可选，默认与用户名相同" />
          </Form.Item>
          <Form.Item name="role" label="角色" rules={[{ required: true, message: '请选择角色' }]}>
            <Select options={roleOptions} disabled={isDeptLead(currentUser?.role)} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

export default PersonnelManagement;
