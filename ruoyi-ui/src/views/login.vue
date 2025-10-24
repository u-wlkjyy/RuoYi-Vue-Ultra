<template>
  <div class="login-container">
    <!-- 左侧品牌展示区 -->
    <div class="login-brand">
      <div class="brand-content">
        <div class="brand-logo">
          <img src="../assets/logo/logo.png" alt="Logo" class="logo-img">
        </div>
        <h1 class="brand-title">
          {{title}}
        </h1>
        <p class="brand-subtitle">{{ title }} 让软件开发更简单</p>

      </div>
    </div>

    <!-- 右侧登录表单区 -->
    <div class="login-form-container">
      <div class="login-form-wrapper">
        <div class="form-header">
          <h2 class="form-title">欢迎登录</h2>
          <p class="form-subtitle">请输入您的账号信息</p>
        </div>

        <el-form ref="loginRef" :model="loginForm" :rules="loginRules" class="login-form">
          <el-form-item prop="username">
            <el-input
                v-model="loginForm.username"
                type="text"
                size="large"
                auto-complete="off"
                placeholder="请输入账号"
                class="form-input"
            >
              <template #prefix>
                <svg-icon icon-class="user" class="input-icon" />
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="password">
            <el-input
                v-model="loginForm.password"
                type="password"
                size="large"
                auto-complete="off"
                placeholder="请输入密码"
                class="form-input"
                @keyup.enter="handleLogin"
            >
              <template #prefix>
                <svg-icon icon-class="password" class="input-icon" />
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="code" v-if="captchaEnabled" class="captcha-item">
            <el-input
                v-model="loginForm.code"
                size="large"
                auto-complete="off"
                placeholder="验证码"
                class="captcha-input"
                @keyup.enter="handleLogin"
            >
              <template #prefix>
                <svg-icon icon-class="validCode" class="input-icon" />
              </template>
            </el-input>
            <div class="captcha-image">
              <img :src="codeUrl" @click="getCode" class="captcha-img" alt="验证码"/>
            </div>
          </el-form-item>

          <el-form-item prop="totpCode">
            <el-input
                v-model="loginForm.totpCode"
                size="large"
                auto-complete="off"
                placeholder="2FA验证码（如已启用）"
                maxlength="6"
                class="form-input"
                @keyup.enter="handleLogin"
            >
              <template #prefix>
                <svg-icon icon-class="lock" class="input-icon" />
              </template>
            </el-input>
          </el-form-item>

          <div class="form-options">
            <el-checkbox v-model="loginForm.rememberMe" class="remember-me">
              记住密码
            </el-checkbox>
            <router-link v-if="register" class="forgot-password" :to="'/register'">
              忘记密码？
            </router-link>
          </div>

          <el-form-item class="login-button-item">
            <el-button
                :loading="loading"
                size="large"
                type="primary"
                class="login-button"
                @click.prevent="handleLogin"
            >
              <span v-if="!loading">登 录</span>
              <span v-else>登 录 中...</span>
            </el-button>
          </el-form-item>

          <div class="register-link" v-if="register">
            <span>还没有账号？</span>
            <router-link class="link-text" :to="'/register'">立即注册</router-link>
          </div>
        </el-form>
      </div>
    </div>

    <!-- 底部版权信息 -->
    <div class="login-footer">
      <span>Copyright © 2025-{{ currentYear }} {{title}}版权所有 All Rights Reserved.</span>
    </div>
  </div>
</template>

<script setup>
import { getCodeImg } from "@/api/login"
import Cookies from "js-cookie"
import { encrypt, decrypt } from "@/utils/jsencrypt"
import useUserStore from '@/store/modules/user'

const title = import.meta.env.VITE_APP_TITLE
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()

const loginForm = ref({
  username: "admin",
  password: "admin123",
  rememberMe: false,
  code: "",
  uuid: "",
  totpCode: ""
})

const currentYear = new Date().getFullYear()

const loginRules = {
  username: [{ required: true, trigger: "blur", message: "请输入您的账号" }],
  password: [{ required: true, trigger: "blur", message: "请输入您的密码" }],
  code: [{ required: true, trigger: "change", message: "请输入验证码" }]
}

