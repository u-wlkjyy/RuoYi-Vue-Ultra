<template>
  <div class="app-container home">
    <!-- 欢迎栏 -->
    <div class="welcome-bar">
      <div class="welcome-left">
        <h2>{{ title }}</h2>
        <p>{{ currentTime }}</p>
      </div>
      <div class="welcome-right">
        <div class="user-info-item">
          <span class="label">当前用户：</span>
          <span class="value">{{ userName }}</span>
        </div>
        <div class="user-info-item">
          <span class="label">所属部门：</span>
          <span class="value">{{ userDept }}</span>
        </div>
      </div>
    </div>


    <!-- 快捷入口 -->
    <div class="section-container">
      <div class="section-header">
        <h3>快捷入口</h3>
      </div>
      <div class="quick-links">
        <div class="quick-link-item" @click="goTo('/system/config')">
          <el-icon :size="24"><Edit /></el-icon>
          <span>参数设置</span>
        </div>
        <div class="quick-link-item" @click="goTo('/system/notice')">
          <el-icon :size="24"><Document /></el-icon>
          <span>通知公告</span>
        </div>
        <div class="quick-link-item" @click="goTo('/system/user')">
          <el-icon :size="24"><User /></el-icon>
          <span>用户管理</span>
        </div>
      </div>
    </div>

    <!-- 通知公告 -->
    <div class="section-container">
      <div class="section-header">
        <h3>通知公告</h3>
        <el-button size="small" type="text" @click="goTo('/system/notice')">
          查看更多 <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>
      <div class="notice-list" v-loading="noticeLoading">
        <div
            v-for="notice in noticeList"
            :key="notice.noticeId"
            class="notice-item"
            @click="handleNoticeClick(notice)"
        >
          <div class="notice-left">
            <el-tag :type="notice.noticeType === '1' ? 'warning' : 'primary'" size="small">
              {{ notice.noticeType === '1' ? '通知' : '公告' }}
            </el-tag>
            <span class="notice-title">{{ notice.noticeTitle }}</span>
          </div>
          <div class="notice-right">
            <span class="notice-time">{{ formatNoticeTime(notice.createTime) }}</span>
          </div>
        </div>
        <el-empty v-if="!noticeLoading && noticeList.length === 0" description="暂无通知公告" :image-size="80" />
      </div>
    </div>

  </div>
</template>

