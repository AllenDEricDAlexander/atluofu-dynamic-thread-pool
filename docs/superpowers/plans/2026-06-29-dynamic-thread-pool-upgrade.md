# Dynamic Thread Pool Upgrade Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade the existing dynamic thread pool project to JDK 21 and Spring Boot 3.5 with managed executor governance, trace propagation, Redis-isolated snapshots, Redis-backed audit events, Micrometer metrics, and minimal UI support.

**Architecture:** Keep the current modules and package family. Add focused packages inside the existing starter for properties, executor adapters, context propagation, Redis contracts, audit, and metrics. Admin switches directly to new REST endpoints backed by Redis keys, while the UI changes only enough to call the new endpoints and display platform versus virtual executor fields.

**Tech Stack:** Java 21, Spring Boot 3.5.x, Maven, Redisson, Micrometer, JUnit Jupiter, Mockito, Vue 3, Element Plus, Vite.

---

## Scope Check

The approved design intentionally keeps starter, Admin, sample apps, Redis contracts, metrics, and UI in one implementation plan because they form one end-to-end product slice. This plan still splits delivery into independently committable tasks so each task leaves the repo in a testable state.

Do not create new Maven modules. Do not keep compatibility for the old `dynamic.thread.pool.config` prefix. Do not keep old Admin underscore APIs. Do not start any application server automatically during implementation.

## File Structure Map

Starter module:

- Modify `pom.xml`, `atluofu-dynamic-thread-pool-spring-boot-starter/pom.xml`, `dynamic-thread-pool-admin/pom.xml`, `atluofu-dynamic-thread-pool-test/pom.xml`, `atluofu-dynamic-thread-pool-test2/pom.xml`: JDK 21, Spring Boot 3.5, JUnit Jupiter, Micrometer dependencies.
- Replace `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/resources/META-INF/spring.factories` with `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
- Modify `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/config/DynamicThreadPoolAutoConfig.java`: Boot 3.5 auto-configuration and bean wiring.
- Modify `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/config/DynamicThreadPoolAutoProperties.java`: new `atluofu.dynamic.thread-pool` property tree.
- Create `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/domain/model/valobj/ExecutorKind.java`: executor type enum.
- Create `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/domain/model/entity/ExecutorSnapshot.java`: unified snapshot model.
- Create `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/domain/model/entity/ExecutorUpdateCommand.java`: update command model.
- Create `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/domain/model/entity/UpdateResult.java`: update result model.
- Create `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/executor/ManagedExecutor.java`: shared executor contract.
- Create `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/executor/ManagedExecutorRegistry.java`: local executor registry.
- Create `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/executor/support/ThreadPoolResizeSupport.java`: safe resize helper.
- Create executor adapters under `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/executor/adapter/`.
- Create context helpers under `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/context/`.
- Create Redis contracts under `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/registry/model/`.
- Modify `RedisRegistry.java`, `IRegistry.java`, `ThreadPoolDataReportJob.java`, and `ThreadPoolConfigAdjustListener.java` for snapshots, events, envelopes, and trace restore.
- Create `DtpMeterBinder.java` under `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/metrics/`.

Admin module:

- Modify `dynamic-thread-pool-admin/src/main/java/top/atluofu/middleware/dynamic/thread/pool/types/Response.java`: include trace id.
- Replace `dynamic-thread-pool-admin/src/main/java/top/atluofu/middleware/dynamic/thread/pool/trigger/DynamicThreadPoolController.java` with new REST routes.
- Create `dynamic-thread-pool-admin/src/main/java/top/atluofu/middleware/dynamic/thread/pool/config/DtpTraceIdFilter.java`.
- Create Admin request DTOs under `dynamic-thread-pool-admin/src/main/java/top/atluofu/middleware/dynamic/thread/pool/trigger/model/`.

UI module:

- Modify `atluofu-dynamic-thread-pool-ui/src/api/threadPool.js`: new REST calls.
- Modify `atluofu-dynamic-thread-pool-ui/src/views/ThreadPool.vue`: minimal display/edit changes.

Sample apps and docs:

- Modify `atluofu-dynamic-thread-pool-test/src/main/resources/application-dev.yml`.
- Modify `atluofu-dynamic-thread-pool-test2/src/main/resources/application-dev.yml`.
- Modify sample `ThreadPoolConfig.java` files to optionally expose a virtual executor bean.
- Modify `readme.md` after implementation behavior is verified.

## Commit Boundary Rules

Use one commit per task. Before every commit, run `git status --short` and stage only files listed in that task. There are existing unrelated staged changes in this repository; do not commit them with implementation tasks.

Use path-limited commits:

```bash
git add <task-specific-files>
git commit --only <task-specific-files> -m "<message>"
```

## Task 1: Platform Upgrade And Boot 3 Auto-Configuration

**Files:**
- Modify: `pom.xml`
- Modify: `atluofu-dynamic-thread-pool-spring-boot-starter/pom.xml`
- Modify: `dynamic-thread-pool-admin/pom.xml`
- Modify: `atluofu-dynamic-thread-pool-test/pom.xml`
- Modify: `atluofu-dynamic-thread-pool-test2/pom.xml`
- Modify: `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/config/DynamicThreadPoolAutoConfig.java`
- Delete: `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/resources/META-INF/spring.factories`
- Create: `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Modify tests: all starter/admin/sample tests importing `org.junit.Test` or `org.junit.runner.RunWith`

- [ ] **Step 1: Update Maven platform versions**

In root `pom.xml`, set Spring Boot to 3.5.x and Java 21. Use one Java property and release-based compiler config:

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.16</version>
</parent>

<properties>
    <java.version>21</java.version>
    <maven.compiler.release>21</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>
```

In the root `maven-compiler-plugin`, replace source/target config with:

```xml
<configuration>
    <release>${maven.compiler.release}</release>
    <encoding>${project.build.sourceEncoding}</encoding>
</configuration>
```

Remove `-XX:MaxPermSize`, `-XX:+UseFastAccessorMethods`, `-Xloggc`, `-XX:+PrintGCDetails`, and `-XX:+PrintGCDateStamps` from all `java_jvm` profile values because they are invalid or obsolete on JDK 21.

- [ ] **Step 2: Remove JUnit4 and Vintage dependencies**

Remove these dependencies from all module POMs:

```xml
<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
</dependency>
<dependency>
    <groupId>org.junit.vintage</groupId>
    <artifactId>junit-vintage-engine</artifactId>
