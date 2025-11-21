import { Card, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { userAccounts, type UserAccount } from '../data/mock';

const columns: ColumnsType<UserAccount> = [
  { title: '姓名', dataIndex: 'name', key: 'name' },
  { title: '账号', dataIndex: 'username', key: 'username' },
  {
    title: '角色',
    dataIndex: 'role',
    key: 'role',
    render: value => (
      <Tag color={value === 'superadmin' ? 'red' : 'blue'}>
        {value === 'superadmin' ? '超级管理员' : '操作员'}
      </Tag>
    )
  },
  {
    title: '说明',
    key: 'desc',
    render: (_, record) =>
      record.role === 'superadmin'
        ? '可查看所有无人机、任务、人员'
        : '仅可查看本人任务与无人机'
  }
];

function PersonnelManagement() {
  return (
    <Card>
      <Typography.Title level={4}>人员管理（模拟数据）</Typography.Title>
      <Typography.Paragraph type="secondary">
        数据来源于 docs/账号密码.md，支持超级管理员查看所有账号。
      </Typography.Paragraph>
      <Table<UserAccount> rowKey="username" columns={columns} dataSource={userAccounts} pagination={false} />
    </Card>
  );
}

export default PersonnelManagement;
