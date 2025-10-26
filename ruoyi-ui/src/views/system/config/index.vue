<template>
   <div class="app-container">
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
         <el-form-item label="参数名称" prop="configName">
            <el-input
               v-model="queryParams.configName"
               placeholder="请输入参数名称"
               clearable
               style="width: 240px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="参数键名" prop="configKey">
            <el-input
               v-model="queryParams.configKey"
               placeholder="请输入参数键名"
               clearable
               style="width: 240px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="系统内置" prop="configType">
            <el-select v-model="queryParams.configType" placeholder="系统内置" clearable style="width: 240px">
               <el-option
                  v-for="dict in sys_yes_no"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
               />
            </el-select>
         </el-form-item>
         <el-form-item label="创建时间" style="width: 308px;">
            <el-date-picker
               v-model="dateRange"
               value-format="YYYY-MM-DD"
               type="daterange"
               range-separator="-"
               start-placeholder="开始日期"
               end-placeholder="结束日期"
            ></el-date-picker>
         </el-form-item>
         <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
         </el-form-item>
      </el-form>

      <el-row :gutter="10" class="mb8">
         <el-col :span="1.5">
            <el-button
               type="primary"
               plain
               icon="Plus"
               @click="handleAdd"
               v-hasPermi="['system:config:add']"
            >新增</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="success"
               plain
               icon="Edit"
               :disabled="single"
               @click="handleUpdate"
               v-hasPermi="['system:config:edit']"
            >修改</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="danger"
               plain
               icon="Delete"
               :disabled="multiple"
               @click="handleDelete"
               v-hasPermi="['system:config:remove']"
            >删除</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="warning"
               plain
               icon="Download"
               @click="handleExport"
               v-hasPermi="['system:config:export']"
            >导出</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="danger"
               plain
               icon="Refresh"
               @click="handleRefreshCache"
               v-hasPermi="['system:config:remove']"
            >刷新缓存</el-button>
         </el-col>
         <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <el-table v-loading="loading" :data="configList" @selection-change="handleSelectionChange">
         <el-table-column type="selection" width="55" align="center" />
         <el-table-column label="参数主键" align="center" prop="configId" />
         <el-table-column label="参数名称" align="center" prop="configName" :show-overflow-tooltip="true" />
         <el-table-column label="参数键名" align="center" prop="configKey" :show-overflow-tooltip="true" />
         <el-table-column label="参数键值" align="center" prop="configValue" :show-overflow-tooltip="true" />
         <el-table-column label="配置值类型" align="center" prop="configValType" :show-overflow-tooltip="true" />
         <el-table-column label="系统内置" align="center" prop="configType">
            <template #default="scope">
               <dict-tag :options="sys_yes_no" :value="scope.row.configType" />
            </template>
         </el-table-column>
         <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
         <el-table-column label="创建时间" align="center" prop="createTime" width="180">
            <template #default="scope">
               <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
         </el-table-column>
         <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
            <template #default="scope">
               <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:config:edit']" >修改</el-button>
               <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:config:remove']">删除</el-button>
            </template>
         </el-table-column>
      </el-table>

      <pagination
         v-show="total > 0"
         :total="total"
         v-model:page="queryParams.pageNum"
         v-model:limit="queryParams.pageSize"
         @pagination="getList"
      />

      <!-- 添加或修改参数配置对话框 -->
      <el-dialog :title="title" v-model="open" width="800px" append-to-body>
         <el-form ref="configRef" :model="form" :rules="rules" label-width="100px">
            <el-form-item label="参数名称" prop="configName">
               <el-input v-model="form.configName" placeholder="请输入参数名称" />
            </el-form-item>
            <el-form-item label="参数键名" prop="configKey">
               <el-input v-model="form.configKey" placeholder="请输入参数键名" />
            </el-form-item>
            <el-form-item label="参数键值" prop="configValue">
               <!-- 文本框 -->
               <el-input 
                  v-if="!form.configValType || form.configValType === 'text'" 
                  v-model="form.configValue" 
                  placeholder="请输入参数键值" 
               />
               <!-- 数字框 -->
               <el-input 
                  v-else-if="form.configValType === 'number'" 
                  v-model="form.configValue" 
                  type="number"
                  placeholder="请输入数字" 
               />
               <!-- 多行文本 -->
               <el-input 
                  v-else-if="form.configValType === 'textarea'" 
                  v-model="form.configValue" 
                  type="textarea" 
                  :rows="4"
                  placeholder="请输入参数键值" 
               />
               <!-- 图片上传 -->
               <image-upload 
                  v-else-if="form.configValType === 'imageUpload'" 
                  v-model="form.configValue"
               />
               <!-- 文件上传 -->
               <file-upload 
                  v-else-if="form.configValType === 'fileUpload'" 
                  v-model="form.configValue"
               />
               <!-- 单选按钮 -->
               <el-radio-group 
                  v-else-if="form.configValType === 'option' && metadataOptions.length > 0" 
                  v-model="form.configValue"
               >
                  <el-radio 
                     v-for="option in metadataOptions" 
                     :key="option.value" 
                     :value="option.value"
                  >
                     {{ option.label }}
                  </el-radio>
               </el-radio-group>
               <!-- 下拉选择 -->
               <el-select 
                  v-else-if="form.configValType === 'select' && metadataOptions.length > 0" 
                  v-model="form.configValue" 
                  placeholder="请选择"
                  style="width: 100%"
               >
                  <el-option 
                     v-for="option in metadataOptions" 
                     :key="option.value" 
                     :label="option.label" 
                     :value="option.value"
                  />
               </el-select>
               <!-- 多选框 -->
               <el-checkbox-group 
                  v-else-if="form.configValType === 'checkbox' && metadataOptions.length > 0" 
                  v-model="checkboxValues"
               >
                  <el-checkbox 
                     v-for="option in metadataOptions" 
                     :key="option.value" 
                     :value="option.value"
                  >
                     {{ option.label }}
                  </el-checkbox>
               </el-checkbox-group>
               <!-- 开关 -->
               <el-switch 
                  v-else-if="form.configValType === 'switch'" 
                  v-model="switchValue"
                  active-text="开启"
                  inactive-text="关闭"
               />
               <!-- 日期选择 -->
               <el-date-picker 
                  v-else-if="form.configValType === 'date'" 
                  v-model="form.configValue" 
                  type="date"
                  placeholder="请选择日期"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
               />
               <!-- 日期时间选择 -->
               <el-date-picker 
                  v-else-if="form.configValType === 'datetime'" 
                  v-model="form.configValue" 
                  type="datetime"
                  placeholder="请选择日期时间"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  style="width: 100%"
               />
               <!-- 时间选择 -->
               <el-time-picker 
                  v-else-if="form.configValType === 'time'" 
                  v-model="form.configValue" 
                  placeholder="请选择时间"
                  value-format="HH:mm:ss"
                  style="width: 100%"
               />
               <!-- 颜色选择 -->
               <el-color-picker 
                  v-else-if="form.configValType === 'color'" 
                  v-model="form.configValue"
               />
               <!-- 接口下拉选择 -->
               <el-select 
                  v-else-if="form.configValType === 'apiSelect'" 
                  v-model="form.configValue" 
                  placeholder="请选择"
                  style="width: 100%"
                  :loading="apiSelectLoading"
               >
                  <el-option 
                     v-for="option in apiSelectOptions" 
                     :key="option.value" 
                     :label="option.label" 
                     :value="option.value"
                  />
               </el-select>
               <!-- 未配置选项时的提示 -->
               <el-alert 
                  v-else-if="(form.configValType === 'option' || form.configValType === 'select' || form.configValType === 'checkbox') && metadataOptions.length === 0"
                  title="请先在下方配置可选项" 
                  type="warning" 
                  :closable="false"
               />
               <!-- 默认文本框 -->
               <el-input 
                  v-else 
                  v-model="form.configValue" 
                  placeholder="请输入参数键值" 
               />
            </el-form-item>
            <el-form-item label="备注" prop="remark">
               <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
            </el-form-item>
            
            <!-- 展开/收起高级配置按钮 -->
            <el-form-item label-width="0" style="text-align: center;">
               <el-button 
                  type="primary" 
                  link 
                  @click="showAdvancedConfig = !showAdvancedConfig"
                  :icon="showAdvancedConfig ? 'ArrowUp' : 'ArrowDown'"
               >
                  {{ showAdvancedConfig ? '收起配置类型' : '展开配置类型' }}
               </el-button>
            </el-form-item>

            <!-- 高级配置区域 -->
            <template v-if="showAdvancedConfig">
               <el-form-item label="配置值类型" prop="configValType">
                  <el-select v-model="form.configValType" placeholder="请选择配置值类型" style="width: 100%">
                     <el-option label="文本框" value="text" />
                     <el-option label="数字框" value="number" />
                     <el-option label="多行文本" value="textarea" />
                     <el-option label="图片上传" value="imageUpload" />
                     <el-option label="文件上传" value="fileUpload" />
                     <el-option label="单选按钮" value="option" />
                     <el-option label="下拉选择" value="select" />
                     <el-option label="多选框" value="checkbox" />
                     <el-option label="开关" value="switch" />
                     <el-option label="日期选择" value="date" />
                     <el-option label="日期时间" value="datetime" />
                     <el-option label="时间选择" value="time" />
                     <el-option label="颜色选择" value="color" />
                     <el-option label="接口下拉" value="apiSelect" />
                  </el-select>
               </el-form-item>
               
               <!-- metadata配置项，option、select、checkbox时显示 -->
               <el-form-item label="可选项配置" v-if="form.configValType === 'option' || form.configValType === 'select' || form.configValType === 'checkbox'">
                  <div v-for="(item, index) in metadataOptions" :key="index" style="display: flex; margin-bottom: 10px;">
                     <el-input v-model="item.label" placeholder="选项标签" style="width: 45%; margin-right: 10px;" />
                     <el-input v-model="item.value" placeholder="选项值" style="width: 45%; margin-right: 10px;" />
                     <el-button type="danger" icon="Delete" @click="removeMetadataOption(index)" circle />
                  </div>
                  <el-button type="primary" icon="Plus" @click="addMetadataOption" plain>添加选项</el-button>
               </el-form-item>
               
               <!-- 接口下拉配置 -->
               <el-form-item label="接口配置" v-if="form.configValType === 'apiSelect'">
                  <div style="width: 100%">
                     <div style="margin-bottom: 10px;">
                        <label style="display: inline-block; width: 80px;">接口路径：</label>
                        <el-input v-model="apiConfig.url" placeholder="例如: /system/dept/list" style="width: calc(100% - 80px);" />
                     </div>
                     <div style="margin-bottom: 10px;">
                        <label style="display: inline-block; width: 80px;">数据路径：</label>
                        <el-input v-model="apiConfig.dataPath" placeholder="例如: data 或 data.rows" style="width: calc(100% - 80px);">
                           <template #append>
                              <el-tooltip content="数据在响应中的路径，支持点号分隔，如: data.rows" placement="top">
                                 <el-icon><QuestionFilled /></el-icon>
                              </el-tooltip>
                           </template>
                        </el-input>
                     </div>
                     <div style="margin-bottom: 10px;">
                        <label style="display: inline-block; width: 80px;">标签字段：</label>
                        <el-input v-model="apiConfig.labelField" placeholder="例如: deptName" style="width: calc(100% - 80px);" />
                     </div>
                     <div>
                        <label style="display: inline-block; width: 80px;">值字段：</label>
                        <el-input v-model="apiConfig.valueField" placeholder="例如: deptId" style="width: calc(100% - 80px);" />
                     </div>
                  </div>
               </el-form-item>
               
               <el-form-item label="系统内置" prop="configType">
                  <el-radio-group v-model="form.configType">
                     <el-radio
                        v-for="dict in sys_yes_no"
                        :key="dict.value"
                        :value="dict.value"
                     >{{ dict.label }}</el-radio>
                  </el-radio-group>
               </el-form-item>
            </template>
         </el-form>
         <template #footer>
            <div class="dialog-footer">
               <el-button type="primary" @click="submitForm">确 定</el-button>
               <el-button @click="cancel">取 消</el-button>
            </div>
         </template>
      </el-dialog>
   </div>
