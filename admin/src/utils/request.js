import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers['Authorization'] = `Bearer ${userStore.token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      console.error('[Response Error]', res.code, res.message)
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  error => {
    console.error('[HTTP Error]', error.response?.status, error.message)
    
    if (error.response) {
      const status = error.response.status
      const message = error.response.data?.message || '请求失败'
      
      if (status === 401 || status === 403) {
        const userStore = useUserStore()
        // 清除缓存
        userStore.token = ''
        userStore.userId = ''
        userStore.userInfo = {}
        userStore.roleCode = ''
        localStorage.removeItem('token')
        localStorage.removeItem('userId')
        localStorage.removeItem('userInfo')
        localStorage.removeItem('roleCode')
        
        ElMessage.error('登录状态已失效，请重新登录')
        router.push('/login')
      } else {
        ElMessage.error(message)
      }
    } else {
      ElMessage.error('网络异常，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

export default request
