# 动态线程池管理平台

## 项目介绍

动态线程池管理平台基于 Spring Boot、Redis 和 Vue，提供线程池与虚拟线程执行器的注册、监控、动态调整和审计能力。业务应用接入 SDK 后，平台可以按应用、实例和执行器维度查看运行快照，并通过 Redis Topic 下发变更。

核心能力：

- 管理 `ThreadPoolExecutor`、`ThreadPoolTaskExecutor` 和 `BoundedVirtualThreadExecutor`。
- 定时上报执行器快照，包括线程池容量、队列、任务数和虚拟线程并发指标。
- Admin 后台通过 `/api/v1/dtp` 接口查询快照、发布调整命令和查看审计事件。
- 前端基于 Vue 3 展示应用、实例、执行器和审计数据。

## 项目结构

```text
atluofu-dynamic-thread-pool/
├── atluofu-dynamic-thread-pool-spring-boot-starter/  # SDK 自动配置、执行器适配、Redis 注册中心
├── atluofu-dynamic-thread-pool-test/                 # 示例应用 1
├── atluofu-dynamic-thread-pool-test2/                # 示例应用 2
├── dynamic-thread-pool-admin/                        # Admin REST API
├── atluofu-dynamic-thread-pool-ui/                   # Vue 管理界面
└── docs/                                             # 升级需求和说明文档
```

## 前置条件

- JDK 21
- Maven 3.9+
- Redis
- Node.js

## SDK 配置

业务应用通过 `atluofu.dynamic.thread-pool` 前缀配置 SDK。示例：

```yaml
atluofu:
  dynamic:
    thread-pool:
      enabled: true
      app-name: ${spring.application.name}
      instance-id: ${spring.application.name}-${server.port}
      registry:
        type: redis
        redis:
          host: 127.0.0.1
          port: 6379
          password: YourRedisPassword
          database: 0
      report:
        enabled: true
        interval: 20s
      trace:
        enabled: true
        mdc-enabled: true
        trace-id-key: traceId
        request-id-key: requestId
      virtual:
        enabled: true
        default-concurrency-limit: 500
```

## 业务执行器声明

普通线程池继续以 Spring Bean 方式声明，SDK 会自动托管支持的执行器类型。虚拟线程执行器由业务应用显式声明 `BoundedVirtualThreadExecutor` Bean：

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.atluofu.middleware.dynamic.thread.pool.sdk.executor.virtual.BoundedVirtualThreadExecutor;

@Configuration
public class ThreadPoolConfig {

    @Bean("virtualTaskExecutor")
    public BoundedVirtualThreadExecutor virtualTaskExecutor() {
        return new BoundedVirtualThreadExecutor("sample-virtual", 100);
    }
}
```

该 Bean 会以 `ExecutorKind.VIRTUAL_THREAD_PER_TASK` 注册，Admin 通过虚拟线程并发上限接口调整 `concurrencyLimit`。

## Redis 数据模型

当前 Redis key 由 `DtpRedisKeys` 统一生成：

| Key | 说明 |
|-----|------|
| `DTP:APPS` | 已注册应用集合 |
| `DTP:APP:{appName}:INSTANCES` | 应用实例集合 |
| `DTP:SNAPSHOT:{appName}:{instanceId}:{executorName}` | 单个执行器快照 |
| `DTP:CHANGE_TOPIC:{appName}` | 应用配置变更 Topic |
| `DTP:EVENT:{appName}:{yyyyMMdd}` | 应用按日审计事件列表 |

## Admin API

Admin 控制器统一挂载在 `/api/v1/dtp`。响应保持项目通用包装：

```json
{
  "code": "0000",
  "info": "调用成功",
  "data": {}
}
```

接口列表：

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/v1/dtp/apps` | GET | 查询应用集合 |
| `/api/v1/dtp/apps/{appName}/instances` | GET | 查询应用实例集合 |
| `/api/v1/dtp/apps/{appName}/instances/{instanceId}/executors` | GET | 查询实例执行器快照列表 |
| `/api/v1/dtp/apps/{appName}/instances/{instanceId}/executors/{executorName}` | GET | 查询单个执行器快照 |
| `/api/v1/dtp/apps/{appName}/instances/{instanceId}/executors/{executorName}/resize` | POST | 调整平台线程池容量 |
| `/api/v1/dtp/apps/{appName}/instances/{instanceId}/executors/{executorName}/virtual-limit` | POST | 调整虚拟线程执行器并发上限 |
| `/api/v1/dtp/events?appName={appName}&date={yyyyMMdd}` | GET | 查询审计事件 |

