# 动态线程池管理平台

## 📖 项目介绍

动态线程池管理平台是一个基于 Redis + Spring Boot + Vue 的线程池动态配置和监控系统。通过该平台，可以实时查看线程池运行状态，并动态调整线程池配置（核心线程数、最大线程数），无需重启应用即可生效。

### 核心特性

- ✅ **动态调整**：实时调整线程池核心参数，无需重启应用
- ✅ **实时监控**：实时查看线程池运行状态（活跃线程数、队列大小等）
- ✅ **Redis 持久化**：线程池配置持久化到 Redis，应用重启后自动恢复
- ✅ **可视化管理**：基于 Vue 的前端管理界面，操作简便
- ✅ **多应用支持**：支持多个应用实例的线程池统一管理

---

## 🏗️ 系统架构

```
┌─────────────────────────────────────────────────────────────────┐
│                     前端管理界面 (Vue 3)                         │
│                    http://localhost:3000                        │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Admin 管理后台 (Spring Boot)                    │
│                    http://localhost:8089                        │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  DynamicThreadPoolController                             │   │
│  │  - query_thread_pool_list    查询线程池列表              │   │
│  │  - query_thread_pool_config  查询线程池配置              │   │
│  │  - update_thread_pool_config 更新线程池配置              │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                          Redis                                   │
│  - THREAD_POOL_CONFIG_LIST_KEY: 线程池配置列表                  │
│  - THREAD_POOL_CONFIG_PARAMETER_LIST_KEY_*: 单个线程池配置      │
│  - DYNAMIC_THREAD_POOL_REDIS_TOPIC_*: 配置变更主题              │
└─────────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┴───────────────┐
              │                               │
              ▼                               ▼
┌─────────────────────────┐     ┌─────────────────────────────────┐
│   应用 A (SDK 客户端)     │     │   应用 B (SDK 客户端)            │
│  - DynamicThreadPoolService │  │  - 监听 Redis Topic              │
│  - 定时上报 (20s)          │  │  - 动态调整 ThreadPoolExecutor   │
│  - 监听配置变更            │  │  - 上报状态到 Redis              │
└─────────────────────────┘     └─────────────────────────────────┘
```

---

## 📁 项目结构

```
atluofu-dynamic-thread-pool/
├── atluofu-dynamic-thread-pool-spring-boot-starter/  # SDK 核心模块
│   ├── src/main/java/
│   │   └── top/atluofu/middleware/dynamic/thread/pool/sdk/
│   │       ├── config/                    # 自动配置类
│   │       │   ├── DynamicThreadPoolAutoConfig.java
│   │       │   └── DynamicThreadPoolAutoProperties.java
│   │       ├── domain/                    # 领域服务
│   │       │   ├── DynamicThreadPoolService.java
│   │       │   └── model/entity/          # 实体类
│   │       ├── trigger/                   # 触发器
│   │       │   ├── job/                   # 定时任务
│   │       │   │   └── ThreadPoolDataReportJob.java
│   │       │   └── listener/              # 监听器
│   │       │       └── ThreadPoolConfigAdjustListener.java
│   │       └── registry/                  # 注册中心
│   │           └── redis/
│   │               └── RedisRegistry.java
│   └── src/test/java/                     # 单元测试
│
├── atluofu-dynamic-thread-pool-test/      # 测试应用（示例）
│   ├── src/main/java/
│   │   └── top/atluofu/
│   │       ├── Application.java           # 启动类
│   │       └── config/
│   │           ├── ThreadPoolConfig.java  # 线程池配置
│   │           └── ThreadPoolConfigProperties.java
│   └── src/test/java/                     # 集成测试/冒烟测试
│
├── dynamic-thread-pool-admin/             # 管理后台
│   ├── src/main/java/
│   │   └── top/atluofu/middleware/dynamic/thread/pool/
│   │       ├── Application.java
│   │       └── trigger/
│   │           └── DynamicThreadPoolController.java  # REST API
│   └── src/test/java/                     # 单元测试
│
├── atluofu-dynamic-thread-pool-ui/        # 前端管理界面 (Vue 3)
│   ├── src/
│   │   ├── api/               # API 接口
│   │   ├── views/             # 页面组件
│   │   ├── router/            # 路由配置
│   │   └── utils/             # 工具类
│   ├── package.json
│   └── vite.config.js
│
└── docs/                      # 文档和素材
```

