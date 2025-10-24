<template>
  <div class="two-factor-container">
    <el-alert
      v-if="!is2faEnabled"
      title="提示"
      type="info"
      :closable="false"
      style="margin-bottom: 20px"
    >
      <p>启用两步验证可以增强账户安全性，登录时需要输入2FA验证码。</p>
      <p>推荐使用 Google Authenticator、Microsoft Authenticator 等应用生成验证码。</p>
    </el-alert>

    <el-alert
      v-else
      title="两步验证已启用"
      type="success"
      :closable="false"
      style="margin-bottom: 20px"
    >
      您的账户已启用两步验证，登录时需要输入验证码。
    </el-alert>

    <!-- 绑定2FA -->
    <div v-if="!is2faEnabled" class="bind-section">
      <el-button type="primary" @click="handleShowBindDialog">启用两步验证</el-button>
    </div>

    <!-- 解绑2FA -->
    <div v-else class="unbind-section">
      <el-button type="danger" @click="handleShowUnbindDialog">关闭两步验证</el-button>
    </div>

    <!-- 绑定对话框 -->
    <el-dialog
      v-model="bindDialogVisible"
      title="绑定两步验证"
      width="600px"
      :close-on-click-modal="false"
    >
      <div class="bind-dialog-content">
        <el-steps :active="bindStep" finish-status="success" align-center>
          <el-step title="扫描二维码"></el-step>
          <el-step title="验证绑定"></el-step>
        </el-steps>

        <div v-if="bindStep === 1" class="step-content">
          <p style="margin-bottom: 15px; text-align: center;">
            请使用验证器应用扫描下方二维码
          </p>
          <div class="qrcode-container">
            <qrcode-vue 
              v-if="qrCodeUrl" 
              :value="qrCodeUrl" 
              :size="200" 
              level="H"
            />
          </div>
          <div class="secret-key">
            <p>密钥（手动输入）：</p>
            <el-input 
              v-model="secretKey" 
              readonly 
              class="secret-key-input"
            >
              <template #append>
                <el-button @click="copySecretKey">复制</el-button>
              </template>
            </el-input>
          </div>
          <div style="text-align: center; margin-top: 20px;">
            <el-button type="primary" @click="bindStep = 2">下一步</el-button>
            <el-button @click="bindDialogVisible = false">取消</el-button>
          </div>
        </div>

        <div v-if="bindStep === 2" class="step-content">
          <p style="margin-bottom: 15px; text-align: center;">
            请输入验证器应用显示的6位验证码
          </p>
          <el-form ref="bindFormRef" :model="bindForm" :rules="bindRules">
            <el-form-item prop="code">
              <el-input
                v-model="bindForm.code"
                placeholder="请输入6位验证码"
                maxlength="6"
                style="width: 300px; margin: 0 auto; display: block;"
                @keyup.enter="handleBind"
              />
            </el-form-item>
          </el-form>
          <div style="text-align: center; margin-top: 20px;">
            <el-button @click="bindStep = 1">上一步</el-button>
            <el-button type="primary" :loading="bindLoading" @click="handleBind">
              确认绑定
            </el-button>
            <el-button @click="bindDialogVisible = false">取消</el-button>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 解绑对话框 -->
    <el-dialog
      v-model="unbindDialogVisible"
      title="关闭两步验证"
      width="500px"
      :close-on-click-modal="false"
    >
      <div class="unbind-dialog-content">
        <el-alert
          title="警告"
          type="warning"
          :closable="false"
          style="margin-bottom: 20px"
        >
          关闭两步验证会降低账户安全性，请谨慎操作！
        </el-alert>
        
        <el-form ref="unbindFormRef" :model="unbindForm" :rules="unbindRules">
          <el-form-item label="当前密码" prop="password">
            <el-input
              v-model="unbindForm.password"
              type="password"
              placeholder="请输入当前密码"
              show-password
            />
          </el-form-item>
          <el-form-item label="验证码" prop="code">
            <el-input
              v-model="unbindForm.code"
              placeholder="请输入6位验证码"
              maxlength="6"
              @keyup.enter="handleUnbind"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="unbindDialogVisible = false">取 消</el-button>
          <el-button type="danger" :loading="unbindLoading" @click="handleUnbind">
            确认关闭
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import QrcodeVue from 'qrcode.vue'
import { get2faBindInfo, bind2fa, unbind2fa } from '@/api/system/user'

