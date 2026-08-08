<template>
  <div class="login-page">
    <img class="login-photo" src="@/assets/meeting-team.jpg" alt="团队会议协作场景" />
    <div class="photo-shade"></div>

    <div class="brand-lockup">
      <span class="brand-icon"><el-icon><FullScreen /></el-icon></span>
      <span class="brand-name">会议签到中心</span>
    </div>

    <section class="login-intro">
      <span class="intro-label">QR Attendance Console</span>
      <h1>每一次到场<br />都清晰可见</h1>
      <p>统一管理会议、成员与签到数据，让会务执行更准确、更从容。</p>
      <div class="intro-meta">
        <span><i class="meta-dot success"></i>服务在线</span>
        <span><i class="meta-dot"></i>数据实时同步</span>
      </div>
    </section>

    <main class="login-panel">
      <div class="panel-inner">
        <div class="panel-heading">
          <span class="panel-kicker">账号登录</span>
          <h2>欢迎回来</h2>
          <p>使用系统账号进入管理工作台</p>
        </div>

        <el-form ref="loginFormRef" :model="loginForm" :rules="rules" class="login-form" label-position="top">
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名"
              size="large"
              :prefix-icon="User"
              autocomplete="username"
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              :prefix-icon="Lock"
              autocomplete="current-password"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-button type="primary" size="large" :loading="loading" class="login-btn" @click="handleLogin">
            进入工作台
            <el-icon class="login-arrow"><ArrowRight /></el-icon>
          </el-button>
        </el-form>

        <div class="panel-footer">
          <el-icon><Lock /></el-icon>
          <span>登录信息将被加密传输</span>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loginFormRef = ref(null)
const loading = ref(false)
const loginForm = reactive({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (loading.value) return

  try {
    await loginFormRef.value.validate()
    loading.value = true
    userStore.logout()
    await userStore.login(loginForm)
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/dashboard')
  } catch (error) {
    if (error !== false) {
      console.error('登录失败:', error)
      ElMessage.error(error?.message || '登录失败，请检查用户名和密码')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background: #111820;
}

.login-photo,
.photo-shade {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.login-photo {
  object-fit: cover;
  object-position: center;
}

.photo-shade {
  background: rgba(10, 17, 27, 0.5);
}

.brand-lockup {
  position: absolute;
  z-index: 2;
  top: 32px;
  left: 40px;
  display: flex;
  align-items: center;
  gap: 11px;
  color: #fff;
}

.brand-icon {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  font-size: 20px;
  background: $color-primary;
  border-radius: $radius-base;
}

.brand-name {
  font-size: 15px;
  font-weight: 650;
}

.login-intro {
  position: absolute;
  z-index: 2;
  left: clamp(40px, 7vw, 112px);
  bottom: clamp(60px, 12vh, 128px);
  width: min(520px, 42vw);
  color: #fff;
}

.intro-label {
  display: inline-block;
  margin-bottom: 16px;
  color: #afc8ff;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
}

.login-intro h1 {
  margin: 0 0 18px;
  font-size: 46px;
  font-weight: 650;
  line-height: 1.2;
}

.login-intro p {
  max-width: 480px;
  margin: 0;
  color: rgba(255, 255, 255, 0.76);
  font-size: 15px;
  line-height: 1.8;
}

.intro-meta {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-top: 30px;
  color: rgba(255, 255, 255, 0.68);
  font-size: 12px;

  span {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.meta-dot {
  width: 7px;
  height: 7px;
  background: #7aa2f7;
  border-radius: 50%;

  &.success {
    background: #4bd3a4;
  }
}

.login-panel {
  position: relative;
  z-index: 3;
  width: min(480px, 42vw);
  min-height: 100vh;
  margin-left: auto;
  display: flex;
  align-items: center;
  background: #fff;
  box-shadow: -20px 0 60px rgba(0, 0, 0, 0.16);
}

.panel-inner {
  width: 100%;
  padding: 48px 56px;
}

.panel-heading {
  margin-bottom: 32px;

  .panel-kicker {
    display: block;
    margin-bottom: 10px;
    color: $color-primary;
    font-size: 12px;
    font-weight: 650;
  }

  h2 {
    margin: 0 0 8px;
    color: $text-primary;
    font-size: 28px;
    font-weight: 650;
  }

  p {
    margin: 0;
    color: $text-secondary;
    font-size: 13px;
  }
}

.login-form {
  :deep(.el-form-item) {
    margin-bottom: 22px;
  }

  :deep(.el-form-item__label) {
    margin-bottom: 7px;
    color: $text-regular;
    font-size: 13px;
    font-weight: 550;
  }

  :deep(.el-input__wrapper) {
    min-height: 46px;
    padding: 0 14px;
    box-shadow: 0 0 0 1px $border-base inset;

    &.is-focus {
      box-shadow: 0 0 0 1px $color-primary inset;
    }
  }
}

.login-btn {
  width: 100%;
  height: 46px;
  margin-top: 4px;
  font-weight: 600;
}

.login-arrow {
  margin-left: 8px;
}

.panel-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 26px;
  color: $text-placeholder;
  font-size: 11px;
}

@media (max-width: 960px) {
  .login-intro {
    display: none;
  }

  .photo-shade {
    background: rgba(10, 17, 27, 0.5);
  }

  .login-panel {
    width: min(460px, calc(100% - 48px));
    min-height: auto;
    margin: 96px auto 32px;
    border-radius: $radius-base;
  }

  .brand-lockup {
    top: 24px;
    left: 24px;
  }
}

@media (max-width: 560px) {
  .login-page {
    display: flex;
    align-items: flex-end;
  }

  .login-photo {
    height: 36%;
    object-position: 60% center;
  }

  .photo-shade {
    height: 36%;
    background: rgba(10, 17, 27, 0.42);
  }

  .brand-lockup {
    top: 22px;
    left: 20px;
  }

  .login-panel {
    width: 100%;
    min-height: 70vh;
    margin: 30vh 0 0;
    align-items: flex-start;
    border-radius: $radius-base $radius-base 0 0;
  }

  .panel-inner {
    padding: 36px 24px 28px;
  }

  .panel-heading {
    margin-bottom: 26px;

    h2 {
      font-size: 24px;
    }
  }
}
</style>
