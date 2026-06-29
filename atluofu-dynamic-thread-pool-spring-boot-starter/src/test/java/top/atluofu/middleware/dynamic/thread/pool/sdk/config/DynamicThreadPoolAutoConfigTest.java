package top.atluofu.middleware.dynamic.thread.pool.sdk.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ExecutorSnapshot;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @description 动态线程池自动配置单元测试
 */
public class DynamicThreadPoolAutoConfigTest {

    @Test
    public void test_createRedisObjectMapper_serializeInstant() throws Exception {
        ObjectMapper objectMapper = DynamicThreadPoolAutoConfig.createRedisObjectMapper();

        ExecutorSnapshot snapshot = new ExecutorSnapshot();
        snapshot.setAppName("test-app");
        snapshot.setInstanceId("i-001");
        snapshot.setExecutorName("executor01");
        snapshot.setReportTime(Instant.parse("2026-06-29T10:15:30Z"));

        String json = objectMapper.writeValueAsString(snapshot);

        assertTrue(json.contains("reportTime"));
    }

}
