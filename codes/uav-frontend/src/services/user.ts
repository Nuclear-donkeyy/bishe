import { http, ensureSuccess } from './http';

export interface UserRow {
  id: number;
  username: string;
  name: string;
  role: string;
  status?: string;
}

export const userApi = {
  list: () => http.get<UserRow[]>('/users').then(r => ensureSuccess(r.data, '获取用户失败')),
  create: (payload: { username: string; password: string; name?: string; role: string }) =>
    http.post<UserRow>('/users', payload).then(r => ensureSuccess(r.data, '新增用户失败')),
  delete: (id: number) =>
    http.delete(`/users/${id}`).then(r => {
      if (r.data && (r.data as any).success === false) {
        throw new Error((r.data as any).message || '删除用户失败');
      }
      return;
    }),
  resetPassword: (id: number) => http.post(`/users/${id}/reset-password`).then(() => void 0)
};