---

## 🔧 核心实现原理

### 1. SDK 自动配置 (DynamicThreadPoolAutoConfig)

SDK 模块通过 Spring Boot 自动装配机制，自动注册以下组件：

```java
@Configuration
@EnableConfigurationProperties(DynamicThreadPoolAutoProperties.class)
public class DynamicThreadPoolAutoConfig {
    
    // 1. 创建 Redisson 客户端
    @Bean("dynamicThreadRedissonClient")
    public RedissonClient redissonClient(DynamicThreadPoolAutoProperties properties) {
        // 配置 Redis 连接
    }
    
    // 2. 创建配置调整监听器
    @Bean
    public ThreadPoolConfigAdjustListener threadPoolConfigAdjustListener(...) {
        return new ThreadPoolConfigAdjustListener(...);
    }
    
    // 3. 订阅 Redis Topic，监听配置变更
    @Bean(name = "dynamicThreadPoolRedisTopic")
    @DependsOn("dynamicThreadPollService")
    public RTopic dynamicThreadPoolRedisTopic(...) {
        RTopic topic = redissonClient.getTopic("DYNAMIC_THREAD_POOL_REDIS_TOPIC_" + applicationName);
        topic.addListener(ThreadPoolConfigEntity.class, threadPoolConfigAdjustListener);
        return topic;
    }
    
    // 4. 创建线程池服务
    @Bean("dynamicThreadPollService")
    public DynamicThreadPoolService dynamicThreadPollService(...) {
        // 从 Redis 加载缓存配置，初始化线程池
    }
    
    // 5. 创建定时上报任务
    @Bean
    public ThreadPoolDataReportJob threadPoolDataReportJob(...) {
        return new ThreadPoolDataReportJob(...);
    }
}
```

### 2. 动态调整流程

```
1. 用户在前端页面修改配置
         ↓
2. 前端调用 Admin API (POST /update_thread_pool_config)
         ↓
3. Admin 发布消息到 Redis Topic
         ↓
4. 应用 SDK 的监听器收到消息
         ↓
5. 调用 ThreadPoolExecutor.setCorePoolSize()/setMaximumPoolSize()
         ↓
6. 上报最新状态到 Redis
         ↓
7. 前端轮询获取最新状态
```

### 3. 定时上报机制

```java
public class ThreadPoolDataReportJob {
    
    @Scheduled(cron = "0/20 * * * * ?")  // 每 20 秒执行一次
    public void execReportThreadPoolList() {
        // 1. 查询所有线程池状态
        List<ThreadPoolConfigEntity> entities = dynamicThreadPoolService.queryThreadPoolList();
        
        // 2. 上报到 Redis
        registry.reportThreadPool(entities);
        
        // 3. 逐个上报详细配置
        for (ThreadPoolConfigEntity entity : entities) {
            registry.reportThreadPoolConfigParameter(entity);
        }
    }
}
```

### 4. 配置变更监听器

```java
public class ThreadPoolConfigAdjustListener implements MessageListener<ThreadPoolConfigEntity> {
    
    @Override
    public void onMessage(CharSequence charSequence, ThreadPoolConfigEntity config) {
        // 1. 调整线程池配置
        dynamicThreadPoolService.updateThreadPoolConfig(config);
        
        // 2. 上报最新数据
        List<ThreadPoolConfigEntity> entities = dynamicThreadPoolService.queryThreadPoolList();
        registry.reportThreadPool(entities);
        
        // 3. 上报单个线程池配置
        ThreadPoolConfigEntity current = dynamicThreadPoolService.queryThreadPoolConfigByName(
            config.getThreadPoolName());
        registry.reportThreadPoolConfigParameter(current);
    }
}
```

