import request from '@/utils/request'

// 获取所有用户列表
export const getAllUsers = () => request.get('/user/list')

// 分页查询用户列表
export const getUserList = (params) => request.get('/user/page', { params })

// 获取用户详情
export const getUserDetail = (id) => request.get(`/user/${id}`)

// 创建用户
export const createUser = (data) => request.post('/user', data)

// 更新用户
export const updateUser = (id, data) => request.put(`/user/${id}`, data)

// 删除用户
export const deleteUser = (id) => request.delete(`/user/${id}`)

// 获取部门列表
export const getDeptList = () => request.get('/dept/list')
