package top.atluofu.middleware.dynamic.thread.pool.sdk.context;

/**
 * @author      有罗敷的马同学
 * @description DTP 上下文感知 Runnable
 * @Date        下午9:27 2026/6/29
 **/
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