</dependency>
```

Keep or add:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 3: Convert one representative test to JUnit Jupiter and repeat the same import pattern in all tests**

Replace:

```java
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;
```

With:

```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringExtension;
```

For Spring tests, replace:

```java
@RunWith(SpringRunner.class)
@SpringBootTest
```

With:

```java
@SpringBootTest
```

For Mockito controller tests, replace:

```java
@RunWith(MockitoJUnitRunner.class)
public class DynamicThreadPoolControllerTest {

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }
}
```

With:

```java
@ExtendWith(MockitoExtension.class)
class DynamicThreadPoolControllerTest {
}
```

Use these imports:

```java
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
```

- [ ] **Step 4: Migrate auto-configuration registration**

Delete `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/resources/META-INF/spring.factories`.

Create `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` with exactly:

```text
top.atluofu.middleware.dynamic.thread.pool.sdk.config.DynamicThreadPoolAutoConfig
```

In `DynamicThreadPoolAutoConfig.java`, replace `@Configuration` with:

```java
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@AutoConfiguration
@EnableConfigurationProperties(DynamicThreadPoolAutoProperties.class)
@ConditionalOnProperty(prefix = "atluofu.dynamic.thread-pool", name = "enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class DynamicThreadPoolAutoConfig {
}
```

Keep existing bean methods compiling for this task. Do not change Redis keys, executor models, or Admin API behavior in Task 1.

- [ ] **Step 5: Run targeted Maven compile**

Run:

```bash
mvn -pl atluofu-dynamic-thread-pool-spring-boot-starter -am test -DskipTests=false
```

Expected: PASS. If the command fails, fix only JUnit imports, Maven dependency, Java 21 compiler, or Spring Boot auto-configuration registration failures in this task.

- [ ] **Step 6: Commit Task 1**

```bash
git status --short
git add pom.xml atluofu-dynamic-thread-pool-spring-boot-starter/pom.xml dynamic-thread-pool-admin/pom.xml atluofu-dynamic-thread-pool-test/pom.xml atluofu-dynamic-thread-pool-test2/pom.xml atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/config/DynamicThreadPoolAutoConfig.java atluofu-dynamic-thread-pool-spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
git add -u atluofu-dynamic-thread-pool-spring-boot-starter/src/main/resources/META-INF/spring.factories atluofu-dynamic-thread-pool-spring-boot-starter/src/test dynamic-thread-pool-admin/src/test atluofu-dynamic-thread-pool-test/src/test
git commit --only pom.xml atluofu-dynamic-thread-pool-spring-boot-starter/pom.xml dynamic-thread-pool-admin/pom.xml atluofu-dynamic-thread-pool-test/pom.xml atluofu-dynamic-thread-pool-test2/pom.xml atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/config/DynamicThreadPoolAutoConfig.java atluofu-dynamic-thread-pool-spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports atluofu-dynamic-thread-pool-spring-boot-starter/src/main/resources/META-INF/spring.factories atluofu-dynamic-thread-pool-spring-boot-starter/src/test dynamic-thread-pool-admin/src/test atluofu-dynamic-thread-pool-test/src/test -m "build: upgrade to java 21 and boot 3 auto config"
```

## Task 2: New Configuration Prefix And Sample Application Properties

**Files:**
- Modify: `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/config/DynamicThreadPoolAutoProperties.java`
- Modify: `atluofu-dynamic-thread-pool-test/src/main/resources/application-dev.yml`
- Modify: `atluofu-dynamic-thread-pool-test2/src/main/resources/application-dev.yml`
- Modify: `dynamic-thread-pool-admin/src/main/resources/application-dev.yml`
- Test: `atluofu-dynamic-thread-pool-spring-boot-starter/src/test/java/top/atluofu/middleware/dynamic/thread/pool/sdk/config/DynamicThreadPoolAutoPropertiesTest.java`

- [ ] **Step 1: Replace the properties test with new-prefix binding coverage**

Set `DynamicThreadPoolAutoPropertiesTest.java` to use `ApplicationContextRunner`:

```java
package top.atluofu.middleware.dynamic.thread.pool.sdk.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicThreadPoolAutoPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DynamicThreadPoolAutoConfig.class))
            .withPropertyValues(
                    "spring.application.name=test-app",
                    "server.port=8093",
                    "atluofu.dynamic.thread-pool.enabled=true",
                    "atluofu.dynamic.thread-pool.registry.redis.host=127.0.0.1",
                    "atluofu.dynamic.thread-pool.registry.redis.port=6379",
                    "atluofu.dynamic.thread-pool.registry.redis.password=pwd",
                    "atluofu.dynamic.thread-pool.report.interval=20s",
                    "atluofu.dynamic.thread-pool.trace.trace-id-key=traceId",
                    "atluofu.dynamic.thread-pool.trace.request-id-key=requestId",
                    "atluofu.dynamic.thread-pool.virtual.default-concurrency-limit=500"
            );

    @Test
    void shouldBindNewPrefix() {
        contextRunner.run(context -> {
            DynamicThreadPoolAutoProperties properties = context.getBean(DynamicThreadPoolAutoProperties.class);

            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getRegistry().getRedis().getHost()).isEqualTo("127.0.0.1");
            assertThat(properties.getRegistry().getRedis().getPort()).isEqualTo(6379);
            assertThat(properties.getRegistry().getRedis().getPassword()).isEqualTo("pwd");
            assertThat(properties.getReport().getInterval()).hasSeconds(20);
            assertThat(properties.getTrace().getTraceIdKey()).isEqualTo("traceId");
            assertThat(properties.getTrace().getRequestIdKey()).isEqualTo("requestId");
            assertThat(properties.getVirtual().getDefaultConcurrencyLimit()).isEqualTo(500);
        });
    }
}
```

- [ ] **Step 2: Implement the new property model**

Replace `DynamicThreadPoolAutoProperties.java` with nested classes using the new prefix:

```java
package top.atluofu.middleware.dynamic.thread.pool.sdk.config;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "atluofu.dynamic.thread-pool", ignoreInvalidFields = true)
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class DynamicThreadPoolAutoProperties {

    private boolean enabled = true;

    private String appName;

    private String instanceId;

    private Registry registry = new Registry();

    private Report report = new Report();

    private Trace trace = new Trace();

    private Virtual virtual = new Virtual();

    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    public static class Registry {

        private String type = "redis";

        private Redis redis = new Redis();
    }

    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    public static class Redis {

        private String host = "127.0.0.1";

        private int port = 6379;

        private String password;

        private int database = 0;

        private int poolSize = 64;

        private int minIdleSize = 10;

        private int idleTimeout = 10000;

        private int connectTimeout = 10000;

        private int retryAttempts = 3;

        private int retryInterval = 1000;

        private int pingInterval = 0;

        private boolean keepAlive = true;
    }

    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    public static class Report {

        private boolean enabled = true;

        private Duration interval = Duration.ofSeconds(20);
    }

    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    public static class Trace {

        private boolean enabled = true;

        private boolean mdcEnabled = true;

        private String traceIdKey = "traceId";

        private String requestIdKey = "requestId";
    }

    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    public static class Virtual {

        private boolean enabled = true;

        private int defaultConcurrencyLimit = 500;
    }
}
```

- [ ] **Step 3: Update Redisson property access in auto-configuration**

In `DynamicThreadPoolAutoConfig.redissonClient`, replace direct `properties.getHost()` style calls with:

```java
DynamicThreadPoolAutoProperties.Redis redis = properties.getRegistry().getRedis();
config.useSingleServer()
        .setAddress("redis://" + redis.getHost() + ":" + redis.getPort())
        .setPassword(redis.getPassword())
        .setDatabase(redis.getDatabase())
        .setConnectionPoolSize(redis.getPoolSize())
        .setConnectionMinimumIdleSize(redis.getMinIdleSize())
        .setIdleConnectionTimeout(redis.getIdleTimeout())
        .setConnectTimeout(redis.getConnectTimeout())
        .setRetryAttempts(redis.getRetryAttempts())
        .setRetryInterval(redis.getRetryInterval())
        .setPingConnectionInterval(redis.getPingInterval())
        .setKeepAlive(redis.isKeepAlive());
```

Log:

```java
log.info("动态线程池，注册器（redis）链接初始化完成。{} {} {}", redis.getHost(), redis.getPoolSize(), !redissonClient.isShutdown());
```

- [ ] **Step 4: Update sample application YAML**

In both sample `application-dev.yml` files, replace the old dynamic thread pool block with:

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
          password: HomeLab666
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

Update Admin `application-dev.yml` to use the same Redis block under `atluofu.dynamic.thread-pool.registry.redis`.

- [ ] **Step 5: Run the properties test**

Run:

```bash
mvn -pl atluofu-dynamic-thread-pool-spring-boot-starter -Dtest=DynamicThreadPoolAutoPropertiesTest test
```

Expected: PASS.

- [ ] **Step 6: Commit Task 2**

```bash
git status --short
git add atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/config/DynamicThreadPoolAutoProperties.java atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/config/DynamicThreadPoolAutoConfig.java atluofu-dynamic-thread-pool-spring-boot-starter/src/test/java/top/atluofu/middleware/dynamic/thread/pool/sdk/config/DynamicThreadPoolAutoPropertiesTest.java atluofu-dynamic-thread-pool-test/src/main/resources/application-dev.yml atluofu-dynamic-thread-pool-test2/src/main/resources/application-dev.yml dynamic-thread-pool-admin/src/main/resources/application-dev.yml
git commit --only atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/config/DynamicThreadPoolAutoProperties.java atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/config/DynamicThreadPoolAutoConfig.java atluofu-dynamic-thread-pool-spring-boot-starter/src/test/java/top/atluofu/middleware/dynamic/thread/pool/sdk/config/DynamicThreadPoolAutoPropertiesTest.java atluofu-dynamic-thread-pool-test/src/main/resources/application-dev.yml atluofu-dynamic-thread-pool-test2/src/main/resources/application-dev.yml dynamic-thread-pool-admin/src/main/resources/application-dev.yml -m "feat: bind dynamic thread pool boot properties"
```

## Task 3: Managed Executor Model, Registry, And Safe Resize

**Files:**
- Create: `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/domain/model/valobj/ExecutorKind.java`
- Create: `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/domain/model/entity/ExecutorSnapshot.java`
- Create: `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/domain/model/entity/ExecutorUpdateCommand.java`
- Create: `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/domain/model/entity/UpdateResult.java`
- Create: `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/executor/ManagedExecutor.java`
- Create: `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/executor/ManagedExecutorRegistry.java`
- Create: `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/executor/support/ThreadPoolResizeSupport.java`
- Create: `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/executor/adapter/ThreadPoolExecutorManagedExecutor.java`
- Test: `atluofu-dynamic-thread-pool-spring-boot-starter/src/test/java/top/atluofu/middleware/dynamic/thread/pool/sdk/executor/support/ThreadPoolResizeSupportTest.java`
- Test: `atluofu-dynamic-thread-pool-spring-boot-starter/src/test/java/top/atluofu/middleware/dynamic/thread/pool/sdk/executor/adapter/ThreadPoolExecutorManagedExecutorTest.java`

- [ ] **Step 1: Write failing resize tests**

Create `ThreadPoolResizeSupportTest.java`:

```java
package top.atluofu.middleware.dynamic.thread.pool.sdk.executor.support;

import org.junit.jupiter.api.Test;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ThreadPoolResizeSupportTest {

    @Test
    void shouldResizeWhenNewMaximumIsLowerThanCurrentCore() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 20, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(100));

        ThreadPoolResizeSupport.resize(executor, 4, 8);

        assertThat(executor.getCorePoolSize()).isEqualTo(4);
        assertThat(executor.getMaximumPoolSize()).isEqualTo(8);
    }

    @Test
    void shouldResizeWhenNewCoreIsHigherThanCurrentMaximum() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 4, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(100));

        ThreadPoolResizeSupport.resize(executor, 6, 8);

        assertThat(executor.getCorePoolSize()).isEqualTo(6);
        assertThat(executor.getMaximumPoolSize()).isEqualTo(8);
    }

    @Test
    void shouldRejectInvalidSizes() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 4, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(100));

        assertThatThrownBy(() -> ThreadPoolResizeSupport.resize(executor, 0, 4))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("corePoolSize and maximumPoolSize must be positive");
        assertThatThrownBy(() -> ThreadPoolResizeSupport.resize(executor, 5, 4))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("corePoolSize must <= maximumPoolSize");
    }
}
```

- [ ] **Step 2: Write failing managed executor adapter tests**

Create `ThreadPoolExecutorManagedExecutorTest.java`:

```java
package top.atluofu.middleware.dynamic.thread.pool.sdk.executor.adapter;