---

## 🚀 快速开始

### 前置条件

- JDK 17+
- Maven 3.6+
- Redis (需配置密码)
- Node.js 16+

### 1. 配置 Redis

修改配置文件中的 Redis 连接信息：

**Admin 模块** (`dynamic-thread-pool-admin/src/main/resources/application-dev.yml`):
```yaml
redis:
  sdk:
    config:
      host: 127.0.0.1
      port: 6379
      password: YourRedisPassword
```

**Test 模块** (`atluofu-dynamic-thread-pool-test/src/main/resources/application-dev.yml`):
```yaml
dynamic:
  thread:
    pool:
      config:
        enable: true
        host: 127.0.0.1
        port: 6379
        password: YourRedisPassword
```

### 2. 构建项目

```bash
cd /Users/mario/SelfProject/be/atluofu-dynamic-thread-pool
mvn clean install -DskipTests
```

### 3. 启动服务

使用启动脚本：
```bash
./start-all.sh
```

或手动启动：

**启动 Admin 后端** (端口 8089):
```bash
cd dynamic-thread-pool-admin
java -jar target/dynamic-thread-pool-admin-app.jar --spring.profiles.active=dev
```

**启动 Test 应用** (端口 8093):
```bash
cd atluofu-dynamic-thread-pool-test
java -jar target/dynamic-thread-pool-test-app.jar --spring.profiles.active=dev
```

**启动前端** (端口 3000):
```bash
cd atluofu-dynamic-thread-pool-ui
npm run dev
```

### 4. 访问管理界面

浏览器访问：**http://localhost:3000**

---

## 📡 API 接口

### Admin 管理后台接口

| 接口 | 方法 | 描述 | 示例 |
|------|------|------|------|
| `/api/v1/dynamic/thread/pool/query_thread_pool_list` | GET | 查询线程池列表 | `curl http://localhost:8089/api/v1/dynamic/thread/pool/query_thread_pool_list` |
| `/api/v1/dynamic/thread/pool/query_thread_pool_config` | GET | 查询线程池配置 | `curl http://localhost:8089/api/v1/dynamic/thread/pool/query_thread_pool_config?appName=xxx&threadPoolName=xxx` |
| `/api/v1/dynamic/thread/pool/update_thread_pool_config` | POST | 更新线程池配置 | `curl -X POST http://localhost:8089/api/v1/dynamic/thread/pool/update_thread_pool_config -H "Content-Type: application/json" -d '{"appName":"xxx","threadPoolName":"xxx","corePoolSize":20,"maximumPoolSize":50}'` |

### 响应格式

```json
{
  "code": "0000",
  "info": "调用成功",
  "data": [...]
}
```

---

## 🧪 测试

### 单元测试

```bash
# SDK 模块测试
cd atluofu-dynamic-thread-pool-spring-boot-starter
mvn test

# Admin 模块测试
cd dynamic-thread-pool-admin
mvn test
```

### 集成测试 & 冒烟测试

```bash
cd atluofu-dynamic-thread-pool-test
mvn test -Dtest=ThreadPoolIntegrationTest,SmokeTest
```

### 测试覆盖

| 模块 | 测试类 | 测试数 | 通过率 |
|------|--------|--------|--------|
| SDK | DynamicThreadPoolServiceTest | 8 | ✅ 100% |
| SDK | ThreadPoolConfigAdjustListenerTest | 4 | ✅ 100% |
| Admin | DynamicThreadPoolControllerTest | 8 | ✅ 100% |
| 集成测试 | ThreadPoolIntegrationTest | 5 | ✅ 100% |
| 冒烟测试 | SmokeTest | 7 | ✅ 100% |
| **合计** | | **32** | **✅ 100%** |