</template>

<script setup name="Config">
import { listConfig, getConfig, delConfig, addConfig, updateConfig, refreshCache } from "@/api/system/config"

const { proxy } = getCurrentInstance()
const { sys_yes_no } = proxy.useDict("sys_yes_no")

const configList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")
const dateRange = ref([])
const metadataOptions = ref([])
const showAdvancedConfig = ref(false)
const checkboxValues = ref([])
const switchValue = ref(false)
const apiConfig = ref({ url: '', dataPath: 'rows', labelField: '', valueField: '' })
const apiSelectOptions = ref([])
const apiSelectLoading = ref(false)

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    configName: undefined,
    configKey: undefined,
    configType: undefined
  },
  rules: {
    configName: [{ required: true, message: "参数名称不能为空", trigger: "blur" }],
    configKey: [{ required: true, message: "参数键名不能为空", trigger: "blur" }],
    configValue: [{ required: true, message: "参数键值不能为空", trigger: "blur" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询参数列表 */
function getList() {
  loading.value = true
  listConfig(proxy.addDateRange(queryParams.value, dateRange.value)).then(response => {
    configList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

/** 表单重置 */
function reset() {
  form.value = {
    configId: undefined,
    configName: undefined,
    configKey: undefined,
    configValue: undefined,
    configType: "Y",
    configValType: undefined,
    metadata: undefined,
    remark: undefined
  }
  metadataOptions.value = []
  showAdvancedConfig.value = false
  checkboxValues.value = []
  switchValue.value = false
  apiConfig.value = { url: '', dataPath: 'rows', labelField: '', valueField: '' }
  apiSelectOptions.value = []
  apiSelectLoading.value = false
  proxy.resetForm("configRef")
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  dateRange.value = []
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.configId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加参数"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const configId = row.configId || ids.value
  getConfig(configId).then(response => {
    form.value = response.data
    
    // 如果已配置configValType，自动展开高级配置
    if (form.value.configValType) {
      showAdvancedConfig.value = true
    }
    
    // 解析metadata
    if (form.value.metadata) {
      try {
        const metadata = JSON.parse(form.value.metadata)
        
        // 根据类型解析metadata
        if (form.value.configValType === 'apiSelect') {
          apiConfig.value = metadata
          // 等待DOM更新后加载接口数据
          nextTick(() => {
            loadApiSelectOptions()
          })
        } else if (form.value.configValType === 'option' || form.value.configValType === 'select' || form.value.configValType === 'checkbox') {
          metadataOptions.value = metadata
          // checkbox需要解析数组值
          if (form.value.configValType === 'checkbox') {
            try {
              checkboxValues.value = JSON.parse(form.value.configValue) || []
            } catch (e) {
              checkboxValues.value = form.value.configValue ? form.value.configValue.split(',') : []
            }
          }
        }
      } catch (e) {
        console.error('解析metadata失败:', e)
        metadataOptions.value = []
        apiConfig.value = { url: '', dataPath: 'rows', labelField: '', valueField: '' }
      }
    }
    
    // switch类型的值转换
    if (form.value.configValType === 'switch') {
      switchValue.value = form.value.configValue === 'true' || form.value.configValue === '1'
    }
    
    open.value = true
    title.value = "修改参数"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["configRef"].validate(valid => {
    if (valid) {
      // 根据类型处理configValue和metadata
      if (form.value.configValType === 'checkbox') {
        // checkbox: 值为数组的JSON字符串，metadata为选项配置
        form.value.configValue = JSON.stringify(checkboxValues.value)
        form.value.metadata = JSON.stringify(metadataOptions.value)
      } else if (form.value.configValType === 'switch') {
        // switch: 值为字符串true/false
        form.value.configValue = switchValue.value ? 'true' : 'false'
        form.value.metadata = null
      } else if (form.value.configValType === 'apiSelect') {
        // apiSelect: metadata为接口配置
        form.value.metadata = JSON.stringify(apiConfig.value)
      } else if (form.value.configValType === 'option' || form.value.configValType === 'select') {
        // option/select: metadata为选项配置
        form.value.metadata = JSON.stringify(metadataOptions.value)
      } else {
        // 其他类型不需要metadata
        form.value.metadata = null
      }
      
      if (form.value.configId != undefined) {
        updateConfig(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addConfig(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const configIds = row.configId || ids.value
  proxy.$modal.confirm('是否确认删除参数编号为"' + configIds + '"的数据项？').then(function () {
    return delConfig(configIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download("system/config/export", {
    ...queryParams.value
  }, `config_${new Date().getTime()}.xlsx`)
}

/** 刷新缓存按钮操作 */
function handleRefreshCache() {
  refreshCache().then(() => {
    proxy.$modal.msgSuccess("刷新缓存成功")
  })
}

/** 添加metadata选项 */
function addMetadataOption() {
  metadataOptions.value.push({
    label: '',
    value: ''
  })
}

/** 删除metadata选项 */
function removeMetadataOption(index) {
  metadataOptions.value.splice(index, 1)
}

/** 加载接口下拉选择的数据 */
function loadApiSelectOptions() {
  const config = apiConfig.value
  
  // 验证配置是否完整
  if (!config.url || !config.dataPath || !config.labelField || !config.valueField) {
    apiSelectOptions.value = []
    return
  }
  
  apiSelectLoading.value = true
  
  // 使用request发送请求
  import("@/utils/request").then(({ default: request }) => {
    request({
      url: config.url,
      method: 'get'
    }).then(response => {
      try {
        // 根据dataPath获取数据
        const paths = config.dataPath.split('.')
        let data = response
        
        for (const path of paths) {
          if (data && typeof data === 'object' && path in data) {
            data = data[path]
          } else {
            throw new Error(`无法找到路径: ${config.dataPath}`)
          }
        }
        
        // 验证data是数组
        if (!Array.isArray(data)) {
          throw new Error('数据路径必须指向一个数组')
        }
        
        // 转换为选项格式
        apiSelectOptions.value = data.map(item => ({
          label: item[config.labelField],
          value: String(item[config.valueField])
        }))
      } catch (error) {
        console.error('解析接口数据失败:', error)
        proxy.$modal.msgError('加载数据失败: ' + error.message)
        apiSelectOptions.value = []
      }
    }).catch(error => {
      console.error('请求接口失败:', error)
      proxy.$modal.msgError('请求接口失败')
      apiSelectOptions.value = []
    }).finally(() => {
      apiSelectLoading.value = false
    })
  })
}

// 监听apiConfig变化，自动加载数据
watch(() => [apiConfig.value.url, apiConfig.value.dataPath, apiConfig.value.labelField, apiConfig.value.valueField], 
  () => {
    if (form.value.configValType === 'apiSelect') {
      loadApiSelectOptions()
    }
  },
  { deep: true }
)

getList()
</script>

