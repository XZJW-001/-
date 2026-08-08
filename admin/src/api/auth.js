import request from '@/utils/request'

// 登录
export const login = (data) => request.post('/auth/login', data)

// 登出
export const logout = () => request.post('/auth/logout')

// 获取用户信息
export const getUserInfo = () => request.get('/auth/userInfo')

// 修改密码
export const changePassword = (oldPassword, newPassword) => 
  request.put('/auth/password', null, { params: { oldPassword, newPassword } })