const codeUrl = ref("")
const loading = ref(false)
// 验证码开关
const captchaEnabled = ref(true)
// 注册开关
const register = ref(false)
const redirect = ref(undefined)

watch(route, (newRoute) => {
  redirect.value = newRoute.query && newRoute.query.redirect
}, { immediate: true })

function handleLogin() {
  proxy.$refs.loginRef.validate(valid => {
    if (valid) {
      loading.value = true
      // 勾选了需要记住密码设置在 cookie 中设置记住用户名和密码
      if (loginForm.value.rememberMe) {
        Cookies.set("username", loginForm.value.username, { expires: 30 })
        Cookies.set("password", encrypt(loginForm.value.password), { expires: 30 })
        Cookies.set("rememberMe", loginForm.value.rememberMe, { expires: 30 })
      } else {
        // 否则移除
        Cookies.remove("username")
        Cookies.remove("password")
        Cookies.remove("rememberMe")
      }
      // 调用action的登录方法
      userStore.login(loginForm.value).then(() => {
        const query = route.query
        const otherQueryParams = Object.keys(query).reduce((acc, cur) => {
          if (cur !== "redirect") {
            acc[cur] = query[cur]
          }
          return acc
        }, {})
        router.push({ path: redirect.value || "/", query: otherQueryParams })
      }).catch(() => {
        loading.value = false
        // 重新获取验证码
        if (captchaEnabled.value) {
          getCode()
        }
      })
    }
  })
}

function getCode() {
  getCodeImg().then(res => {
    captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled
    if (captchaEnabled.value) {
      codeUrl.value = "data:image/gif;base64," + res.img
      loginForm.value.uuid = res.uuid
    }
  })
}

function getCookie() {
  const username = Cookies.get("username")
  const password = Cookies.get("password")
  const rememberMe = Cookies.get("rememberMe")
  loginForm.value = {
    username: username === undefined ? loginForm.value.username : username,
    password: password === undefined ? loginForm.value.password : decrypt(password),
    rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
  }
}

getCode()
getCookie()
</script>

