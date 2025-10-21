<template>
  <div class="upload-file">
    <div class="upload-buttons" v-if="!disabled">
      <el-upload
          multiple
          :action="uploadFileUrl"
          :before-upload="handleBeforeUpload"
          :file-list="fileList"
          :data="data"
          :limit="limit"
          :on-error="handleUploadError"
          :on-exceed="handleExceed"
          :on-success="handleUploadSuccess"
          :show-file-list="false"
          :headers="headers"
          class="upload-file-uploader"
          ref="fileUpload"
      >
        <!-- 上传按钮 -->
        <el-button type="primary">选取文件</el-button>
      </el-upload>

      <!-- 从资源库选择按钮 -->
      <el-button
          type="success"
          plain
          @click="openLibrary"
      >
        <el-icon><FolderOpened /></el-icon> 从资源库选择
      </el-button>
    </div>

    <!-- 上传提示 -->
    <div class="el-upload__tip" v-if="showTip && !disabled">
      请上传
      <template v-if="fileSize"> 大小不超过 <b style="color: #f56c6c">{{ fileSize }}MB</b> </template>
      <template v-if="fileType"> 格式为 <b style="color: #f56c6c">{{ fileType.join("/") }}</b> </template>
      的文件
    </div>
    <!-- 文件列表 -->
    <transition-group ref="uploadFileList" class="upload-file-list el-upload-list el-upload-list--text" name="el-fade-in-linear" tag="ul">
      <li :key="file.uid" class="el-upload-list__item ele-upload-list__item-content" v-for="(file, index) in fileList">
        <el-link :href="`${baseUrl}${file.url}`" :underline="false" target="_blank">
          <span class="el-icon-document"> {{ getFileName(file.name) }} </span>
        </el-link>
        <div class="ele-upload-list__item-content-action">
          <el-link :underline="false" @click="handleDelete(index)" type="danger" v-if="!disabled">&nbsp;删除</el-link>
        </div>
      </li>
    </transition-group>

    <!-- 资源库选择对话框 -->
    <el-dialog
        v-model="libraryVisible"
        title="选择附件"
        width="80%"
        append-to-body
        destroy-on-close
    >
      <div class="library-container">
        <!-- 搜索栏 -->
        <el-input
            v-model="searchKeyword"
            placeholder="搜索附件名称"
            clearable
            style="margin-bottom: 20px;"
            @keyup.enter="searchLibrary"
        >
          <template #append>
            <el-button :icon="Search" @click="searchLibrary" />
          </template>
        </el-input>

        <!-- 附件列表 -->
        <div v-loading="libraryLoading" class="attachment-list">
          <el-table
              :data="libraryAttachments"
              style="width: 100%"
              @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="55" />
            <el-table-column label="文件名" min-width="200">
              <template #default="scope">
                <el-icon><Document /></el-icon>
                {{ scope.row.fileName }}
              </template>
            </el-table-column>
            <el-table-column label="文件类型" width="100">
              <template #default="scope">
                <el-tag size="small">{{ scope.row.extension }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="文件大小" width="120">
              <template #default="scope">
                {{ scope.row.sizeFormatted }}
              </template>
            </el-table-column>
            <el-table-column label="修改时间" width="180">
              <template #default="scope">
                {{ formatDate(scope.row.modifiedDate) }}
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 空状态 -->
        <el-empty v-if="!libraryLoading && libraryAttachments.length === 0" description="暂无附件" />

        <!-- 分页 -->
        <el-pagination
            v-if="libraryTotal > 0"
            v-model:current-page="libraryPageNum"
            v-model:page-size="libraryPageSize"
            :total="libraryTotal"
            :page-sizes="[20, 40, 60, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @current-change="loadLibraryAttachments"
            @size-change="loadLibraryAttachments"
            style="margin-top: 20px; justify-content: center;"
        />
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="libraryVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmSelection">
            确定选择 ({{ selectedAttachments.length }})
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { getToken } from "@/utils/auth"
import Sortable from 'sortablejs'
import { getAttachmentList } from "@/api/filesystem"
import { Search, FolderOpened, Document } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: [String, Object, Array],
  // 上传接口地址
  action: {
    type: String,
    default: "/common/upload"
  },
  // 上传携带的参数
  data: {
    type: Object
  },
  // 数量限制
  limit: {
    type: Number,
    default: 5
  },
  // 大小限制(MB)
  fileSize: {
    type: Number,
    default: 5
  },
  // 文件类型, 例如['png', 'jpg', 'jpeg']
  fileType: {
    type: Array,
    default: () => ["doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "pdf","zip", "rar", "7z", "tar", "gz", "bz2", "xz"]
  },
  // 是否显示提示
  isShowTip: {
    type: Boolean,
    default: true
  },
  // 禁用组件（仅查看文件）
  disabled: {
    type: Boolean,
    default: false
  },
  // 拖动排序
  drag: {
    type: Boolean,
    default: true
  }
})

