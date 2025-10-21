<template>
  <div class="component-upload-image">
    <el-upload
        multiple
        :disabled="disabled"
        :action="uploadImgUrl"
        list-type="picture-card"
        :on-success="handleUploadSuccess"
        :before-upload="handleBeforeUpload"
        :data="data"
        :limit="limit"
        :on-error="handleUploadError"
        :on-exceed="handleExceed"
        ref="imageUpload"
        :before-remove="handleDelete"
        :show-file-list="true"
        :headers="headers"
        :file-list="fileList"
        :on-preview="handlePictureCardPreview"
        :class="{ hide: fileList.length >= limit }"
    >
      <el-icon class="avatar-uploader-icon"><plus /></el-icon>
    </el-upload>
    <!-- 上传提示 -->
    <div class="el-upload__tip" v-if="showTip && !disabled">
      请上传
      <template v-if="fileSize">
        大小不超过 <b style="color: #f56c6c">{{ fileSize }}MB</b>
      </template>
      <template v-if="fileType">
        格式为 <b style="color: #f56c6c">{{ fileType.join("/") }}</b>
      </template>
      的文件
    </div>

    <!-- 从图库选择按钮 -->
    <el-button
        v-if="!disabled && fileList.length < limit"
        type="primary"
        plain
        size="small"
        style="margin-top: 10px;"
        @click="openGallery"
    >
      <el-icon><Picture /></el-icon> 从图库选择
    </el-button>

    <el-dialog
        v-model="dialogVisible"
        title="预览"
        width="800px"
        append-to-body
    >
      <img
          :src="dialogImageUrl"
          style="display: block; max-width: 100%; margin: 0 auto"
      />
    </el-dialog>

    <!-- 图库选择对话框 -->
    <el-dialog
        v-model="galleryVisible"
        title="选择图片"
        width="80%"
        append-to-body
        destroy-on-close
    >
      <div class="gallery-container">
        <!-- 搜索栏 -->
        <el-input
            v-model="searchKeyword"
            placeholder="搜索图片名称"
            clearable
            style="margin-bottom: 20px;"
            @keyup.enter="searchGallery"
        >
          <template #append>
            <el-button :icon="Search" @click="searchGallery" />
          </template>
        </el-input>

        <!-- 图片网格 -->
        <div v-loading="galleryLoading" class="image-grid">
          <div
              v-for="image in galleryImages"
              :key="image.filePath"
              class="image-item"
              :class="{ selected: selectedImages.includes(image.filePath) }"
              @click="toggleImageSelection(image)"
          >
            <img :src="image.url" :alt="image.fileName" />
            <div class="image-info">
              <div class="image-name" :title="image.fileName">{{ image.fileName }}</div>
              <div class="image-size">{{ image.sizeFormatted }}</div>
            </div>
            <div v-if="selectedImages.includes(image.filePath)" class="selected-mark">
              <el-icon><Select /></el-icon>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <el-empty v-if="!galleryLoading && galleryImages.length === 0" description="暂无图片" />

        <!-- 分页 -->
        <el-pagination
            v-if="galleryTotal > 0"
            v-model:current-page="galleryPageNum"
            v-model:page-size="galleryPageSize"
            :total="galleryTotal"
            :page-sizes="[20, 40, 60, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @current-change="loadGalleryImages"
            @size-change="loadGalleryImages"
            style="margin-top: 20px; justify-content: center;"
        />
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="galleryVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmSelection">
            确定选择 ({{ selectedImages.length }})
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { getToken } from "@/utils/auth"
import { isExternal } from "@/utils/validate"
import Sortable from 'sortablejs'
import { getImageList } from "@/api/filesystem"
import { Search, Select, Picture } from '@element-plus/icons-vue'

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
  // 图片数量限制
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
    default: () => ["png", "jpg", "jpeg"]
  },
  // 是否显示提示
  isShowTip: {
    type: Boolean,
    default: true
  },
  // 禁用组件（仅查看图片）
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
const dialogImageUrl = ref("")
const dialogVisible = ref(false)
const baseUrl = import.meta.env.VITE_APP_BASE_API
const uploadImgUrl = ref(import.meta.env.VITE_APP_BASE_API + props.action) // 上传的图片服务器地址
const headers = ref({ Authorization: "Bearer " + getToken() })
const fileList = ref([])
const showTip = computed(
    () => props.isShowTip && (props.fileType || props.fileSize)
)

// 图库相关状态
const galleryVisible = ref(false)
const galleryLoading = ref(false)
const galleryImages = ref([])
const selectedImages = ref([])
const searchKeyword = ref('')
const galleryPageNum = ref(1)
const galleryPageSize = ref(20)
const galleryTotal = ref(0)

watch(() => props.modelValue, val => {
  if (val) {
    // 首先将值转为数组
    const list = Array.isArray(val) ? val : props.modelValue.split(",")
    // 然后将数组转为对象数组
    fileList.value = list.map(item => {
      if (typeof item === "string") {
        if (item.indexOf(baseUrl) === -1 && !isExternal(item)) {
          item = { name: baseUrl + item, url: baseUrl + item }
        } else {
          item = { name: item, url: item }
        }
      }
      return item
    })
  } else {
    fileList.value = []
    return []
  }
},{ deep: true, immediate: true })

