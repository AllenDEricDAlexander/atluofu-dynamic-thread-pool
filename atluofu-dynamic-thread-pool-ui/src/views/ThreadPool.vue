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
            <div class="header-right">
              <el-tag v-if="dataState === 'stale'" type="warning" effect="plain" size="small">
                <el-icon><Warning /></el-icon>
                数据可能已过期，请等待上报
              </el-tag>
              <el-tag type="info">共 {{ total }} 个线程池</el-tag>
            </div>
          </div>
        </template>

        <el-table
          :data="threadPoolList"
          v-loading="loading"
          style="width: 100%"
          :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
          row-key="threadPoolName"
          border
        >
          <el-table-column prop="appName" label="应用名称" :resizable="true" :min-width="100" />
          <el-table-column prop="threadPoolName" label="线程池名称" :resizable="true" :min-width="100" />
          <el-table-column prop="corePoolSize" label="核心线程数" :resizable="true" :min-width="80" align="center" />
          <el-table-column prop="maximumPoolSize" label="最大线程数" :resizable="true" :min-width="80" align="center" />
          <el-table-column prop="activeCount" label="活跃线程数" :resizable="true" :min-width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="getActiveType(row.activeCount, row.maximumPoolSize)">
                {{ row.activeCount }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="poolSize" label="当前池中线程数" :resizable="true" :min-width="90" align="center" />
          <el-table-column prop="queueType" label="队列类型" :resizable="true" :min-width="100" />
          <el-table-column prop="queueSize" label="队列任务数" :resizable="true" :min-width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="getQueueType(row.queueSize, row.remainingCapacity)">
                {{ row.queueSize }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="remainingCapacity" label="队列剩余容量" :resizable="true" :min-width="90" align="center" />
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
        <el-form-item label="线程池名称">
          <el-input v-model="editForm.threadPoolName" disabled />
        </el-form-item>
        <el-form-item label="核心线程数">
          <el-input-number 
            v-model="editForm.corePoolSize" 
            :min="1" 
            :max="editForm.maximumPoolSize"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="最大线程数">
          <el-input-number 
            v-model="editForm.maximumPoolSize" 
            :min="editForm.corePoolSize" 
            :max="1000"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="当前状态">
          <div class="status-info">
            <el-tag type="info">活跃线程：{{ editForm.activeCount }}</el-tag>
            <el-tag type="warning" style="margin-left: 10px">队列任务：{{ editForm.queueSize }}</el-tag>
            <el-tag type="success" style="margin-left: 10px">剩余容量：{{ editForm.remainingCapacity }}</el-tag>
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
import { queryThreadPoolList, queryThreadPoolConfig, updateThreadPoolConfig } from '@/api/threadPool'
import { Monitor, List, Edit, Refresh, VideoPlay, VideoPause, Warning } from '@element-plus/icons-vue'

// 数据
const threadPoolList = ref([])
const total = ref(0)
const loading = ref(false)
const autoRefresh = ref(false)
const autoRefreshInterval = ref(null)
const editDialogVisible = ref(false)
const updating = ref(false)

// 数据状态
const dataState = ref('loading') // 'loading' | 'normal' | 'stale'
const lastFetchTime = ref(0)

const editForm = ref({
  appName: '',
  threadPoolName: '',
  corePoolSize: 0,
  maximumPoolSize: 0,
  activeCount: 0,
  queueSize: 0,
  remainingCapacity: 0
})

// 获取活跃线程类型
const getActiveType = (activeCount, maximumPoolSize) => {
  const rate = activeCount / maximumPoolSize
  if (rate >= 0.8) return 'danger'
  if (rate >= 0.5) return 'warning'
  return 'success'
}

// 获取队列类型
const getQueueType = (queueSize, remainingCapacity) => {
  const total = queueSize + remainingCapacity
  const rate = total > 0 ? queueSize / total : 0
  if (rate >= 0.8) return 'danger'
  if (rate >= 0.5) return 'warning'
  return 'success'
}

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    const res = await queryThreadPoolList()
    const newData = res.data || []
    lastFetchTime.value = Date.now()
    if (newData.length > 0) {
      threadPoolList.value = newData
      total.value = newData.length
      dataState.value = 'normal'
    } else {
      // 数据为空时保留上一次状态，显示"数据过期"提示
      dataState.value = 'stale'
    }
  } catch (error) {
    console.error('获取数据失败', error)
    // 请求失败时保留上一次数据
    dataState.value = 'stale'
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
    const res = await queryThreadPoolConfig({
      appName: row.appName,
      threadPoolName: row.threadPoolName
    })
    
    // 如果查询返回 null，使用列表中的数据
    const data = res.data || row
    
    editForm.value = {
      appName: data.appName || row.appName,
      threadPoolName: data.threadPoolName || row.threadPoolName,
      corePoolSize: data.corePoolSize || row.corePoolSize,
      maximumPoolSize: data.maximumPoolSize || row.maximumPoolSize,
      activeCount: data.activeCount || row.activeCount,
      queueSize: data.queueSize || row.queueSize,
      remainingCapacity: data.remainingCapacity || row.remainingCapacity
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
    await updateThreadPoolConfig({
      appName: editForm.value.appName,
      threadPoolName: editForm.value.threadPoolName,
      corePoolSize: editForm.value.corePoolSize,
      maximumPoolSize: editForm.value.maximumPoolSize
    })
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
  flex-wrap: wrap;
  gap: 8px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
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
}
</style>