const { proxy } = getCurrentInstance()
const emit = defineEmits()
const number = ref(0)
const uploadList = ref([])
const baseUrl = import.meta.env.VITE_APP_BASE_API
const uploadFileUrl = ref(import.meta.env.VITE_APP_BASE_API + props.action) // 上传文件服务器地址
const headers = ref({ Authorization: "Bearer " + getToken() })
const fileList = ref([])
const showTip = computed(
    () => props.isShowTip && (props.fileType || props.fileSize)
)

// 资源库相关状态
const libraryVisible = ref(false)
const libraryLoading = ref(false)
const libraryAttachments = ref([])
const selectedAttachments = ref([])
const searchKeyword = ref('')
const libraryPageNum = ref(1)
const libraryPageSize = ref(20)
const libraryTotal = ref(0)

watch(() => props.modelValue, val => {
  if (val) {
    let temp = 1
    // 首先将值转为数组
    const list = Array.isArray(val) ? val : props.modelValue.split(',')
    // 然后将数组转为对象数组
    fileList.value = list.map(item => {
      if (typeof item === "string") {
        item = { name: item, url: item }
      }
      item.uid = item.uid || new Date().getTime() + temp++
      return item
    })
  } else {
    fileList.value = []
    return []
  }
},{ deep: true, immediate: true })

// 上传前校检格式和大小
function handleBeforeUpload(file) {
  // 校检文件类型
  if (props.fileType.length) {
    const fileName = file.name.split('.')
    const fileExt = fileName[fileName.length - 1]
    const isTypeOk = props.fileType.indexOf(fileExt) >= 0
    if (!isTypeOk) {
      proxy.$modal.msgError(`文件格式不正确，请上传${props.fileType.join("/")}格式文件!`)
      return false
    }
  }
  // 校检文件名是否包含特殊字符
  if (file.name.includes(',')) {
    proxy.$modal.msgError('文件名不正确，不能包含英文逗号!')
    return false
  }
  // 校检文件大小
  if (props.fileSize) {
    const isLt = file.size / 1024 / 1024 < props.fileSize
    if (!isLt) {
      proxy.$modal.msgError(`上传文件大小不能超过 ${props.fileSize} MB!`)
      return false
    }
  }
  proxy.$modal.loading("正在上传文件，请稍候...")
  number.value++
  return true
}

// 文件个数超出
function handleExceed() {
  proxy.$modal.msgError(`上传文件数量不能超过 ${props.limit} 个!`)
}

// 上传失败
function handleUploadError(err) {
  proxy.$modal.msgError("上传文件失败")
  proxy.$modal.closeLoading()
}

// 上传成功回调
function handleUploadSuccess(res, file) {
  if (res.code === 200) {
    uploadList.value.push({ name: res.fileName, url: res.fileName })
    uploadedSuccessfully()
  } else {
    number.value--
    proxy.$modal.closeLoading()
    proxy.$modal.msgError(res.msg)
    proxy.$refs.fileUpload.handleRemove(file)
    uploadedSuccessfully()
  }
}

// 删除文件
function handleDelete(index) {
  fileList.value.splice(index, 1)
  emit("update:modelValue", listToString(fileList.value))
}