<style lang='scss' scoped>
.login-container {
  display: flex;
  min-height: 100vh;
  background: linear-gradient(135deg, #2c5282 0%, #1a365d 100%);
  position: relative;
  overflow: hidden;
}

/* 左侧品牌展示区 */
.login-brand {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px;
  position: relative;
  z-index: 1;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-size: cover;
    background: url("../assets/images/login-background.jpg");
    background-repeat: no-repeat;
    background-size: 100% 100%;
    opacity: 0.5;
    z-index: -1;
  }

  .brand-content {
    text-align: center;
    color: white;
    max-width: 500px;
    margin-top: 60px;

    .brand-logo {
      margin-bottom: 30px;

      .logo-img {
        width: 60px;
        height: 60px;
      }
    }

    .brand-title {
      font-size: 2.5rem;
      font-weight: 700;
      margin-bottom: 15px;
      text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
      background: linear-gradient(45deg, #fff, #e0e7ff);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
      position: relative;
      height: 40px;


    }

    .brand-subtitle {
      font-size: 1.2rem;
      margin-bottom: 40px;
      opacity: 0.9;
      font-weight: 700;
      letter-spacing: 2px;
    }

    .brand-features {
      display: flex;
      justify-content: space-around;
      margin-top: 50px;

      .feature-item {
        text-align: center;

        .feature-icon {
          font-size: 2rem;
          margin-bottom: 10px;
          background: rgba(255, 255, 255, 0.1);
          width: 60px;
          height: 60px;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          margin: 0 auto 10px;
          backdrop-filter: blur(10px);
          border: 1px solid rgba(255, 255, 255, 0.2);
        }

        .feature-text {
          font-size: 0.9rem;
          opacity: 0.8;
        }
      }
    }
  }
}

/* 右侧登录表单区 */
.login-form-container {
  flex: 0 0 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  position: relative;
  z-index: 1;

  .login-form-wrapper {
    width: 100%;
    max-width: 400px;
    padding: 50px 40px;

    .form-header {
      text-align: center;
      margin-bottom: 40px;

      .form-title {
        font-size: 2rem;
        font-weight: 600;
        color: #1f2937;
        margin-bottom: 8px;
      }

      .form-subtitle {
        color: #6b7280;
        font-size: 0.95rem;
        margin: 0;
      }
    }

    .login-form {
      .el-form-item {
        margin-bottom: 24px;

        &.captcha-item {
          display: flex;
          gap: 12px;
          align-items: flex-start;

          .captcha-input {
            flex: 1;
          }

          .captcha-image {
            flex: 0 0 120px;
            height: 48px;

            .captcha-img {
              width: 100%;
              height: 100%;
              border-radius: 6px;
              cursor: pointer;
              border: 1px solid #d1d5db;
              transition: all 0.3s ease;

              &:hover {
                border-color: #3b82f6;
                transform: scale(1.02);
              }
            }
          }
        }

        &.login-button-item {
          margin-bottom: 0;
        }
      }

      .form-input {
        .el-input__wrapper {
          height: 48px;
          border-radius: 8px;
          border: 1px solid #e5e7eb;
          transition: all 0.3s ease;
          box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);

          &:hover {
            border-color: #3b82f6;
          }

          &.is-focus {
            border-color: #3b82f6;
            box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
          }

          .el-input__inner {
            height: 46px;
            font-size: 15px;
            color: #374151;

            &::placeholder {
              color: #9ca3af;
            }
          }
        }

        .input-icon {
          color: #6b7280;
          font-size: 16px;
        }
      }

      .form-options {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 30px;

        .remember-me {
          color: #6b7280;
          font-size: 14px;

          :deep(.el-checkbox__label) {
            color: #6b7280;
          }
        }

        .forgot-password {
          color: #3b82f6;
          text-decoration: none;
          font-size: 14px;
          transition: color 0.3s ease;

          &:hover {
            color: #2563eb;
          }
        }
      }

      .login-button {
        width: 100%;
        height: 48px;
        border-radius: 8px;
        font-size: 16px;
        font-weight: 500;
        background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
        border: none;
        transition: all 0.3s ease;
        box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);

        &:hover {
          transform: translateY(-2px);
          box-shadow: 0 6px 20px rgba(59, 130, 246, 0.4);
        }

        &:active {
          transform: translateY(0);
        }
      }

      .register-link {
        text-align: center;
        margin-top: 24px;
        color: #6b7280;
        font-size: 14px;

        .link-text {
          color: #3b82f6;
          text-decoration: none;
          font-weight: 500;
          margin-left: 4px;
          transition: color 0.3s ease;

          &:hover {
            color: #2563eb;
          }
        }
      }
    }
  }
}

/* 底部版权信息 */
.login-footer {
  position: absolute;
  bottom: 20px;
  left: calc((100% - 480px) / 2);
  transform: translateX(-50%);
  color: rgba(255, 255, 255, 0.8);
  font-size: 12px;
  text-align: center;
  z-index: 1;
  backdrop-filter: blur(10px);
  background: rgba(0, 0, 0, 0.1);
  padding: 8px 20px;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .login-container {
    flex-direction: column;

    .login-brand {
      flex: 0 0 auto;
      padding: 40px 20px;

      .brand-content {
        .brand-title {
          font-size: 2rem;
        }

        .brand-features {
          margin-top: 30px;

          .feature-item {
            .feature-icon {
              width: 50px;
              height: 50px;
              font-size: 1.5rem;
            }
          }
        }
      }
    }

    .login-form-container {
      flex: 1;
      min-height: auto;

      .login-form-wrapper {
        padding: 30px 20px;
      }
    }
  }
}

@media (max-width: 768px) {
  .login-brand {
    .brand-content {
      .brand-title {
        font-size: 1.8rem;
      }

      .brand-subtitle {
        font-size: 1rem;
      }

      .brand-features {
        flex-direction: column;
        gap: 20px;
      }
    }
  }

  .login-form-container {
    .login-form-wrapper {
      max-width: 100%;
      padding: 20px;

      .form-header {
        .form-title {
          font-size: 1.5rem;
        }
      }
    }
  }
}

@media (max-width: 480px) {
  .login-container {
    .login-form-container {
      .login-form-wrapper {
        .login-form {
          .el-form-item {
            &.captcha-item {
              flex-direction: column;
              gap: 8px;

              .captcha-image {
                flex: none;
                width: 100%;
                height: 40px;
              }
            }
          }
        }
      }
    }
  }
}
</style>
