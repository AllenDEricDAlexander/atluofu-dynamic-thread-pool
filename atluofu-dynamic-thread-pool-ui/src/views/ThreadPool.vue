<template>
  <div class="thread-pool-container">
    <el-header class="page-header">
      <div class="header-content">
        <h1 class="page-title">
          <el-icon><Monitor /></el-icon>
          动态线程池管理平台
        </h1>
        <div class="header-actions">
          <el-tag effect="dark" :type="autoRefresh ? 'success' : 'info'">
            {{ autoRefresh ? '自动刷新中' : '已停止刷新' }}
          </el-tag>
          <el-button 
            :type="autoRefresh ? 'warning' : 'primary'" 
            @click="toggleAutoRefresh"
            :icon="autoRefresh ? 'VideoPause' : 'VideoPlay'"
          >
            {{ autoRefresh ? '停止刷新' : '自动刷新' }}
          </el-button>
          <el-button type="success" @click="fetchData" :icon="Refresh">
            立即刷新
          </el-button>
        </div>
      </div>
    </el-header>

    <el-main class="page-main">
      <el-card shadow="hover">
        <template #header>
          <div class="card-header">
            <span class="card-title">
              <el-icon><List /></el-icon>
              线程池列表
            </span>
            <el-tag type="info">共 {{ total }} 个线程池</el-tag>
          </div>
        </template>

        <el-table
          :data="threadPoolList"
          v-loading="loading"
          style="width: 100%"
          :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
          row-key="rowKey"
          border
        >
          <el-table-column prop="appName" label="应用名称" :resizable="true" :min-width="100" :formatter="formatColumnValue" />
          <el-table-column prop="instanceId" label="实例 ID" :resizable="true" :min-width="120" :formatter="formatColumnValue" />
          <el-table-column prop="executorName" label="执行器名称" :resizable="true" :min-width="120" :formatter="formatColumnValue" />
          <el-table-column prop="executorKind" label="执行器类型" :resizable="true" :min-width="160" :formatter="formatColumnValue" />
          <el-table-column prop="corePoolSize" label="核心线程数" :resizable="true" :min-width="90" align="center" :formatter="formatColumnValue" />
          <el-table-column prop="maximumPoolSize" label="最大线程数" :resizable="true" :min-width="90" align="center" :formatter="formatColumnValue" />
          <el-table-column prop="activeCount" label="活跃线程数" :resizable="true" :min-width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="getActiveType(row.activeCount, row.maximumPoolSize)">
                {{ formatValue(row.activeCount) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="poolSize" label="当前池中线程数" :resizable="true" :min-width="110" align="center" :formatter="formatColumnValue" />
          <el-table-column prop="queueSize" label="队列任务数" :resizable="true" :min-width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="getQueueType(row.queueSize, row.remainingCapacity)">
                {{ formatValue(row.queueSize) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="remainingCapacity" label="队列剩余容量" :resizable="true" :min-width="110" align="center" :formatter="formatColumnValue" />
          <el-table-column prop="concurrencyLimit" label="并发限制" :resizable="true" :min-width="90" align="center" :formatter="formatColumnValue" />
          <el-table-column prop="runningTasks" label="运行任务数" :resizable="true" :min-width="90" align="center" :formatter="formatColumnValue" />
          <el-table-column prop="submittedTasks" label="提交任务数" :resizable="true" :min-width="90" align="center" :formatter="formatColumnValue" />
          <el-table-column prop="completedTasks" label="完成任务数" :resizable="true" :min-width="90" align="center" :formatter="formatColumnValue" />
          <el-table-column prop="failedTasks" label="失败任务数" :resizable="true" :min-width="90" align="center" :formatter="formatColumnValue" />
          <el-table-column prop="rejectedTasks" label="拒绝任务数" :resizable="true" :min-width="90" align="center" :formatter="formatColumnValue" />
          <el-table-column prop="availablePermits" label="可用许可数" :resizable="true" :min-width="90" align="center" :formatter="formatColumnValue" />
          <el-table-column label="操作" :resizable="true" :min-width="80" align="center" fixed="right">
            <template #default="{ row }">
              <el-button
                type="primary"
                size="small"
                @click="openEditDialog(row)"
                :icon="Edit"
              >
                编辑
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-main>

    <!-- 编辑对话框 -->
    <el-dialog 
      v-model="editDialogVisible" 
      title="编辑线程池配置" 
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="editForm" label-width="100px" label-position="left">
        <el-form-item label="应用名称">
          <el-input v-model="editForm.appName" disabled />
        </el-form-item>
        <el-form-item label="实例 ID">
          <el-input v-model="editForm.instanceId" disabled />
        </el-form-item>
        <el-form-item label="执行器名称">
          <el-input v-model="editForm.executorName" disabled />
        </el-form-item>
        <el-form-item label="执行器类型">
          <el-input v-model="editForm.executorKind" disabled />
        </el-form-item>
        <el-form-item v-if="!isVirtualExecutor(editForm)" label="核心线程数">
          <el-input-number 
            v-model="editForm.corePoolSize" 
            :min="1" 
            :max="editForm.maximumPoolSize"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item v-if="!isVirtualExecutor(editForm)" label="最大线程数">
          <el-input-number 
            v-model="editForm.maximumPoolSize" 
            :min="editForm.corePoolSize" 
            :max="1000"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item v-if="!isVirtualExecutor(editForm)" label="存活秒数">
          <el-input-number
            v-model="editForm.keepAliveSeconds"
            :min="0"
            :max="86400"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item v-if="!isVirtualExecutor(editForm) && hasKnownValue(editForm.allowCoreThreadTimeOut)" label="核心超时">
          <el-switch v-model="editForm.allowCoreThreadTimeOut" />
        </el-form-item>
        <el-form-item v-if="!isVirtualExecutor(editForm) && !hasKnownValue(editForm.allowCoreThreadTimeOut)" label="核心超时">
          <el-input placeholder="未知" disabled />
        </el-form-item>
        <el-form-item v-if="isVirtualExecutor(editForm)" label="并发限制">
          <el-input-number
            v-model="editForm.concurrencyLimit"
            :min="1"
            :max="1000000"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="当前状态">
          <div class="status-info">
            <el-tag type="info">活跃线程：{{ formatValue(editForm.activeCount) }}</el-tag>
            <el-tag type="warning">队列任务：{{ formatValue(editForm.queueSize) }}</el-tag>
            <el-tag type="success">剩余容量：{{ formatValue(editForm.remainingCapacity) }}</el-tag>
            <el-tag type="primary">运行任务：{{ formatValue(editForm.runningTasks) }}</el-tag>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUpdate" :loading="updating">
          确认修改
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  queryApps,
  queryInstances,
  queryExecutors,
  queryExecutor,
  resizeExecutor,
  updateVirtualLimit
} from '@/api/threadPool'

// 数据
const threadPoolList = ref([])
const total = ref(0)
const loading = ref(false)
const autoRefresh = ref(false)
const autoRefreshInterval = ref(null)
const editDialogVisible = ref(false)
const updating = ref(false)

const editForm = ref({
  appName: '',
  instanceId: '',
  executorName: '',
  executorKind: '',
  corePoolSize: null,
  maximumPoolSize: null,
  keepAliveSeconds: null,
  allowCoreThreadTimeOut: null,
  activeCount: null,
  poolSize: null,
  queueSize: null,
  remainingCapacity: null,
  concurrencyLimit: null,
  runningTasks: null,
  submittedTasks: null,
  completedTasks: null,
  failedTasks: null,
  rejectedTasks: null,
  availablePermits: null
})

const formatValue = value => value === null || value === undefined ? '-' : value

const formatColumnValue = (row, column, cellValue) => formatValue(cellValue)

const isVirtualExecutor = row => row.executorKind === 'VIRTUAL_THREAD_PER_TASK'

const hasKnownValue = value => value !== null && value !== undefined

const hasPayloadValue = value => value !== null && value !== undefined && value !== ''

// 获取活跃线程类型
const getActiveType = (activeCount, maximumPoolSize) => {
  if (activeCount === null || activeCount === undefined || !maximumPoolSize) return 'info'
  const rate = activeCount / maximumPoolSize
  if (rate >= 0.8) return 'danger'
  if (rate >= 0.5) return 'warning'
  return 'success'
}

// 获取队列类型
const getQueueType = (queueSize, remainingCapacity) => {
  if (queueSize === null || queueSize === undefined || remainingCapacity === null || remainingCapacity === undefined) return 'info'
  const total = queueSize + remainingCapacity
  const rate = total > 0 ? queueSize / total : 0
  if (rate >= 0.8) return 'danger'
  if (rate >= 0.5) return 'warning'
  return 'success'
}

const getList = data => Array.isArray(data) ? data : []

const getIdentityValue = (item, keys) => {
  if (typeof item === 'string') return item
  for (const key of keys) {
    if (item && item[key] !== null && item[key] !== undefined) return item[key]
  }
  return ''
}

const normalizeExecutor = (executor, appName, instanceId) => ({
  ...executor,
  appName: executor.appName || appName,
  instanceId: executor.instanceId || instanceId,
  executorName: executor.executorName,
  completedTasks: executor.completedTasks ?? executor.completedTaskCount,
  rowKey: `${executor.appName || appName}:${executor.instanceId || instanceId}:${executor.executorName}`
})

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    const appsRes = await queryApps()
    const apps = getList(appsRes.data)
    const instanceGroups = await Promise.all(apps.map(async app => {
      const appName = getIdentityValue(app, ['appName', 'name'])
      if (!appName) return []
      const instancesRes = await queryInstances(appName)
      const instances = getList(instancesRes.data)
      return Promise.all(instances.map(async instance => {
        const instanceId = getIdentityValue(instance, ['instanceId', 'id'])
        if (!instanceId) return []
        const executorsRes = await queryExecutors(appName, instanceId)
        return getList(executorsRes.data).map(executor => normalizeExecutor(executor, appName, instanceId))
      }))
    }))
    threadPoolList.value = instanceGroups.flat(2)
    total.value = threadPoolList.value.length
  } catch (error) {
    console.error('获取数据失败', error)
  } finally {
    loading.value = false
  }
}

// 切换自动刷新
const toggleAutoRefresh = () => {
  if (autoRefresh.value) {
    stopAutoRefresh()
  } else {
    startAutoRefresh()
  }
}

// 开始自动刷新
const startAutoRefresh = () => {
  autoRefresh.value = true
  fetchData()
  autoRefreshInterval.value = setInterval(() => {
    fetchData()
  }, 3000)
  ElMessage.success('已开始自动刷新（每 3 秒）')
}

// 停止自动刷新
const stopAutoRefresh = () => {
  autoRefresh.value = false
  if (autoRefreshInterval.value) {
    clearInterval(autoRefreshInterval.value)
    autoRefreshInterval.value = null
  }
  ElMessage.info('已停止自动刷新')
}

// 打开编辑对话框
const openEditDialog = async (row) => {
  try {
    const res = await queryExecutor(row.appName, row.instanceId, row.executorName)
    
    // 如果查询返回 null，使用列表中的数据
    const data = res.data || row
    
    editForm.value = {
      appName: data.appName || row.appName,
      instanceId: data.instanceId || row.instanceId,
      executorName: data.executorName || row.executorName,
      executorKind: data.executorKind || row.executorKind,
      corePoolSize: data.corePoolSize ?? row.corePoolSize ?? null,
      maximumPoolSize: data.maximumPoolSize ?? row.maximumPoolSize ?? null,
      keepAliveSeconds: data.keepAliveSeconds ?? row.keepAliveSeconds ?? null,
      allowCoreThreadTimeOut: data.allowCoreThreadTimeOut ?? row.allowCoreThreadTimeOut ?? null,
      activeCount: data.activeCount ?? row.activeCount ?? null,
      poolSize: data.poolSize ?? row.poolSize ?? null,
      queueSize: data.queueSize ?? row.queueSize ?? null,
      remainingCapacity: data.remainingCapacity ?? row.remainingCapacity ?? null,
      concurrencyLimit: data.concurrencyLimit ?? row.concurrencyLimit ?? null,
      runningTasks: data.runningTasks ?? row.runningTasks ?? null,
      submittedTasks: data.submittedTasks ?? row.submittedTasks ?? null,
      completedTasks: data.completedTasks ?? row.completedTasks ?? null,
      failedTasks: data.failedTasks ?? row.failedTasks ?? null,
      rejectedTasks: data.rejectedTasks ?? row.rejectedTasks ?? null,
      availablePermits: data.availablePermits ?? row.availablePermits ?? null
    }
    editDialogVisible.value = true
  } catch (error) {
    console.error('获取配置失败', error)
    ElMessage.error('获取配置失败，请重试')
  }
}

// 更新配置
const handleUpdate = async () => {
  updating.value = true
  try {
    if (isVirtualExecutor(editForm.value)) {
      await updateVirtualLimit(editForm.value.appName, editForm.value.instanceId, editForm.value.executorName, {
        concurrencyLimit: editForm.value.concurrencyLimit,
        operator: 'admin'
      })
    } else {
      const resizePayload = {
        corePoolSize: editForm.value.corePoolSize,
        maximumPoolSize: editForm.value.maximumPoolSize,
        operator: 'admin'
      }
      if (hasPayloadValue(editForm.value.keepAliveSeconds)) {
        resizePayload.keepAliveSeconds = editForm.value.keepAliveSeconds
      }
      if (hasKnownValue(editForm.value.allowCoreThreadTimeOut)) {
        resizePayload.allowCoreThreadTimeOut = editForm.value.allowCoreThreadTimeOut
      }
      await resizeExecutor(editForm.value.appName, editForm.value.instanceId, editForm.value.executorName, resizePayload)
    }
    ElMessage.success('配置更新成功')
    editDialogVisible.value = false
    fetchData()
  } catch (error) {
    console.error('更新失败', error)
  } finally {
    updating.value = false
  }
}

// 生命周期
onMounted(() => {
  fetchData()
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<style scoped>
.thread-pool-container {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.page-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 0;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
  padding: 0 20px;
}

.page-title {
  color: #fff;
  font-size: 24px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.page-main {
  padding: 20px;
  flex: 1;
  overflow: auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-info {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
</style>
