package top.atluofu.middleware.dynamic.thread.pool.sdk.registry.model;

/**
 * @author      有罗敷的马同学
 * @description 动态线程池 Redis key 模型
 * @Date        上午12:20 2026/6/30
 **/
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