// 上传前loading加载
function handleBeforeUpload(file) {
  let isImg = false
  if (props.fileType.length) {
    let fileExtension = ""
    if (file.name.lastIndexOf(".") > -1) {
      fileExtension = file.name.slice(file.name.lastIndexOf(".") + 1)
    }
    isImg = props.fileType.some(type => {
      if (file.type.indexOf(type) > -1) return true
      if (fileExtension && fileExtension.indexOf(type) > -1) return true
      return false
    })
  } else {
    isImg = file.type.indexOf("image") > -1
  }
  if (!isImg) {
    proxy.$modal.msgError(`文件格式不正确，请上传${props.fileType.join("/")}图片格式文件!`)
    return false
  }
  if (file.name.includes(',')) {
    proxy.$modal.msgError('文件名不正确，不能包含英文逗号!')
    return false
  }
  if (props.fileSize) {
    const isLt = file.size / 1024 / 1024 < props.fileSize
    if (!isLt) {
      proxy.$modal.msgError(`上传头像图片大小不能超过 ${props.fileSize} MB!`)
      return false
    }
  }
  proxy.$modal.loading("正在上传图片，请稍候...")
  number.value++
}

// 文件个数超出
function handleExceed() {
  proxy.$modal.msgError(`上传文件数量不能超过 ${props.limit} 个!`)
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
    proxy.$refs.imageUpload.handleRemove(file)
    uploadedSuccessfully()
  }
}

// 删除图片
function handleDelete(file) {
  const findex = fileList.value.map(f => f.name).indexOf(file.name)
  if (findex > -1 && uploadList.value.length === number.value) {
    fileList.value.splice(findex, 1)
    emit("update:modelValue", listToString(fileList.value))
    return false
  }
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

// 上传失败
function handleUploadError() {
  proxy.$modal.msgError("上传图片失败")
  proxy.$modal.closeLoading()
}

// 预览
function handlePictureCardPreview(file) {
  dialogImageUrl.value = file.url
  dialogVisible.value = true
}

// 对象转成指定字符串分隔
function listToString(list, separator) {
  let strs = ""
  separator = separator || ","
  for (let i in list) {
    if (undefined !== list[i].url && list[i].url.indexOf("blob:") !== 0) {
      strs += list[i].url.replace(baseUrl, "") + separator
    }
  }
  return strs != "" ? strs.substr(0, strs.length - 1) : ""
}

// 打开图库
function openGallery() {
  galleryVisible.value = true
  selectedImages.value = []
  galleryPageNum.value = 1
  searchKeyword.value = ''
  loadGalleryImages()
}

// 加载图库图片
function loadGalleryImages() {
  galleryLoading.value = true
  getImageList({
    keyword: searchKeyword.value,
    pageNum: galleryPageNum.value,
    pageSize: galleryPageSize.value
  }).then(response => {
    galleryImages.value = response.data.rows || []
    galleryTotal.value = response.data.total || 0
  }).catch(() => {
    proxy.$modal.msgError('加载图库失败')
  }).finally(() => {
    galleryLoading.value = false
  })
}

// 搜索图库
function searchGallery() {
  galleryPageNum.value = 1
  loadGalleryImages()
}

// 切换图片选择
function toggleImageSelection(image) {
  const index = selectedImages.value.indexOf(image.filePath)

  // 检查是否超出限制
  if (index === -1) {
    // 计算当前已选择数量 + 已上传数量
    const currentTotal = fileList.value.length + selectedImages.value.length
    if (currentTotal >= props.limit) {
      proxy.$modal.msgWarning(`最多只能选择 ${props.limit} 张图片`)
      return
    }
    selectedImages.value.push(image.filePath)
  } else {
    selectedImages.value.splice(index, 1)
  }
}

// 确认选择
function confirmSelection() {
  if (selectedImages.value.length === 0) {
    proxy.$modal.msgWarning('请选择图片')
    return
  }

  // 将选中的图片添加到fileList
  const newImages = galleryImages.value
      .filter(img => selectedImages.value.includes(img.filePath))
      .map(img => ({
        name: img.url,
        url: img.url
      }))

  fileList.value = fileList.value.concat(newImages)
  emit("update:modelValue", listToString(fileList.value))

  galleryVisible.value = false
  proxy.$modal.msgSuccess(`成功添加 ${newImages.length} 张图片`)
}

// 初始化拖拽排序
onMounted(() => {
  if (props.drag && !props.disabled) {
    nextTick(() => {
      const element = proxy.$refs.imageUpload?.$el?.querySelector('.el-upload-list')
      Sortable.create(element, {
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
// .el-upload--picture-card 控制加号部分
:deep(.hide .el-upload--picture-card) {
  display: none;
}

:deep(.el-upload.el-upload--picture-card.is-disabled) {
  display: none !important;
}

// 图库样式
.gallery-container {
  max-height: 70vh;
  overflow-y: auto;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 15px;
  min-height: 200px;
}

.image-item {
  position: relative;
  border: 2px solid #dcdfe6;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  background: #fff;

  &:hover {
    border-color: #409eff;
    box-shadow: 0 2px 12px 0 rgba(64, 158, 255, 0.3);
    transform: translateY(-2px);
  }

  &.selected {
    border-color: #409eff;
    box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
  }

  img {
    width: 100%;
    height: 150px;
    object-fit: cover;
    display: block;
  }

  .image-info {
    padding: 8px;
    background: #f5f7fa;
  }

  .image-name {
    font-size: 12px;
    color: #606266;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    margin-bottom: 4px;
  }

  .image-size {
    font-size: 11px;
    color: #909399;
  }

  .selected-mark {
    position: absolute;
    top: 5px;
    right: 5px;
    width: 24px;
    height: 24px;
    background: #409eff;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    font-size: 14px;
  }
}

:deep(.el-pagination) {
  display: flex;
  justify-content: center;
}
</style>