// 上传结束处理
function uploadedSuccessfully() {
  if (number.value > 0 && uploadList.value.length === number.value) {
    fileList.value = fileList.value.filter(f => f.url !== undefined).concat(uploadList.value)
    uploadList.value = []
    number.value = 0
    emit("update:modelValue", listToString(fileList.value))
    proxy.$modal.closeLoading()
  }
}

// 获取文件名称
function getFileName(name) {
  // 如果是url那么取最后的名字 如果不是直接返回
  if (name.lastIndexOf("/") > -1) {
    return name.slice(name.lastIndexOf("/") + 1)
  } else {
    return name
  }
}

// 对象转成指定字符串分隔
function listToString(list, separator) {
  let strs = ""
  separator = separator || ","
  for (let i in list) {
    if (list[i].url) {
      strs += list[i].url + separator
    }
  }
  return strs != '' ? strs.substr(0, strs.length - 1) : ''
}

// 格式化日期时间
function formatDate(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

// 打开资源库
function openLibrary() {
  libraryVisible.value = true
  selectedAttachments.value = []
  libraryPageNum.value = 1
  searchKeyword.value = ''
  loadLibraryAttachments()
}

// 加载资源库附件
function loadLibraryAttachments() {
  libraryLoading.value = true
  getAttachmentList({
    keyword: searchKeyword.value,
    pageNum: libraryPageNum.value,
    pageSize: libraryPageSize.value
  }).then(response => {
    libraryAttachments.value = response.data.rows || []
    libraryTotal.value = response.data.total || 0
  }).catch(() => {
    proxy.$modal.msgError('加载资源库失败')
  }).finally(() => {
    libraryLoading.value = false
  })
}

// 搜索资源库
function searchLibrary() {
  libraryPageNum.value = 1
  loadLibraryAttachments()
}

// 处理表格选择变化
function handleSelectionChange(selection) {
  selectedAttachments.value = selection
}

// 确认选择
function confirmSelection() {
  if (selectedAttachments.value.length === 0) {
    proxy.$modal.msgWarning('请选择附件')
    return
  }

  // 检查数量限制
  const currentTotal = fileList.value.length + selectedAttachments.value.length
  if (currentTotal > props.limit) {
    proxy.$modal.msgWarning(`最多只能选择 ${props.limit} 个文件`)
    return
  }

  // 将选中的附件添加到fileList
  let temp = new Date().getTime()
  const newFiles = selectedAttachments.value.map(attachment => ({
    name: attachment.filePath,
    url: attachment.filePath,
    uid: temp++
  }))

  fileList.value = fileList.value.concat(newFiles)
  emit("update:modelValue", listToString(fileList.value))

  libraryVisible.value = false
  proxy.$modal.msgSuccess(`成功添加 ${newFiles.length} 个文件`)
}

// 初始化拖拽排序
onMounted(() => {
  if (props.drag && !props.disabled) {
    nextTick(() => {
      const element = proxy.$refs.uploadFileList?.$el || proxy.$refs.uploadFileList
      Sortable.create(element, {
        ghostClass: 'file-upload-darg',
        onEnd: (evt) => {
          const movedItem = fileList.value.splice(evt.oldIndex, 1)[0]
          fileList.value.splice(evt.newIndex, 0, movedItem)
          emit('update:modelValue', listToString(fileList.value))
        }
      })
    })
  }
})
</script>
<style scoped lang="scss">
.file-upload-darg {
  opacity: 0.5;
  background: #c8ebfb;
}

.upload-buttons {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 5px;
}

.upload-file-uploader {
  display: inline-block;
  :deep(.el-upload) {
    display: inline-block;
  }
}
.upload-file-list .el-upload-list__item {
  border: 1px solid #e4e7ed;
  line-height: 2;
  margin-bottom: 10px;
  position: relative;
  transition: none !important;
}
.upload-file-list .ele-upload-list__item-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: inherit;
}
.ele-upload-list__item-content-action .el-link {
  margin-right: 10px;
}

// 资源库样式
.library-container {
  max-height: 70vh;
  overflow-y: auto;
}

.attachment-list {
  min-height: 300px;

  :deep(.el-table) {
    .el-icon {
      margin-right: 5px;
      color: #909399;
    }
  }
}

:deep(.el-pagination) {
  display: flex;
  justify-content: center;
}
</style>