const { proxy } = getCurrentInstance()

const is2faEnabled = ref(false)
const bindDialogVisible = ref(false)
const unbindDialogVisible = ref(false)
const bindStep = ref(1)
const qrCodeUrl = ref('')
const secretKey = ref('')
const bindLoading = ref(false)
const unbindLoading = ref(false)

const bindForm = reactive({
  code: ''
})

const bindRules = {
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { min: 6, max: 6, message: '验证码必须是6位', trigger: 'blur' }
  ]
}

const unbindForm = reactive({
  password: '',
  code: ''
})

const unbindRules = {
  password: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { min: 6, max: 6, message: '验证码必须是6位', trigger: 'blur' }
  ]
}

// 获取2FA状态
function get2faStatus() {
  get2faBindInfo().then(response => {
    const data = response.data || response
    is2faEnabled.value = data.is2faEnabled === 1
  }).catch(() => {
    is2faEnabled.value = false
  })
}

// 显示绑定对话框
function handleShowBindDialog() {
  get2faBindInfo().then(response => {
    const data = response.data || response
    if (data.is2faEnabled === 1) {
      ElMessage.warning('两步验证已启用')
      return
    }
    qrCodeUrl.value = data.qrCodeUrl
    secretKey.value = data.secretKey
    bindStep.value = 1
    bindForm.code = ''
    bindDialogVisible.value = true
  }).catch(() => {
    ElMessage.error('获取2FA信息失败')
  })
}

// 复制密钥
function copySecretKey() {
  navigator.clipboard.writeText(secretKey.value).then(() => {
    ElMessage.success('密钥已复制到剪贴板')
  })
}

// 绑定2FA
function handleBind() {
  proxy.$refs.bindFormRef.validate(valid => {
    if (valid) {
      bindLoading.value = true
      bind2fa({
        secretKey: secretKey.value,
        code: bindForm.code
      }).then(response => {
        ElMessage.success('两步验证绑定成功')
        bindDialogVisible.value = false
        is2faEnabled.value = true
        bindLoading.value = false
      }).catch(() => {
        bindLoading.value = false
      })
    }
  })
}

// 显示解绑对话框
function handleShowUnbindDialog() {
  unbindForm.password = ''
  unbindForm.code = ''
  unbindDialogVisible.value = true
}

// 解绑2FA
function handleUnbind() {
  proxy.$refs.unbindFormRef.validate(valid => {
    if (valid) {
      unbindLoading.value = true
      unbind2fa({
        password: unbindForm.password,
        code: unbindForm.code
      }).then(response => {
        ElMessage.success('两步验证已关闭')
        unbindDialogVisible.value = false
        is2faEnabled.value = false
        unbindLoading.value = false
      }).catch(() => {
        unbindLoading.value = false
      })
    }
  })
}

onMounted(() => {
  get2faStatus()
})
</script>

<style scoped lang="scss">
.two-factor-container {
  padding: 20px;
}

.bind-section,
.unbind-section {
  margin-top: 20px;
}

.bind-dialog-content,
.unbind-dialog-content {
  padding: 20px 0;
}

.step-content {
  margin-top: 30px;
}

.qrcode-container {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
  margin: 20px auto;
  width: fit-content;
}

.secret-key {
  margin-top: 20px;
  text-align: center;

  p {
    margin-bottom: 10px;
    color: #666;
  }

  .secret-key-input {
    width: 400px;
    margin: 0 auto;
  }
}
</style>