<script setup name="Index">
import { ref, onMounted, onBeforeUnmount, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import useUserStore from '@/store/modules/user'
import { getUserProfile } from "@/api/system/user"
import { listNotice, getNotice } from "@/api/system/notice"
import {
  Document,
  OfficeBuilding,
  Bell,
  Check,
  Edit,
  QuestionFilled,
  Reading,
  ArrowRight, User
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const { proxy } = getCurrentInstance()

// 当前时间
const currentTime = ref('')
let timeInterval = null


// 用户信息
const userName = ref('管理员')
const userDept = ref('暂无')
const loginTime = ref('')

const title = import.meta.env.VITE_APP_TITLE

// 通知公告
const noticeList = ref([])
const noticeLoading = ref(false)

// 更新当前时间
function updateTime() {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  const hour = String(now.getHours()).padStart(2, '0')
  const minute = String(now.getMinutes()).padStart(2, '0')
  const second = String(now.getSeconds()).padStart(2, '0')
  const weekDays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  const weekDay = weekDays[now.getDay()]

  currentTime.value = `${year}年${month}月${day}日 ${hour}:${minute}:${second} ${weekDay}`
}

// 跳转页面
function goTo(path) {
  router.push(path)
}


// 获取用户部门信息
function getUserInfo() {
  // 从 userStore 获取用户名
  if (userStore.name) {
    userName.value = userStore.name
  }

  // 获取用户详细信息（包括部门）
  getUserProfile().then(response => {
    if (response.data && response.data.dept) {
      userDept.value = response.data.dept.deptName || '暂无'
    }
  }).catch(() => {
    // 如果获取失败，保持默认值
  })

  // 获取登录时间（从本地存储或store中获取）
  const now = new Date()
  const hour = String(now.getHours()).padStart(2, '0')
  const minute = String(now.getMinutes()).padStart(2, '0')
  loginTime.value = `今天 ${hour}:${minute}`
}

// 获取通知公告列表
function getNoticeList() {
  noticeLoading.value = true
  listNotice({
    pageNum: 1,
    pageSize: 5
  }).then(response => {
    noticeList.value = response.rows || []
  }).catch(() => {
    noticeList.value = []
  }).finally(() => {
    noticeLoading.value = false
  })
}

// 点击通知公告
function handleNoticeClick(notice) {
  // 打开通知公告详情对话框
  getNotice(notice.noticeId).then(response => {
    const noticeData = response.data
    proxy.$alert(noticeData.noticeContent, noticeData.noticeTitle, {
      dangerouslyUseHTMLString: true,
      confirmButtonText: '关闭',
      customClass: 'notice-dialog'
    })
  })
}

// 格式化通知时间
function formatNoticeTime(timeStr) {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now - date
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))

  if (days === 0) {
    const hours = Math.floor(diff / (1000 * 60 * 60))
    if (hours === 0) {
      const minutes = Math.floor(diff / (1000 * 60))
      return minutes <= 0 ? '刚刚' : `${minutes}分钟前`
    }
    return `${hours}小时前`
  } else if (days === 1) {
    return '昨天'
  } else if (days < 7) {
    return `${days}天前`
  } else {
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${date.getFullYear()}-${month}-${day}`
  }
}

onMounted(() => {
  updateTime()
  timeInterval = setInterval(updateTime, 1000)
  getUserInfo()
  getNoticeList()
})

onBeforeUnmount(() => {
  if (timeInterval) {
    clearInterval(timeInterval)
  }
})
</script>

<style scoped lang="scss">
.home {
  margin: -20px;
  padding: 0;
  background-color: #f5f5f5;
  min-height: calc(100vh - 84px);

  // 欢迎栏
  .welcome-bar {
    background: #ffffff;
    padding: 24px 32px;
    margin-top : 30px;
    margin-bottom: 16px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1px solid #e8e8e8;

    .welcome-left {
      h2 {
        margin: 0 0 8px 0;
        font-size: 20px;
        font-weight: 500;
        color: #262626;
      }

      p {
        margin: 0;
        font-size: 14px;
        color: #8c8c8c;
      }
    }

    .welcome-right {
      display: flex;
      gap: 32px;

      .user-info-item {
        font-size: 14px;
        color: #595959;

        .label {
          color: #8c8c8c;
        }

        .value {
          font-weight: 500;
          color: #262626;
        }
      }
    }
  }

  // 统计卡片行
  .stat-row {
    padding: 24px 32px 0;
  }

  .stat-card {
    background: #ffffff;
    border: 1px solid #e8e8e8;
    border-radius: 2px;
    padding: 24px;
    display: flex;
    align-items: center;
    margin-bottom: 16px;
    transition: border-color 0.3s;

    &:hover {
      border-color: #1890ff;
    }

    .stat-icon {
      width: 56px;
      height: 56px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 2px;
      margin-right: 16px;

      &.stat-blue {
        background: #e6f7ff;
        color: #1890ff;
      }

      &.stat-green {
        background: #f6ffed;
        color: #52c41a;
      }

      &.stat-orange {
        background: #fff7e6;
        color: #fa8c16;
      }

      &.stat-red {
        background: #fff1f0;
        color: #f5222d;
      }
    }

    .stat-content {
      flex: 1;

      .stat-number {
        font-size: 28px;
        font-weight: 500;
        color: #262626;
        line-height: 1;
        margin-bottom: 8px;
      }

      .stat-label {
        font-size: 14px;
        color: #8c8c8c;
      }
    }
  }

  // 区块容器
  .section-container {
    background: #ffffff;
    border: 1px solid #e8e8e8;
    border-radius: 2px;
    margin: 0 32px 24px;
    overflow: hidden;

    .section-header {
      padding: 16px 24px;
      border-bottom: 1px solid #e8e8e8;
      background: #fafafa;
      display: flex;
      justify-content: space-between;
      align-items: center;

      h3 {
        margin: 0;
        font-size: 16px;
        font-weight: 500;
        color: #262626;
      }
    }
  }

  // 快捷入口
  .quick-links {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 1px;
    background: #e8e8e8;
    border-top: 1px solid #e8e8e8;

    .quick-link-item {
      background: #ffffff;
      padding: 24px;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 12px;
      cursor: pointer;
      transition: all 0.3s;

      .el-icon {
        color: #1890ff;
      }

      span {
        font-size: 14px;
        color: #595959;
      }

      &:hover {
        background: #fafafa;

        .el-icon {
          color: #40a9ff;
        }

        span {
          color: #262626;
        }
      }
    }
  }

  // 通知公告列表
  .notice-list {
    padding: 0;
    min-height: 200px;

    .notice-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px 24px;
      border-bottom: 1px solid #e8e8e8;
      cursor: pointer;
      transition: background 0.3s;

      &:last-child {
        border-bottom: none;
      }

      &:hover {
        background: #fafafa;
      }

      .notice-left {
        flex: 1;
        display: flex;
        align-items: center;
        gap: 12px;
        overflow: hidden;

        .notice-title {
          font-size: 14px;
          color: #262626;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }

      .notice-right {
        flex-shrink: 0;
        margin-left: 16px;

        .notice-time {
          font-size: 13px;
          color: #8c8c8c;
        }
      }
    }
  }

}

@media (max-width: 768px) {
  .home {
    .welcome-bar {
      flex-direction: column;
      align-items: flex-start;
      padding: 16px;
      gap: 12px;

      .welcome-right {
        flex-direction: column;
        gap: 8px;
      }
    }

    .stat-row {
      padding: 16px;
    }

    .stat-card {
      padding: 16px;

      .stat-icon {
        width: 48px;
        height: 48px;
      }

      .stat-content {
        .stat-number {
          font-size: 24px;
        }
      }
    }

    .section-container {
      margin: 0 16px 16px;
    }

    .quick-links {
      grid-template-columns: repeat(2, 1fr);
    }
  }
}
</style>