平台线程池调整示例：

```bash
curl -X POST "http://localhost:8089/api/v1/dtp/apps/dynamic-thread-pool-test-app/instances/dynamic-thread-pool-test-app-8093/executors/threadPoolExecutor01/resize" \
  -H "Content-Type: application/json" \
  -d '{"corePoolSize":20,"maximumPoolSize":50,"keepAliveSeconds":60,"allowCoreThreadTimeOut":false,"operator":"admin"}'
```

虚拟线程并发上限调整示例：

```bash
curl -X POST "http://localhost:8089/api/v1/dtp/apps/dynamic-thread-pool-test-app/instances/dynamic-thread-pool-test-app-8093/executors/virtualTaskExecutor/virtual-limit" \
  -H "Content-Type: application/json" \
  -d '{"concurrencyLimit":100,"operator":"admin"}'
```

## 本地构建

安装后端依赖并打包：

```bash
mvn clean package
```

构建前端：

```bash
npm --prefix atluofu-dynamic-thread-pool-ui install
npm --prefix atluofu-dynamic-thread-pool-ui run build
```

## 验证命令

升级计划要求的完整验证命令：

```bash
mvn clean test
mvn clean package
npm --prefix atluofu-dynamic-thread-pool-ui run build
```

说明：示例应用依赖 Redis。运行 SpringBootTest 或手动启动示例应用前，请确保 `application-dev.yml` 中的 Redis 地址、端口、密码和库号可用。

## 运行入口

打包完成后可分别启动 Admin、示例应用和前端开发服务。不要在验证命令中自动启动应用。

Admin：

```bash
java -jar dynamic-thread-pool-admin/target/dynamic-thread-pool-admin-app.jar --spring.profiles.active=dev
```

示例应用：

```bash
java -jar atluofu-dynamic-thread-pool-test/target/dynamic-thread-pool-test-app.jar --spring.profiles.active=dev
```

前端开发服务：

```bash
npm --prefix atluofu-dynamic-thread-pool-ui run dev
```

## 核心类

| 类名 | 作用 |
|------|------|
| `DynamicThreadPoolAutoConfig` | SDK 自动配置入口，注册 Redisson、执行器注册表、服务、监听器和上报任务 |
| `DynamicThreadPoolAutoProperties` | `atluofu.dynamic.thread-pool` 配置属性 |
| `DynamicThreadPoolService` | 查询 `ExecutorSnapshot`，执行 `ExecutorUpdateCommand` |
| `ManagedExecutorRegistry` | 托管执行器注册表 |
| `ThreadPoolExecutorManagedExecutor` | `ThreadPoolExecutor` 适配器 |
| `ThreadPoolTaskExecutorManagedExecutor` | Spring `ThreadPoolTaskExecutor` 适配器 |
| `BoundedVirtualThreadManagedExecutor` | `BoundedVirtualThreadExecutor` 适配器 |
| `RedisRegistry` | Redis 注册中心，负责快照、变更消息和审计事件 |
| `DynamicThreadPoolController` | Admin REST API |

## 执行器快照字段

| 字段 | 说明 |
|------|------|
| `appName` | 应用名称 |
| `instanceId` | 应用实例 ID |
| `executorName` | Spring Bean 名称 |
| `executorKind` | 执行器类型 |
| `virtual` | 是否虚拟线程执行器 |
| `resizable` | 是否支持容量调整 |
| `corePoolSize` / `maximumPoolSize` | 平台线程池容量 |
| `activeCount` / `poolSize` | 平台线程池运行线程数 |
| `queueType` / `queueSize` / `remainingCapacity` | 队列信息 |
| `concurrencyLimit` / `availablePermits` | 虚拟线程并发限制 |
| `runningTasks` / `submittedTasks` / `completedTaskCount` / `failedTasks` / `rejectedTasks` | 任务统计 |
| `reportTime` | 快照生成时间 |

## 常见问题

### Redis 连接失败

检查业务应用和 Admin 的 Redis 配置是否一致，尤其是密码和 database。

### 查询不到执行器

确认业务应用已启动并接入 SDK，`atluofu.dynamic.thread-pool.enabled` 为 `true`，并且上报任务已写入 `DTP:SNAPSHOT:{appName}:{instanceId}:{executorName}`。

### 虚拟线程执行器不能 resize

虚拟线程执行器不使用 `corePoolSize` 和 `maximumPoolSize`。请使用 `/virtual-limit` 接口调整 `concurrencyLimit`。