---

## 🔍 核心类说明

### SDK 模块

| 类名 | 作用 |
|------|------|
| `DynamicThreadPoolAutoConfig` | SDK 自动配置类，注册所有必要组件 |
| `DynamicThreadPoolService` | 线程池服务，提供查询和更新方法 |
| `ThreadPoolConfigAdjustListener` | Redis 消息监听器，接收配置变更 |
| `ThreadPoolDataReportJob` | 定时上报任务，每 20 秒上报一次状态 |
| `RedisRegistry` | Redis 注册中心实现，负责数据持久化 |
| `ThreadPoolConfigEntity` | 线程池配置实体类 |

### Admin 模块

| 类名 | 作用 |
|------|------|
| `DynamicThreadPoolController` | REST API 控制器，提供管理接口 |
| `Application` | 启动类，包含 Redis 配置 |

### 前端模块

| 文件 | 作用 |
|------|------|
| `src/views/ThreadPool.vue` | 主页面，展示线程池列表和编辑功能 |
| `src/api/threadPool.js` | API 接口封装 |
| `src/utils/request.js` | Axios 请求封装 |

---

## 📊 线程池状态字段

| 字段 | 说明 | 示例 |
|------|------|------|
| `appName` | 应用名称 | `dynamic-thread-pool-test-app` |
| `threadPoolName` | 线程池名称 | `threadPoolExecutor01` |
| `corePoolSize` | 核心线程数 | `20` |
| `maximumPoolSize` | 最大线程数 | `50` |
| `activeCount` | 活跃线程数 | `5` |
| `poolSize` | 当前池中线程数 | `20` |
| `queueType` | 队列类型 | `LinkedBlockingQueue` |
| `queueSize` | 队列任务数 | `100` |
| `remainingCapacity` | 队列剩余容量 | `4900` |

---

## ⚠️ 注意事项

1. **Redis 密码配置**：确保配置文件中的 Redis 密码正确
2. **Bean 初始化顺序**：使用 `@DependsOn` 确保正确的初始化顺序
3. **配置属性名称**：使用 `enable` 而非 `enabled`
4. **主类配置**：确保 pom.xml 中的主类包名正确
5. **前端代理**：Vite 配置了 API 代理到后端 (8089 端口)

---

## 🛠️ 常见问题

### 1. Redis 连接失败
**错误**：`NOAUTH Authentication required`
**解决**：在配置文件中设置正确的 Redis 密码

### 2. 端口被占用
**错误**：`Port 8089/8093/3000 is already in use`
**解决**：修改对应服务的端口配置或关闭占用端口的进程

### 3. 前端无法连接后端
**解决**：检查 `vite.config.js` 中的代理配置是否正确

### 4. Test 应用启动失败
**错误**：`IllegalArgumentException` in `ThreadPoolExecutor`
**解决**：清理 Redis 数据后重启 `redis-cli -a password FLUSHDB`

---

## 📝 更新日志

### v1.0.0 (2026-03-31)
- ✅ 修复日志字段错误（poolSize → corePoolSize）
- ✅ 修复上报数据使用最新状态
- ✅ 修复空对象返回问题
- ✅ 修复 Bean 方法名重复问题
- ✅ 添加 `@DependsOn` 确保初始化顺序
- ✅ 创建 Vue 3 前端管理界面
- ✅ 添加完整的单元测试和集成测试

---

## 📄 License

MIT

---

## 👥 作者

有罗敷的马同学 / atluofu

---

## 🔗 相关链接

- [Redisson 文档](https://github.com/redisson/redisson)
- [Vue 3 文档](https://vuejs.org/)
- [Element Plus 文档](https://element-plus.org/)
- [Spring Boot 文档](https://spring.io/projects/spring-boot)
