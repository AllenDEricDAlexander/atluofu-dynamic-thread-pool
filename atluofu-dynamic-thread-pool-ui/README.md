# 动态线程池可视化管理平台

基于 Vue 3 + Element Plus 的动态线程池管理前端应用

## 技术栈

- Vue 3 - 渐进式 JavaScript 框架
- Element Plus - 基于 Vue 3 的组件库
- Vue Router - 官方路由管理器
- Axios - HTTP 客户端
- Vite - 下一代前端构建工具

## 功能特性

- 📊 实时查看线程池状态（核心线程数、最大线程数、活跃线程数等）
- 🔄 自动刷新（每 3 秒）
- ✏️ 在线编辑线程池配置
- 📈 队列状态监控（队列任务数、剩余容量）
- 🎨 现代化 UI 设计

## 项目结构

```
atluofu-dynamic-thread-pool-ui/
├── src/
│   ├── api/              # API 接口
│   │   └── threadPool.js
│   ├── assets/           # 静态资源
│   ├── components/       # 公共组件
│   ├── router/           # 路由配置
│   │   └── index.js
│   ├── utils/            # 工具函数
│   │   └── request.js
│   ├── views/            # 页面组件
│   │   └── ThreadPool.vue
│   ├── App.vue           # 根组件
│   └── main.js           # 入口文件
├── index.html
├── package.json
├── vite.config.js
└── README.md
```

## 快速开始

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

访问 http://localhost:3000

### 构建生产版本

```bash
npm run build
```

### 预览生产构建

```bash
npm run preview
```

## 后端接口

本应用需要配合 `dynamic-thread-pool-admin` 后端服务使用：

| 接口 | 方法 | 描述 |
|------|------|------|
| /api/v1/dynamic/thread/pool/query_thread_pool_list | GET | 查询线程池列表 |
| /api/v1/dynamic/thread/pool/query_thread_pool_config | GET | 查询线程池配置 |
| /api/v1/dynamic/thread/pool/update_thread_pool_config | POST | 更新线程池配置 |

## 代理配置

开发环境下，通过 Vite 代理将 API 请求转发到后端服务（默认 http://localhost:8089）

修改 `vite.config.js` 可以更改代理配置：

```javascript
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8089',
      changeOrigin: true
    }
  }
}
```

## 截图预览

- 线程池列表展示
- 实时状态监控
- 在线编辑配置

## 与旧版对比

| 特性 | 旧版 (docs/front) | 新版 (UI) |
|------|------------------|----------|
| 框架 | 原生 HTML/JS | Vue 3 + Element Plus |
| 组件化 | ❌ | ✅ |
| 响应式 | ❌ | ✅ |
| 自动刷新 | ✅ | ✅ (可切换) |
| UI 美观度 | 基础 | 现代化 |
| 可维护性 | 低 | 高 |

## License

MIT