import org.junit.jupiter.api.Test;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ExecutorSnapshot;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ExecutorUpdateCommand;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.UpdateResult;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.valobj.ExecutorKind;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ThreadPoolExecutorManagedExecutorTest {

    @Test
    void shouldCreateSnapshotFromThreadPoolExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 8, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>(100));
        ThreadPoolExecutorManagedExecutor managed = new ThreadPoolExecutorManagedExecutor("app", "instance", "orderExecutor", executor);

        ExecutorSnapshot snapshot = managed.snapshot();

        assertThat(snapshot.getAppName()).isEqualTo("app");
        assertThat(snapshot.getInstanceId()).isEqualTo("instance");
        assertThat(snapshot.getExecutorName()).isEqualTo("orderExecutor");
        assertThat(snapshot.getExecutorKind()).isEqualTo(ExecutorKind.PLATFORM_THREAD_POOL);
        assertThat(snapshot.getCorePoolSize()).isEqualTo(2);
        assertThat(snapshot.getMaximumPoolSize()).isEqualTo(8);
        assertThat(snapshot.getQueueType()).isEqualTo("LinkedBlockingQueue");
        assertThat(snapshot.isResizable()).isTrue();
        assertThat(snapshot.isVirtual()).isFalse();
    }

    @Test
    void shouldUpdateThreadPoolExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 8, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>(100));
        ThreadPoolExecutorManagedExecutor managed = new ThreadPoolExecutorManagedExecutor("app", "instance", "orderExecutor", executor);
        ExecutorUpdateCommand command = new ExecutorUpdateCommand();
        command.setCorePoolSize(4);
        command.setMaximumPoolSize(12);
        command.setKeepAliveSeconds(45L);
        command.setAllowCoreThreadTimeOut(true);

        UpdateResult result = managed.update(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(executor.getCorePoolSize()).isEqualTo(4);
        assertThat(executor.getMaximumPoolSize()).isEqualTo(12);
        assertThat(executor.getKeepAliveTime(TimeUnit.SECONDS)).isEqualTo(45);
        assertThat(executor.allowsCoreThreadTimeOut()).isTrue();
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run:

```bash
mvn -pl atluofu-dynamic-thread-pool-spring-boot-starter -Dtest=ThreadPoolResizeSupportTest,ThreadPoolExecutorManagedExecutorTest test
```

Expected: FAIL because new classes do not exist.

- [ ] **Step 4: Add model classes and interface**

Create `ExecutorKind.java`:

```java
package top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.valobj;

public enum ExecutorKind {

    PLATFORM_THREAD_POOL,
    SPRING_THREAD_POOL_TASK_EXECUTOR,
    VIRTUAL_THREAD_PER_TASK,
    UNKNOWN
}
```

Create `ManagedExecutor.java`:

```java
package top.atluofu.middleware.dynamic.thread.pool.sdk.executor;

import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ExecutorSnapshot;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ExecutorUpdateCommand;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.UpdateResult;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.valobj.ExecutorKind;

public interface ManagedExecutor {

    String appName();

    String instanceId();

    String executorName();

    ExecutorKind kind();

    ExecutorSnapshot snapshot();

    UpdateResult update(ExecutorUpdateCommand command);

    boolean supportsResize();

    boolean supportsVirtualThread();

    boolean supportsQueueMetrics();
}
```

Create `ExecutorSnapshot.java`:

```java
package top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.valobj.ExecutorKind;

import java.time.Instant;

@Getter
@Setter
@ToString
public class ExecutorSnapshot {

    private String appName;
    private String instanceId;
    private String executorName;
    private ExecutorKind executorKind;
    private boolean virtual;
    private boolean resizable;
    private Integer corePoolSize;
    private Integer maximumPoolSize;
    private Integer activeCount;
    private Integer poolSize;
    private Long taskCount;
    private Long completedTaskCount;
    private String queueType;
    private Integer queueSize;
    private Integer remainingCapacity;
    private Integer concurrencyLimit;
    private Long runningTasks;
    private Integer availablePermits;
    private Long submittedTasks;
    private Long failedTasks;
    private Long rejectedTasks;
    private Instant reportTime;
}
```

Create `ExecutorUpdateCommand.java`:

```java
package top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.valobj.ExecutorKind;

@Getter
@Setter
@ToString
public class ExecutorUpdateCommand {

    private String appName;
    private String instanceId;
    private String executorName;
    private ExecutorKind executorKind;
    private Integer corePoolSize;
    private Integer maximumPoolSize;
    private Long keepAliveSeconds;
    private Boolean allowCoreThreadTimeOut;
    private Integer concurrencyLimit;
    private String traceId;
    private String requestId;
    private String operator;
    private Long version;
}
```

Create `UpdateResult.java`:

```java
package top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class UpdateResult {

    private boolean success;
    private String message;
    private ExecutorSnapshot before;
    private ExecutorSnapshot after;
}
```

The required field names are:

```text
appName, instanceId, executorName, executorKind, virtual, resizable,
corePoolSize, maximumPoolSize, activeCount, poolSize, taskCount,
completedTaskCount, queueType, queueSize, remainingCapacity,
concurrencyLimit, runningTasks, availablePermits, submittedTasks,
failedTasks, rejectedTasks, reportTime
```

For `UpdateResult`, fields are:

```text
success, message, before, after
```

For `ExecutorUpdateCommand`, fields are:

```text
appName, instanceId, executorName, executorKind, corePoolSize,
maximumPoolSize, keepAliveSeconds, allowCoreThreadTimeOut,
concurrencyLimit, traceId, requestId, operator, version
```

- [ ] **Step 5: Add registry and resize support**

Create `ManagedExecutorRegistry.java`:

```java
package top.atluofu.middleware.dynamic.thread.pool.sdk.executor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class ManagedExecutorRegistry {

    private final Map<String, ManagedExecutor> executors = new LinkedHashMap<>();

    public ManagedExecutorRegistry(Collection<ManagedExecutor> managedExecutors) {
        for (ManagedExecutor managedExecutor : managedExecutors) {
            executors.put(managedExecutor.executorName(), managedExecutor);
        }
    }

    public Optional<ManagedExecutor> get(String executorName) {
        return Optional.ofNullable(executors.get(executorName));
    }

    public Collection<ManagedExecutor> list() {
        return Collections.unmodifiableList(new ArrayList<>(executors.values()));
    }
}
```

Create `ThreadPoolResizeSupport.java`:

```java
package top.atluofu.middleware.dynamic.thread.pool.sdk.executor.support;

import java.util.concurrent.ThreadPoolExecutor;

public final class ThreadPoolResizeSupport {

    private ThreadPoolResizeSupport() {
    }

    public static void resize(ThreadPoolExecutor executor, int newCore, int newMax) {
        if (newCore <= 0 || newMax <= 0) {
            throw new IllegalArgumentException("corePoolSize and maximumPoolSize must be positive");
        }
        if (newCore > newMax) {
            throw new IllegalArgumentException("corePoolSize must <= maximumPoolSize");
        }

        int currentCore = executor.getCorePoolSize();
        int currentMax = executor.getMaximumPoolSize();

        if (newMax < currentCore) {
            executor.setCorePoolSize(newCore);
            executor.setMaximumPoolSize(newMax);
            return;
        }
        if (newCore > currentMax) {
            executor.setMaximumPoolSize(newMax);
            executor.setCorePoolSize(newCore);
            return;
        }

        executor.setMaximumPoolSize(newMax);
        executor.setCorePoolSize(newCore);
    }
}
```

- [ ] **Step 6: Add the platform thread pool adapter**

Create `ThreadPoolExecutorManagedExecutor.java`:

```java
package top.atluofu.middleware.dynamic.thread.pool.sdk.executor.adapter;

import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ExecutorSnapshot;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ExecutorUpdateCommand;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.UpdateResult;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.valobj.ExecutorKind;
import top.atluofu.middleware.dynamic.thread.pool.sdk.executor.ManagedExecutor;
import top.atluofu.middleware.dynamic.thread.pool.sdk.executor.support.ThreadPoolResizeSupport;

import java.time.Instant;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolExecutorManagedExecutor implements ManagedExecutor {

    private final String appName;
    private final String instanceId;
    private final String executorName;
    private final ThreadPoolExecutor executor;

    public ThreadPoolExecutorManagedExecutor(String appName, String instanceId, String executorName, ThreadPoolExecutor executor) {
        this.appName = appName;
        this.instanceId = instanceId;
        this.executorName = executorName;
        this.executor = executor;
    }

    @Override
    public String appName() {
        return appName;
    }

    @Override
    public String instanceId() {
        return instanceId;
    }

    @Override
    public String executorName() {
        return executorName;
    }

    @Override
    public ExecutorKind kind() {
        return ExecutorKind.PLATFORM_THREAD_POOL;
    }

    @Override
    public ExecutorSnapshot snapshot() {
        ExecutorSnapshot snapshot = new ExecutorSnapshot();
        snapshot.setAppName(appName);
        snapshot.setInstanceId(instanceId);
        snapshot.setExecutorName(executorName);
        snapshot.setExecutorKind(kind());
        snapshot.setVirtual(false);
        snapshot.setResizable(true);
        snapshot.setCorePoolSize(executor.getCorePoolSize());
        snapshot.setMaximumPoolSize(executor.getMaximumPoolSize());
        snapshot.setActiveCount(executor.getActiveCount());
        snapshot.setPoolSize(executor.getPoolSize());
        snapshot.setTaskCount(executor.getTaskCount());
        snapshot.setCompletedTaskCount(executor.getCompletedTaskCount());
        snapshot.setQueueType(executor.getQueue().getClass().getSimpleName());
        snapshot.setQueueSize(executor.getQueue().size());
        snapshot.setRemainingCapacity(executor.getQueue().remainingCapacity());
        snapshot.setReportTime(Instant.now());
        return snapshot;
    }

    @Override
    public UpdateResult update(ExecutorUpdateCommand command) {
        ExecutorSnapshot before = snapshot();
        UpdateResult result = new UpdateResult();
        result.setBefore(before);
        try {
            if (command.getCorePoolSize() != null && command.getMaximumPoolSize() != null) {
                ThreadPoolResizeSupport.resize(executor, command.getCorePoolSize(), command.getMaximumPoolSize());
            }
            if (command.getKeepAliveSeconds() != null) {
                executor.setKeepAliveTime(command.getKeepAliveSeconds(), TimeUnit.SECONDS);
            }
            if (command.getAllowCoreThreadTimeOut() != null) {
                executor.allowCoreThreadTimeOut(command.getAllowCoreThreadTimeOut());
            }
            result.setSuccess(true);
            result.setMessage("success");
        } catch (RuntimeException ex) {
            result.setSuccess(false);
            result.setMessage(ex.getMessage());
        }
        result.setAfter(snapshot());
        return result;
    }

    @Override
    public boolean supportsResize() {
        return true;
    }

    @Override
    public boolean supportsVirtualThread() {
        return false;
    }

    @Override
    public boolean supportsQueueMetrics() {
        return true;
    }
}
```

- [ ] **Step 7: Run tests**

Run:

```bash
mvn -pl atluofu-dynamic-thread-pool-spring-boot-starter -Dtest=ThreadPoolResizeSupportTest,ThreadPoolExecutorManagedExecutorTest test
```

Expected: PASS.

- [ ] **Step 8: Commit Task 3**

```bash
git status --short
git add atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/domain/model/valobj/ExecutorKind.java atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/domain/model/entity/ExecutorSnapshot.java atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/domain/model/entity/ExecutorUpdateCommand.java atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/domain/model/entity/UpdateResult.java atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/executor atluofu-dynamic-thread-pool-spring-boot-starter/src/test/java/top/atluofu/middleware/dynamic/thread/pool/sdk/executor
git commit --only atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/domain/model/valobj/ExecutorKind.java atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/domain/model/entity/ExecutorSnapshot.java atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/domain/model/entity/ExecutorUpdateCommand.java atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/domain/model/entity/UpdateResult.java atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/executor atluofu-dynamic-thread-pool-spring-boot-starter/src/test/java/top/atluofu/middleware/dynamic/thread/pool/sdk/executor -m "feat: add managed executor model"
```

## Task 4: Context Propagation Utilities

**Files:**
- Create: `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/context/DtpContextSnapshot.java`
- Create: `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/context/DtpRunnable.java`
- Create: `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/context/DtpCallable.java`
- Create: `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/context/DtpSupplier.java`
- Create: `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/context/DtpContextAwareExecutorService.java`
- Create: `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/context/DtpTaskDecorator.java`
- Create: `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/context/DtpThreads.java`
- Test: `atluofu-dynamic-thread-pool-spring-boot-starter/src/test/java/top/atluofu/middleware/dynamic/thread/pool/sdk/context/DtpContextPropagationTest.java`

- [ ] **Step 1: Write failing context propagation tests**

Create `DtpContextPropagationTest.java`:

```java
package top.atluofu.middleware.dynamic.thread.pool.sdk.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class DtpContextPropagationTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void runnableShouldRestoreAndThenCleanMdc() {
        MDC.put("traceId", "trace-001");
        Runnable wrapped = DtpRunnable.wrap(() -> assertThat(MDC.get("traceId")).isEqualTo("trace-001"));

        MDC.put("traceId", "worker-old");
        wrapped.run();

        assertThat(MDC.get("traceId")).isEqualTo("worker-old");
    }

    @Test
    void callableShouldReturnValueWithCapturedMdc() throws Exception {
        MDC.put("traceId", "trace-002");
        Callable<String> wrapped = DtpCallable.wrap(() -> MDC.get("traceId"));

        MDC.clear();

        assertThat(wrapped.call()).isEqualTo("trace-002");
        assertThat(MDC.get("traceId")).isNull();
    }

    @Test
    void supplierShouldReturnValueWithCapturedMdc() {
        MDC.put("traceId", "trace-003");
        Supplier<String> wrapped = DtpSupplier.wrap(() -> MDC.get("traceId"));

        MDC.clear();

        assertThat(wrapped.get()).isEqualTo("trace-003");
    }

    @Test
    void executorServiceShouldWrapSubmittedTasks() throws Exception {
        ExecutorService delegate = Executors.newSingleThreadExecutor();
        ExecutorService executor = new DtpContextAwareExecutorService(delegate);
        try {
            MDC.put("traceId", "trace-004");
            Future<String> future = executor.submit(() -> MDC.get("traceId"));

            assertThat(future.get()).isEqualTo("trace-004");
        } finally {
            executor.shutdownNow();
        }
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
mvn -pl atluofu-dynamic-thread-pool-spring-boot-starter -Dtest=DtpContextPropagationTest test
```

Expected: FAIL because context classes do not exist.

- [ ] **Step 3: Add context snapshot and wrappers**

Create `DtpContextSnapshot.java`:

```java
package top.atluofu.middleware.dynamic.thread.pool.sdk.context;

import org.slf4j.MDC;

import java.util.Map;

public final class DtpContextSnapshot {

    private final Map<String, String> mdcContext;

    private DtpContextSnapshot(Map<String, String> mdcContext) {
        this.mdcContext = mdcContext;
    }

    public static DtpContextSnapshot capture() {
        return new DtpContextSnapshot(MDC.getCopyOfContextMap());
    }

    public Scope restore() {
        Map<String, String> previous = MDC.getCopyOfContextMap();
        if (mdcContext == null || mdcContext.isEmpty()) {
            MDC.clear();
        } else {
            MDC.setContextMap(mdcContext);
        }
        return new Scope(previous);
    }

    public static final class Scope implements AutoCloseable {

        private final Map<String, String> previous;

        private Scope(Map<String, String> previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (previous == null || previous.isEmpty()) {
                MDC.clear();
            } else {
                MDC.setContextMap(previous);
            }
        }
    }
}
```

Create `DtpRunnable.java`:

```java
package top.atluofu.middleware.dynamic.thread.pool.sdk.context;

public final class DtpRunnable implements Runnable {

    private final Runnable delegate;
    private final DtpContextSnapshot snapshot;

    private DtpRunnable(Runnable delegate, DtpContextSnapshot snapshot) {
        this.delegate = delegate;
        this.snapshot = snapshot;
    }

    public static Runnable wrap(Runnable runnable) {
        if (runnable instanceof DtpRunnable) {
            return runnable;
        }
        return new DtpRunnable(runnable, DtpContextSnapshot.capture());
    }

    @Override
    public void run() {
        try (DtpContextSnapshot.Scope ignored = snapshot.restore()) {
            delegate.run();
        }
    }
}
```

Create `DtpCallable.java`:

```java
package top.atluofu.middleware.dynamic.thread.pool.sdk.context;

import java.util.concurrent.Callable;

public final class DtpCallable<V> implements Callable<V> {

    private final Callable<V> delegate;
    private final DtpContextSnapshot snapshot;

    private DtpCallable(Callable<V> delegate, DtpContextSnapshot snapshot) {
        this.delegate = delegate;
        this.snapshot = snapshot;
    }

    public static <V> Callable<V> wrap(Callable<V> callable) {
        if (callable instanceof DtpCallable<?>) {
            return callable;
        }
        return new DtpCallable<>(callable, DtpContextSnapshot.capture());
    }

    @Override
    public V call() throws Exception {
        try (DtpContextSnapshot.Scope ignored = snapshot.restore()) {
            return delegate.call();
        }
    }
}
```

Create `DtpSupplier.java`:

```java
package top.atluofu.middleware.dynamic.thread.pool.sdk.context;

import java.util.function.Supplier;

public final class DtpSupplier<T> implements Supplier<T> {

    private final Supplier<T> delegate;
    private final DtpContextSnapshot snapshot;

    private DtpSupplier(Supplier<T> delegate, DtpContextSnapshot snapshot) {
        this.delegate = delegate;
        this.snapshot = snapshot;
    }

    public static <T> Supplier<T> wrap(Supplier<T> supplier) {
        if (supplier instanceof DtpSupplier<?>) {
            return supplier;
        }
        return new DtpSupplier<>(supplier, DtpContextSnapshot.capture());
    }

    @Override
    public T get() {
        try (DtpContextSnapshot.Scope ignored = snapshot.restore()) {
            return delegate.get();
        }
    }
}
```

- [ ] **Step 4: Add executor and Spring helpers**

Create `DtpContextAwareExecutorService.java`:

```java
package top.atluofu.middleware.dynamic.thread.pool.sdk.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class DtpContextAwareExecutorService extends AbstractExecutorService {

    private final ExecutorService delegate;

    public DtpContextAwareExecutorService(ExecutorService delegate) {
        this.delegate = delegate;
    }

    @Override
    public void execute(Runnable command) {
        delegate.execute(DtpRunnable.wrap(command));
    }

    @Override
    public Future<?> submit(Runnable task) {
        return delegate.submit(DtpRunnable.wrap(task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return delegate.submit(DtpRunnable.wrap(task), result);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return delegate.submit(DtpCallable.wrap(task));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return delegate.invokeAll(wrapCallables(tasks));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        return delegate.invokeAll(wrapCallables(tasks), timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws Exception {
        return delegate.invokeAny(wrapCallables(tasks));
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws Exception {
        return delegate.invokeAny(wrapCallables(tasks), timeout, unit);
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    private <T> Collection<Callable<T>> wrapCallables(Collection<? extends Callable<T>> tasks) {
        List<Callable<T>> wrapped = new ArrayList<>(tasks.size());
        for (Callable<T> task : tasks) {
            wrapped.add(DtpCallable.wrap(task));
        }
        return wrapped;
    }
}
```

Create `DtpTaskDecorator.java`:

```java
package top.atluofu.middleware.dynamic.thread.pool.sdk.context;

import org.springframework.core.task.TaskDecorator;

public class DtpTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        return DtpRunnable.wrap(runnable);
    }
}
```

Create `DtpThreads.java`:

```java
package top.atluofu.middleware.dynamic.thread.pool.sdk.context;

public final class DtpThreads {

    private DtpThreads() {
    }

    public static Thread startVirtualThread(Runnable task) {
        return Thread.startVirtualThread(DtpRunnable.wrap(task));
    }

    public static Thread newPlatformThread(String name, Runnable task) {
        return Thread.ofPlatform().name(name).unstarted(DtpRunnable.wrap(task));
    }

    public static Thread newVirtualThread(String name, Runnable task) {
        return Thread.ofVirtual().name(name).unstarted(DtpRunnable.wrap(task));
    }
}
```

- [ ] **Step 5: Run context tests**

Run:

```bash
mvn -pl atluofu-dynamic-thread-pool-spring-boot-starter -Dtest=DtpContextPropagationTest test
```

Expected: PASS.

- [ ] **Step 6: Commit Task 4**

```bash
git status --short
git add atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/context atluofu-dynamic-thread-pool-spring-boot-starter/src/test/java/top/atluofu/middleware/dynamic/thread/pool/sdk/context
git commit --only atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/context atluofu-dynamic-thread-pool-spring-boot-starter/src/test/java/top/atluofu/middleware/dynamic/thread/pool/sdk/context -m "feat: add dtp context propagation"
```

## Task 5: Bounded Virtual Thread Executor And Adapter

**Files:**
- Create: `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/executor/virtual/BoundedVirtualThreadExecutor.java`
- Create: `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/executor/adapter/BoundedVirtualThreadManagedExecutor.java`
- Test: `atluofu-dynamic-thread-pool-spring-boot-starter/src/test/java/top/atluofu/middleware/dynamic/thread/pool/sdk/executor/virtual/BoundedVirtualThreadExecutorTest.java`

- [ ] **Step 1: Write failing virtual executor tests**

Create `BoundedVirtualThreadExecutorTest.java`:

```java
package top.atluofu.middleware.dynamic.thread.pool.sdk.executor.virtual;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoundedVirtualThreadExecutorTest {

    @Test
    void shouldRunTaskWithCapturedMdc() throws Exception {
        BoundedVirtualThreadExecutor executor = new BoundedVirtualThreadExecutor("dtp-virtual", 2);
        CountDownLatch latch = new CountDownLatch(1);
        String[] traceId = new String[1];
        MDC.put("traceId", "trace-virtual");

        executor.execute(() -> {
            traceId[0] = MDC.get("traceId");
            latch.countDown();
        });

        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(traceId[0]).isEqualTo("trace-virtual");
        assertThat(executor.completedTasks()).isEqualTo(1);
        executor.shutdownNow();
    }

    @Test
    void shouldRejectWhenConcurrencyLimitExceeded() throws Exception {
        BoundedVirtualThreadExecutor executor = new BoundedVirtualThreadExecutor("dtp-virtual", 1);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        executor.execute(() -> {
            started.countDown();
            try {
                release.await(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        assertThat(started.await(3, TimeUnit.SECONDS)).isTrue();

        assertThatThrownBy(() -> executor.execute(() -> { }))
                .isInstanceOf(RejectedExecutionException.class)
                .hasMessage("Virtual executor concurrency limit exceeded");
        assertThat(executor.rejectedTasks()).isEqualTo(1);

        release.countDown();
        executor.shutdownNow();
    }

    @Test
    void shouldUpdateConcurrencyLimit() {
        BoundedVirtualThreadExecutor executor = new BoundedVirtualThreadExecutor("dtp-virtual", 1);

        executor.updateConcurrencyLimit(3);

        assertThat(executor.concurrencyLimit()).isEqualTo(3);
        assertThat(executor.availablePermits()).isEqualTo(3);
        executor.shutdownNow();
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
mvn -pl atluofu-dynamic-thread-pool-spring-boot-starter -Dtest=BoundedVirtualThreadExecutorTest test
```

Expected: FAIL because virtual executor does not exist.

- [ ] **Step 3: Implement bounded virtual executor**

Create `BoundedVirtualThreadExecutor.java` with this implementation:

```java
package top.atluofu.middleware.dynamic.thread.pool.sdk.executor.virtual;

import top.atluofu.middleware.dynamic.thread.pool.sdk.context.DtpRunnable;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class BoundedVirtualThreadExecutor extends AbstractExecutorService {

    private final ExecutorService delegate;
    private final String threadNamePrefix;
    private final AtomicLong submitted = new AtomicLong();
    private final AtomicLong completed = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();
    private final AtomicLong rejected = new AtomicLong();
    private final AtomicInteger running = new AtomicInteger();
    private volatile Semaphore semaphore;
    private volatile int concurrencyLimit;

    public BoundedVirtualThreadExecutor(String threadNamePrefix, int concurrencyLimit) {
        if (concurrencyLimit <= 0) {
            throw new IllegalArgumentException("concurrencyLimit must be positive");
        }
        this.threadNamePrefix = threadNamePrefix;
        this.concurrencyLimit = concurrencyLimit;
        this.semaphore = new Semaphore(concurrencyLimit);
        this.delegate = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name(threadNamePrefix + "-", 0).factory());
    }

    @Override
    public void execute(Runnable command) {
        submitted.incrementAndGet();
        if (!semaphore.tryAcquire()) {
            rejected.incrementAndGet();
            throw new RejectedExecutionException("Virtual executor concurrency limit exceeded");
        }
        delegate.execute(() -> {
            running.incrementAndGet();
            try {
                DtpRunnable.wrap(command).run();
                completed.incrementAndGet();
            } catch (Throwable ex) {
                failed.incrementAndGet();
                throw ex;
            } finally {
                running.decrementAndGet();
                semaphore.release();
            }
        });
    }

    public synchronized void updateConcurrencyLimit(int newConcurrencyLimit) {
        if (newConcurrencyLimit <= 0) {
            throw new IllegalArgumentException("concurrencyLimit must be positive");
        }
        int usedPermits = Math.max(0, concurrencyLimit - semaphore.availablePermits());
        Semaphore newSemaphore = new Semaphore(newConcurrencyLimit);
        int permitsToAcquire = Math.min(usedPermits, newConcurrencyLimit);
        newSemaphore.tryAcquire(permitsToAcquire);
        this.concurrencyLimit = newConcurrencyLimit;
        this.semaphore = newSemaphore;
    }

    public int concurrencyLimit() {
        return concurrencyLimit;
    }

    public int runningTasks() {
        return running.get();
    }

    public long submittedTasks() {
        return submitted.get();
    }

    public long completedTasks() {
        return completed.get();
    }

    public long failedTasks() {
        return failed.get();
    }

    public long rejectedTasks() {
        return rejected.get();
    }

    public int availablePermits() {
        return semaphore.availablePermits();
    }

    public String threadNamePrefix() {
        return threadNamePrefix;
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }
}
```

- [ ] **Step 4: Implement virtual managed adapter**

Create `BoundedVirtualThreadManagedExecutor.java`:

```java
package top.atluofu.middleware.dynamic.thread.pool.sdk.executor.adapter;

import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ExecutorSnapshot;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ExecutorUpdateCommand;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.UpdateResult;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.valobj.ExecutorKind;
import top.atluofu.middleware.dynamic.thread.pool.sdk.executor.ManagedExecutor;
import top.atluofu.middleware.dynamic.thread.pool.sdk.executor.virtual.BoundedVirtualThreadExecutor;

import java.time.Instant;

public class BoundedVirtualThreadManagedExecutor implements ManagedExecutor {

    private final String appName;
    private final String instanceId;
    private final String executorName;
    private final BoundedVirtualThreadExecutor executor;

    public BoundedVirtualThreadManagedExecutor(String appName, String instanceId, String executorName, BoundedVirtualThreadExecutor executor) {
        this.appName = appName;
        this.instanceId = instanceId;
        this.executorName = executorName;
        this.executor = executor;
    }

    @Override
    public String appName() {
        return appName;
    }

    @Override
    public String instanceId() {
        return instanceId;
    }

    @Override
    public String executorName() {
        return executorName;
    }

    @Override
    public ExecutorKind kind() {
        return ExecutorKind.VIRTUAL_THREAD_PER_TASK;
    }

    @Override
    public ExecutorSnapshot snapshot() {
        ExecutorSnapshot snapshot = new ExecutorSnapshot();
        snapshot.setAppName(appName);
        snapshot.setInstanceId(instanceId);
        snapshot.setExecutorName(executorName);
        snapshot.setExecutorKind(kind());
        snapshot.setVirtual(true);
        snapshot.setResizable(false);
        snapshot.setConcurrencyLimit(executor.concurrencyLimit());
        snapshot.setRunningTasks((long) executor.runningTasks());
        snapshot.setSubmittedTasks(executor.submittedTasks());
        snapshot.setCompletedTaskCount(executor.completedTasks());
        snapshot.setFailedTasks(executor.failedTasks());
        snapshot.setRejectedTasks(executor.rejectedTasks());
        snapshot.setAvailablePermits(executor.availablePermits());
        snapshot.setReportTime(Instant.now());
        return snapshot;
    }

    @Override
    public UpdateResult update(ExecutorUpdateCommand command) {
        ExecutorSnapshot before = snapshot();
        UpdateResult result = new UpdateResult();
        result.setBefore(before);
        try {
            executor.updateConcurrencyLimit(command.getConcurrencyLimit());
            result.setSuccess(true);
            result.setMessage("success");
        } catch (RuntimeException ex) {
            result.setSuccess(false);
            result.setMessage(ex.getMessage());
        }
        result.setAfter(snapshot());
        return result;
    }

    @Override
    public boolean supportsResize() {
        return false;
    }

    @Override
    public boolean supportsVirtualThread() {
        return true;
    }

    @Override
    public boolean supportsQueueMetrics() {
        return false;
    }
}
```

- [ ] **Step 5: Run virtual tests**

Run:

```bash
mvn -pl atluofu-dynamic-thread-pool-spring-boot-starter -Dtest=BoundedVirtualThreadExecutorTest test
```

Expected: PASS.

- [ ] **Step 6: Commit Task 5**

```bash
git status --short
git add atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/executor/virtual atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/executor/adapter/BoundedVirtualThreadManagedExecutor.java atluofu-dynamic-thread-pool-spring-boot-starter/src/test/java/top/atluofu/middleware/dynamic/thread/pool/sdk/executor/virtual
git commit --only atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/executor/virtual atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/executor/adapter/BoundedVirtualThreadManagedExecutor.java atluofu-dynamic-thread-pool-spring-boot-starter/src/test/java/top/atluofu/middleware/dynamic/thread/pool/sdk/executor/virtual -m "feat: add bounded virtual thread executor"
```

## Task 6: Redis Snapshot Keys, Message Envelope, And Audit Events

**Files:**
- Modify: `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/registry/IRegistry.java`
- Modify: `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/registry/redis/RedisRegistry.java`
- Create: `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/registry/model/DtpRedisKeys.java`
- Create: `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/registry/model/DtpConfigChangeMessage.java`
- Create: `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/registry/model/DtpAuditEvent.java`
- Test: `atluofu-dynamic-thread-pool-spring-boot-starter/src/test/java/top/atluofu/middleware/dynamic/thread/pool/sdk/registry/model/DtpRedisKeysTest.java`
- Test: `atluofu-dynamic-thread-pool-spring-boot-starter/src/test/java/top/atluofu/middleware/dynamic/thread/pool/sdk/registry/redis/RedisRegistryTest.java`

- [ ] **Step 1: Write Redis key tests**

Create `DtpRedisKeysTest.java`:

```java
package top.atluofu.middleware.dynamic.thread.pool.sdk.registry.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DtpRedisKeysTest {

    @Test
    void shouldBuildExpectedKeys() {
        assertThat(DtpRedisKeys.apps()).isEqualTo("DTP:APPS");
        assertThat(DtpRedisKeys.instances("order-app")).isEqualTo("DTP:APP:order-app:INSTANCES");
        assertThat(DtpRedisKeys.snapshot("order-app", "order-8093", "orderExecutor"))
                .isEqualTo("DTP:SNAPSHOT:order-app:order-8093:orderExecutor");
        assertThat(DtpRedisKeys.changeTopic("order-app")).isEqualTo("DTP:CHANGE_TOPIC:order-app");
        assertThat(DtpRedisKeys.event("order-app", "20260629")).isEqualTo("DTP:EVENT:order-app:20260629");
    }
}
```

- [ ] **Step 2: Add Redis model classes**

Create `DtpRedisKeys.java`:

```java
package top.atluofu.middleware.dynamic.thread.pool.sdk.registry.model;

public final class DtpRedisKeys {

    private DtpRedisKeys() {
    }

    public static String apps() {
        return "DTP:APPS";
    }

    public static String instances(String appName) {
        return "DTP:APP:" + appName + ":INSTANCES";
    }

    public static String snapshot(String appName, String instanceId, String executorName) {
        return "DTP:SNAPSHOT:" + appName + ":" + instanceId + ":" + executorName;
    }

    public static String changeTopic(String appName) {
        return "DTP:CHANGE_TOPIC:" + appName;
    }

    public static String event(String appName, String yyyyMMdd) {
        return "DTP:EVENT:" + appName + ":" + yyyyMMdd;
    }
}
```

Create `DtpConfigChangeMessage.java`:

```java
package top.atluofu.middleware.dynamic.thread.pool.sdk.registry.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ExecutorUpdateCommand;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.valobj.ExecutorKind;

import java.time.Instant;

@Getter
@Setter
@ToString
public class DtpConfigChangeMessage {

    private String messageId;
    private String traceId;
    private String requestId;
    private String appName;
    private String instanceId;
    private String executorName;
    private ExecutorKind executorKind;
    private ExecutorUpdateCommand payload;
    private String operator;
    private Instant timestamp;
}
```

Create `DtpAuditEvent.java`:

```java
package top.atluofu.middleware.dynamic.thread.pool.sdk.registry.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ExecutorSnapshot;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.valobj.ExecutorKind;

import java.time.Instant;

@Getter
@Setter
@ToString
public class DtpAuditEvent {

    private String eventId;
    private String traceId;
    private String requestId;
    private String appName;
    private String instanceId;
    private String executorName;
    private ExecutorKind executorKind;
    private String operator;
    private String operationType;
    private ExecutorSnapshot beforeValue;
    private ExecutorSnapshot afterValue;
    private boolean success;
    private String errorMessage;
    private Instant createdAt;
}
```

- [ ] **Step 3: Change registry contract**

Replace `IRegistry` methods with:

```java
void reportSnapshot(ExecutorSnapshot snapshot);

void reportSnapshots(List<ExecutorSnapshot> snapshots);

List<ExecutorSnapshot> querySnapshots(String appName, String instanceId);

ExecutorSnapshot querySnapshot(String appName, String instanceId, String executorName);

void publishConfigChange(DtpConfigChangeMessage message);

void recordAuditEvent(DtpAuditEvent event);

List<DtpAuditEvent> queryAuditEvents(String appName, String date);
```

- [ ] **Step 4: Implement Redis registry methods**

In `RedisRegistry`, replace global `THREAD_POOL_CONFIG_LIST_KEY` usage with:

```java
redissonClient.getSet(DtpRedisKeys.apps()).add(snapshot.getAppName());
redissonClient.getSet(DtpRedisKeys.instances(snapshot.getAppName())).add(snapshot.getInstanceId());
redissonClient.<ExecutorSnapshot>getBucket(DtpRedisKeys.snapshot(snapshot.getAppName(), snapshot.getInstanceId(), snapshot.getExecutorName()))
        .set(snapshot, Duration.ofSeconds(90));
```

Use `RTopic.publish(message)` for `publishConfigChange`.

Use `RList<DtpAuditEvent>` at `DtpRedisKeys.event(appName, date)` for events and set expiry on the list key to 30 days.

- [ ] **Step 5: Run Redis model tests**

Run:

```bash
mvn -pl atluofu-dynamic-thread-pool-spring-boot-starter -Dtest=DtpRedisKeysTest,RedisRegistryTest test
```

Expected: PASS after updating `RedisRegistryTest` mock expectations to the new methods and keys.

- [ ] **Step 6: Commit Task 6**

```bash
git status --short
git add atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/registry atluofu-dynamic-thread-pool-spring-boot-starter/src/test/java/top/atluofu/middleware/dynamic/thread/pool/sdk/registry
git commit --only atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/registry atluofu-dynamic-thread-pool-spring-boot-starter/src/test/java/top/atluofu/middleware/dynamic/thread/pool/sdk/registry -m "feat: isolate dtp redis snapshots and events"
```

## Task 7: Starter Wiring, Reporting Job, And Redis Listener

**Files:**
- Modify: `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/config/DynamicThreadPoolAutoConfig.java`
- Modify: `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/domain/DynamicThreadPoolService.java`
- Modify: `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/domain/IDynamicThreadPoolService.java`
- Create: `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/executor/adapter/ThreadPoolTaskExecutorManagedExecutor.java`
- Modify: `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/trigger/job/ThreadPoolDataReportJob.java`
- Modify: `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/trigger/listener/ThreadPoolConfigAdjustListener.java`
- Test: `atluofu-dynamic-thread-pool-spring-boot-starter/src/test/java/top/atluofu/middleware/dynamic/thread/pool/sdk/domain/DynamicThreadPoolServiceTest.java`
- Test: `atluofu-dynamic-thread-pool-spring-boot-starter/src/test/java/top/atluofu/middleware/dynamic/thread/pool/sdk/trigger/listener/ThreadPoolConfigAdjustListenerTest.java`

- [ ] **Step 1: Update service tests to use ManagedExecutorRegistry**

Replace old `DynamicThreadPoolServiceTest` expectations with:

```java
@Test
void shouldQuerySnapshotsFromManagedExecutors() {
    ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 8, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>(100));
    ManagedExecutor managed = new ThreadPoolExecutorManagedExecutor("app", "instance", "orderExecutor", executor);
    DynamicThreadPoolService service = new DynamicThreadPoolService(new ManagedExecutorRegistry(List.of(managed)));

    List<ExecutorSnapshot> snapshots = service.queryExecutorSnapshots();

    assertThat(snapshots).hasSize(1);
    assertThat(snapshots.get(0).getExecutorName()).isEqualTo("orderExecutor");
}
```

Add an update test:

```java
@Test
void shouldUpdateExecutorByName() {
    ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 8, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>(100));
    ManagedExecutor managed = new ThreadPoolExecutorManagedExecutor("app", "instance", "orderExecutor", executor);
    DynamicThreadPoolService service = new DynamicThreadPoolService(new ManagedExecutorRegistry(List.of(managed)));
    ExecutorUpdateCommand command = new ExecutorUpdateCommand();
    command.setExecutorName("orderExecutor");
    command.setCorePoolSize(3);
    command.setMaximumPoolSize(9);

    UpdateResult result = service.updateExecutor(command);

    assertThat(result.isSuccess()).isTrue();
    assertThat(executor.getCorePoolSize()).isEqualTo(3);
    assertThat(executor.getMaximumPoolSize()).isEqualTo(9);
}
```

- [ ] **Step 2: Update domain service API**

Change `IDynamicThreadPoolService` to:

```java
List<ExecutorSnapshot> queryExecutorSnapshots();

ExecutorSnapshot queryExecutorSnapshot(String executorName);

UpdateResult updateExecutor(ExecutorUpdateCommand command);
```

Change `DynamicThreadPoolService` to delegate to `ManagedExecutorRegistry`.

If `executorName` is missing, return `UpdateResult` with `success=false` and `message="executorName must not be blank"`. If no executor is found, return `success=false` and `message="executor not found: " + executorName`.

- [ ] **Step 3: Update auto-configuration executor collection**

In `DynamicThreadPoolAutoConfig`, build `ManagedExecutorRegistry` from:

```java
Map<String, ThreadPoolExecutor> threadPoolExecutorMap
Map<String, ThreadPoolTaskExecutor> threadPoolTaskExecutorMap
Map<String, BoundedVirtualThreadExecutor> boundedVirtualThreadExecutorMap
```

Create adapters with resolved `appName` and `instanceId`. Resolve app and instance with helper methods:

```java
String resolveAppName(ApplicationContext applicationContext, DynamicThreadPoolAutoProperties properties)
String resolveInstanceId(ApplicationContext applicationContext, DynamicThreadPoolAutoProperties properties, String appName)
```

Rules:

- configured `properties.getAppName()` wins;
- otherwise use `spring.application.name`;
- otherwise use `"default-app"`;
- configured `properties.getInstanceId()` wins;
- otherwise use `appName + "-" + server.port`;
- otherwise use `appName + "-" + ManagementFactory.getRuntimeMXBean().getName()`.

- [ ] **Step 4: Update reporting job and listener**

`ThreadPoolDataReportJob` now calls:

```java
List<ExecutorSnapshot> snapshots = dynamicThreadPoolService.queryExecutorSnapshots();
registry.reportSnapshots(snapshots);
```

`ThreadPoolConfigAdjustListener` now implements `MessageListener<DtpConfigChangeMessage>`. Its `onMessage` method must use this exact flow:

```java
@Override
public void onMessage(CharSequence channel, DtpConfigChangeMessage message) {
    try {
        MDC.put("traceId", message.getTraceId());
        MDC.put("requestId", message.getRequestId());
        ExecutorUpdateCommand command = message.getPayload();
        UpdateResult result = dynamicThreadPoolService.updateExecutor(command);
        DtpAuditEvent event = buildAuditEvent(message, result);
        registry.recordAuditEvent(event);
        if (result.getAfter() != null) {
            registry.reportSnapshot(result.getAfter());
        }
    } catch (Exception e) {
        logger.error("动态线程池，配置变更处理失败。应用:{} 实例:{} 执行器:{}",
                message.getAppName(), message.getInstanceId(), message.getExecutorName(), e);
    } finally {
        MDC.clear();
    }
}
```

Add a private `buildAuditEvent(DtpConfigChangeMessage message, UpdateResult result)` method that copies message trace fields, app fields, operator, executor kind, `beforeValue`, `afterValue`, `success`, and `errorMessage`.

- [ ] **Step 5: Run starter domain/listener tests**

Run:

```bash
mvn -pl atluofu-dynamic-thread-pool-spring-boot-starter -Dtest=DynamicThreadPoolServiceTest,ThreadPoolConfigAdjustListenerTest,ThreadPoolDataReportJobTest test
```

Expected: PASS.

- [ ] **Step 6: Commit Task 7**

```bash
git status --short
git add atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/config/DynamicThreadPoolAutoConfig.java atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/domain atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/executor/adapter/ThreadPoolTaskExecutorManagedExecutor.java atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/trigger atluofu-dynamic-thread-pool-spring-boot-starter/src/test/java/top/atluofu/middleware/dynamic/thread/pool/sdk/domain atluofu-dynamic-thread-pool-spring-boot-starter/src/test/java/top/atluofu/middleware/dynamic/thread/pool/sdk/trigger
git commit --only atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/config/DynamicThreadPoolAutoConfig.java atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/domain atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/executor/adapter/ThreadPoolTaskExecutorManagedExecutor.java atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/trigger atluofu-dynamic-thread-pool-spring-boot-starter/src/test/java/top/atluofu/middleware/dynamic/thread/pool/sdk/domain atluofu-dynamic-thread-pool-spring-boot-starter/src/test/java/top/atluofu/middleware/dynamic/thread/pool/sdk/trigger -m "feat: wire managed executors into starter"
```

## Task 8: Admin Trace Filter And New REST API

**Files:**
- Modify: `dynamic-thread-pool-admin/src/main/java/top/atluofu/middleware/dynamic/thread/pool/types/Response.java`
- Replace: `dynamic-thread-pool-admin/src/main/java/top/atluofu/middleware/dynamic/thread/pool/trigger/DynamicThreadPoolController.java`
- Create: `dynamic-thread-pool-admin/src/main/java/top/atluofu/middleware/dynamic/thread/pool/config/DtpTraceIdFilter.java`
- Create: `dynamic-thread-pool-admin/src/main/java/top/atluofu/middleware/dynamic/thread/pool/trigger/model/ResizeExecutorRequest.java`
- Create: `dynamic-thread-pool-admin/src/main/java/top/atluofu/middleware/dynamic/thread/pool/trigger/model/VirtualLimitRequest.java`
- Test: `dynamic-thread-pool-admin/src/test/java/top/atluofu/middleware/dynamic/thread/pool/trigger/DynamicThreadPoolControllerTest.java`

- [ ] **Step 1: Write controller tests for new routes**

Update controller tests to call methods directly:

```java
@Test
void shouldQueryApps() {
    when(mockRedissonClient.getSet(DtpRedisKeys.apps())).thenReturn(mockRSet);
    when(mockRSet.readAll()).thenReturn(Set.of("order-app"));

    Response<Set<String>> response = controller.queryApps();

    assertThat(response.getCode()).isEqualTo(Response.Code.SUCCESS.getCode());
    assertThat(response.getData()).containsExactly("order-app");
}
```

Add a publish test for resize:

```java
@Test
void shouldPublishResizeMessage() {
    ResizeExecutorRequest request = new ResizeExecutorRequest();
    request.setCorePoolSize(4);
    request.setMaximumPoolSize(16);
    request.setOperator("admin");
    when(mockRedissonClient.getTopic(DtpRedisKeys.changeTopic("order-app"))).thenReturn(mockRTopic);

    Response<Boolean> response = controller.resizeExecutor("order-app", "order-8093", "orderExecutor", request);

    assertThat(response.getData()).isTrue();
    verify(mockRTopic).publish(any(DtpConfigChangeMessage.class));
}
```

- [ ] **Step 2: Add trace filter**

Create `DtpTraceIdFilter.java`:

```java
package top.atluofu.middleware.dynamic.thread.pool.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class DtpTraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID = "traceId";

    public static final String REQUEST_ID = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = StringUtils.hasText(request.getHeader("X-Trace-Id"))
                ? request.getHeader("X-Trace-Id")
                : UUID.randomUUID().toString().replace("-", "");
        String requestId = StringUtils.hasText(request.getHeader("X-Request-Id"))
                ? request.getHeader("X-Request-Id")
                : traceId;
        try {
            MDC.put(TRACE_ID, traceId);
            MDC.put(REQUEST_ID, requestId);
            response.setHeader("X-Trace-Id", traceId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID);
            MDC.remove(REQUEST_ID);
        }
    }
}
```

- [ ] **Step 3: Update response model**

Add `traceId` to `Response<T>`. Ensure builder can set it. Add a static helper:

```java
public static <T> Response<T> success(T data) {
    return Response.<T>builder()
            .code(Code.SUCCESS.getCode())
            .info(Code.SUCCESS.getInfo())
            .traceId(MDC.get("traceId"))
            .data(data)
            .build();
}
```

Add `error(String info)` with `traceId(MDC.get("traceId"))`.

- [ ] **Step 4: Replace controller routes**

Create `ResizeExecutorRequest.java`:

```java
package top.atluofu.middleware.dynamic.thread.pool.trigger.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ResizeExecutorRequest {

    private Integer corePoolSize;
    private Integer maximumPoolSize;
    private Long keepAliveSeconds;
    private Boolean allowCoreThreadTimeOut;
    private String operator;
}
```

Create `VirtualLimitRequest.java`:

```java
package top.atluofu.middleware.dynamic.thread.pool.trigger.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class VirtualLimitRequest {

    private Integer concurrencyLimit;
    private String operator;
}
```

Use base path:

```java
@RequestMapping("/api/v1/dtp")
```

Add methods with these signatures:

```java
@GetMapping("/apps")
public Response<Set<String>> queryApps()
```

```java
@GetMapping("/apps/{appName}/instances")
public Response<Set<String>> queryInstances(@PathVariable String appName)
```

```java
@GetMapping("/apps/{appName}/instances/{instanceId}/executors")
public Response<List<ExecutorSnapshot>> queryExecutors(@PathVariable String appName, @PathVariable String instanceId)
```

```java
@GetMapping("/apps/{appName}/instances/{instanceId}/executors/{executorName}")
public Response<ExecutorSnapshot> queryExecutor(...)
```

```java
@PostMapping("/apps/{appName}/instances/{instanceId}/executors/{executorName}/resize")
public Response<Boolean> resizeExecutor(...)
```

```java
@PostMapping("/apps/{appName}/instances/{instanceId}/executors/{executorName}/virtual-limit")
public Response<Boolean> updateVirtualLimit(...)
```

```java
@GetMapping("/events")
public Response<List<DtpAuditEvent>> queryEvents(@RequestParam String appName, @RequestParam String date)
```

Validation behavior:

- resize requires positive core/max and core <= max;
- virtual limit requires positive concurrencyLimit;
- invalid input returns `Response.error("...")`;
- valid update publishes `DtpConfigChangeMessage` to `DtpRedisKeys.changeTopic(appName)`.

Build resize message payload with:

```java
ExecutorUpdateCommand command = new ExecutorUpdateCommand();
command.setAppName(appName);
command.setInstanceId(instanceId);
command.setExecutorName(executorName);
command.setExecutorKind(ExecutorKind.PLATFORM_THREAD_POOL);
command.setCorePoolSize(request.getCorePoolSize());
command.setMaximumPoolSize(request.getMaximumPoolSize());
command.setKeepAliveSeconds(request.getKeepAliveSeconds());
command.setAllowCoreThreadTimeOut(request.getAllowCoreThreadTimeOut());
command.setTraceId(MDC.get("traceId"));
command.setRequestId(MDC.get("requestId"));
command.setOperator(request.getOperator());
```

Build virtual-limit payload with `ExecutorKind.VIRTUAL_THREAD_PER_TASK` and `command.setConcurrencyLimit(request.getConcurrencyLimit())`.

- [ ] **Step 5: Run Admin tests**

Run:

```bash
mvn -pl dynamic-thread-pool-admin -Dtest=DynamicThreadPoolControllerTest test
```

Expected: PASS.

- [ ] **Step 6: Commit Task 8**

```bash
git status --short
git add dynamic-thread-pool-admin/src/main/java/top/atluofu/middleware/dynamic/thread/pool/types/Response.java dynamic-thread-pool-admin/src/main/java/top/atluofu/middleware/dynamic/thread/pool/trigger dynamic-thread-pool-admin/src/main/java/top/atluofu/middleware/dynamic/thread/pool/config/DtpTraceIdFilter.java dynamic-thread-pool-admin/src/test/java/top/atluofu/middleware/dynamic/thread/pool/trigger/DynamicThreadPoolControllerTest.java
git commit --only dynamic-thread-pool-admin/src/main/java/top/atluofu/middleware/dynamic/thread/pool/types/Response.java dynamic-thread-pool-admin/src/main/java/top/atluofu/middleware/dynamic/thread/pool/trigger dynamic-thread-pool-admin/src/main/java/top/atluofu/middleware/dynamic/thread/pool/config/DtpTraceIdFilter.java dynamic-thread-pool-admin/src/test/java/top/atluofu/middleware/dynamic/thread/pool/trigger/DynamicThreadPoolControllerTest.java -m "feat: replace admin dtp rest api"
```

## Task 9: Micrometer Metrics

**Files:**
- Create: `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/metrics/DtpMeterBinder.java`
- Modify: `atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/config/DynamicThreadPoolAutoConfig.java`
- Test: `atluofu-dynamic-thread-pool-spring-boot-starter/src/test/java/top/atluofu/middleware/dynamic/thread/pool/sdk/metrics/DtpMeterBinderTest.java`

- [ ] **Step 1: Write meter binder test**

Create `DtpMeterBinderTest.java`:

```java
package top.atluofu.middleware.dynamic.thread.pool.sdk.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import top.atluofu.middleware.dynamic.thread.pool.sdk.executor.ManagedExecutorRegistry;
import top.atluofu.middleware.dynamic.thread.pool.sdk.executor.adapter.ThreadPoolExecutorManagedExecutor;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class DtpMeterBinderTest {

    @Test
    void shouldRegisterPlatformExecutorMeters() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 8, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>(100));
        ManagedExecutorRegistry registry = new ManagedExecutorRegistry(List.of(
                new ThreadPoolExecutorManagedExecutor("app", "instance", "orderExecutor", executor)
        ));
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

        new DtpMeterBinder(registry).bindTo(meterRegistry);

        assertThat(meterRegistry.find("dtp.executor.pool.core").gauge()).isNotNull();
        assertThat(meterRegistry.find("dtp.executor.pool.max").gauge()).isNotNull();
        assertThat(meterRegistry.find("dtp.executor.queue.size").gauge()).isNotNull();
    }
}
```

- [ ] **Step 2: Implement meter binder**

Create `DtpMeterBinder.java` implementing `MeterBinder`. For each `ManagedExecutor`, take its snapshot and register gauges with tags:

```java
Tags tags = Tags.of(
        "appName", snapshot.getAppName(),
        "instanceId", snapshot.getInstanceId(),
        "executorName", snapshot.getExecutorName(),
        "executorKind", snapshot.getExecutorKind().name(),
        "virtual", String.valueOf(snapshot.isVirtual())
);
```

Use `Gauge.builder("dtp.executor.pool.core", managedExecutor, executor -> value(executor.snapshot().getCorePoolSize())).tags(tags).register(registry);`.

Define helper:

```java
private double value(Number number) {
    return number == null ? 0D : number.doubleValue();
}
```

Register traditional gauges only when `!snapshot.isVirtual()`. Register virtual gauges only when `snapshot.isVirtual()`.

- [ ] **Step 3: Wire bean conditionally**

In `DynamicThreadPoolAutoConfig`, add:

```java
@Bean
@ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
public DtpMeterBinder dtpMeterBinder(ManagedExecutorRegistry managedExecutorRegistry) {
    return new DtpMeterBinder(managedExecutorRegistry);
}
```

- [ ] **Step 4: Run metric test**

Run:

```bash
mvn -pl atluofu-dynamic-thread-pool-spring-boot-starter -Dtest=DtpMeterBinderTest test
```

Expected: PASS.

- [ ] **Step 5: Commit Task 9**

```bash
git status --short
git add atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/metrics atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/config/DynamicThreadPoolAutoConfig.java atluofu-dynamic-thread-pool-spring-boot-starter/src/test/java/top/atluofu/middleware/dynamic/thread/pool/sdk/metrics
git commit --only atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/metrics atluofu-dynamic-thread-pool-spring-boot-starter/src/main/java/top/atluofu/middleware/dynamic/thread/pool/sdk/config/DynamicThreadPoolAutoConfig.java atluofu-dynamic-thread-pool-spring-boot-starter/src/test/java/top/atluofu/middleware/dynamic/thread/pool/sdk/metrics -m "feat: expose dtp micrometer metrics"
```

## Task 10: UI Minimal REST And Executor Kind Adaptation

**Files:**
- Modify: `atluofu-dynamic-thread-pool-ui/src/api/threadPool.js`
- Modify: `atluofu-dynamic-thread-pool-ui/src/views/ThreadPool.vue`

- [ ] **Step 1: Update API client**

Replace `threadPool.js` with functions:

```javascript
import request from '@/utils/request'

export function queryApps() {
  return request({ url: '/apps', method: 'get' })
}

export function queryInstances(appName) {
  return request({ url: `/apps/${appName}/instances`, method: 'get' })
}

export function queryExecutors(appName, instanceId) {
  return request({ url: `/apps/${appName}/instances/${instanceId}/executors`, method: 'get' })
}

export function queryExecutor(appName, instanceId, executorName) {
  return request({ url: `/apps/${appName}/instances/${instanceId}/executors/${executorName}`, method: 'get' })
}

export function resizeExecutor(appName, instanceId, executorName, data) {
  return request({
    url: `/apps/${appName}/instances/${instanceId}/executors/${executorName}/resize`,
    method: 'post',
    data
  })
}

export function updateVirtualLimit(appName, instanceId, executorName, data) {
  return request({
    url: `/apps/${appName}/instances/${instanceId}/executors/${executorName}/virtual-limit`,
    method: 'post',
    data
  })
}
```

In `src/utils/request.js`, set the base URL to the new Admin API prefix:

```javascript
const service = axios.create({
  baseURL: '/api/v1/dtp',
  timeout: 10000
})
```

- [ ] **Step 2: Update table fields**

In `ThreadPool.vue`, replace `threadPoolName` usage with `executorName`. Add columns for:

```text
appName
instanceId
executorName
executorKind
corePoolSize
maximumPoolSize
activeCount
poolSize
queueSize
remainingCapacity
concurrencyLimit
runningTasks
submittedTasks
completedTasks
failedTasks
rejectedTasks
availablePermits
```

For traditional-only cells, display `-` when value is `null` or `undefined`. For virtual-only cells, display `-` when value is `null` or `undefined`.

- [ ] **Step 3: Update edit dialog logic**

Use:

```javascript
const isVirtualExecutor = (row) => row.executorKind === 'VIRTUAL_THREAD_PER_TASK'
```

For traditional executors, submit:

```javascript
await resizeExecutor(editForm.value.appName, editForm.value.instanceId, editForm.value.executorName, {
  corePoolSize: editForm.value.corePoolSize,
  maximumPoolSize: editForm.value.maximumPoolSize,
  keepAliveSeconds: editForm.value.keepAliveSeconds,
  allowCoreThreadTimeOut: editForm.value.allowCoreThreadTimeOut,
  operator: 'admin'
})
```

For virtual executors, submit:

```javascript
await updateVirtualLimit(editForm.value.appName, editForm.value.instanceId, editForm.value.executorName, {
  concurrencyLimit: editForm.value.concurrencyLimit,
  operator: 'admin'
})
```

- [ ] **Step 4: Run UI build**

Run:

```bash
npm --prefix atluofu-dynamic-thread-pool-ui run build
```

Expected: PASS.

- [ ] **Step 5: Commit Task 10**

```bash
git status --short
git add atluofu-dynamic-thread-pool-ui/src/api/threadPool.js atluofu-dynamic-thread-pool-ui/src/views/ThreadPool.vue atluofu-dynamic-thread-pool-ui/src/utils/request.js
git commit --only atluofu-dynamic-thread-pool-ui/src/api/threadPool.js atluofu-dynamic-thread-pool-ui/src/views/ThreadPool.vue atluofu-dynamic-thread-pool-ui/src/utils/request.js -m "feat: adapt ui to dtp rest api"
```

## Task 11: Sample Apps, README, And Full Verification

**Files:**
- Modify: `atluofu-dynamic-thread-pool-test/src/main/java/top/atluofu/config/ThreadPoolConfig.java`
- Modify: `atluofu-dynamic-thread-pool-test2/src/main/java/top/atluofu/config/ThreadPoolConfig.java`
- Modify: `readme.md`
- Modify tests under `atluofu-dynamic-thread-pool-test/src/test/java`

- [ ] **Step 1: Add explicit virtual executor beans to sample apps**

In each sample `ThreadPoolConfig.java`, add:

```java
@Bean("virtualTaskExecutor")
public BoundedVirtualThreadExecutor virtualTaskExecutor() {
    return new BoundedVirtualThreadExecutor("sample-virtual", 100);
}
```

Import:

```java
import top.atluofu.middleware.dynamic.thread.pool.sdk.executor.virtual.BoundedVirtualThreadExecutor;
```

- [ ] **Step 2: Update sample integration tests**

Update sample integration tests to assert virtual executor registration with this assertion:

```java
List<ExecutorSnapshot> snapshots = dynamicThreadPoolService.queryExecutorSnapshots();
assertThat(snapshots).anyMatch(snapshot -> "virtualTaskExecutor".equals(snapshot.getExecutorName())
        && snapshot.getExecutorKind() == ExecutorKind.VIRTUAL_THREAD_PER_TASK);
```

Update old `ThreadPoolConfigEntity` imports to `ExecutorSnapshot`.

- [ ] **Step 3: Refresh README**

Update README sections:

- prerequisites: JDK 21, Maven 3.9+, Redis, Node.js;
- configuration prefix: `atluofu.dynamic.thread-pool`;
- Redis keys: `DTP:APPS`, `DTP:APP:{appName}:INSTANCES`, `DTP:SNAPSHOT:{appName}:{instanceId}:{executorName}`, `DTP:CHANGE_TOPIC:{appName}`, `DTP:EVENT:{appName}:{yyyyMMdd}`;
- Admin API: new `/api/v1/dtp` routes;
- virtual executor: business declares `BoundedVirtualThreadExecutor` bean;
- validation commands from this plan.

- [ ] **Step 4: Run full backend tests**

Run:

```bash
mvn clean test
```

Expected: PASS.

- [ ] **Step 5: Run full package build**

Run:

```bash
mvn clean package
```

Expected: PASS.

- [ ] **Step 6: Run UI build**

Run:

```bash
npm --prefix atluofu-dynamic-thread-pool-ui run build
```

Expected: PASS.

- [ ] **Step 7: Commit Task 11**

```bash
git status --short
git add atluofu-dynamic-thread-pool-test/src/main/java/top/atluofu/config/ThreadPoolConfig.java atluofu-dynamic-thread-pool-test2/src/main/java/top/atluofu/config/ThreadPoolConfig.java atluofu-dynamic-thread-pool-test/src/test readme.md
git commit --only atluofu-dynamic-thread-pool-test/src/main/java/top/atluofu/config/ThreadPoolConfig.java atluofu-dynamic-thread-pool-test2/src/main/java/top/atluofu/config/ThreadPoolConfig.java atluofu-dynamic-thread-pool-test/src/test readme.md -m "docs: refresh dtp upgrade usage"
```

## Final Verification Checklist

Before reporting implementation complete, run:

```bash
mvn clean test
mvn clean package
npm --prefix atluofu-dynamic-thread-pool-ui run build
git status --short
```

Expected:

- Maven tests pass.
- Maven package passes.
- UI build passes.
- Only intentional files remain changed.
- No application server was started automatically.

## Self-Review Notes

Spec coverage:

- JDK 21 and Boot 3.5: Task 1.
- New prefix and old prefix deletion: Task 2.
- Managed executor abstraction: Task 3 and Task 7.
- Safe traditional resize: Task 3.
- MDC and trace context: Task 4 and Task 8.
- Virtual executor as user-declared bean: Task 5 and Task 11.
- Redis trace envelope, key isolation, audit events: Task 6 and Task 7.
- Admin REST API only: Task 8.
- Micrometer required: Task 9.
- UI minimal adaptation: Task 10.
- README and full validation: Task 11.

Type consistency:

- `ExecutorSnapshot`, `ExecutorUpdateCommand`, `UpdateResult`, `ExecutorKind`, and `ManagedExecutor` names match the approved spec.
- REST route names match the approved spec.
- Redis key names match the approved spec.

Placeholder scan:

- The plan avoids unresolved placeholders and makes all scope decisions explicit.
