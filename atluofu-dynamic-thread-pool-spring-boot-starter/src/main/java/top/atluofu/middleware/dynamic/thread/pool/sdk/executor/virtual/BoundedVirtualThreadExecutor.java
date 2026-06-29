package top.atluofu.middleware.dynamic.thread.pool.sdk.executor.virtual;

import top.atluofu.middleware.dynamic.thread.pool.sdk.context.DtpRunnable;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author      有罗敷的马同学
 * @description 有界虚拟线程执行器
 * @Date        下午9:52 2026/6/29
 **/
public class BoundedVirtualThreadExecutor extends AbstractExecutorService {

    private final String threadNamePrefix;

    private final AdjustableSemaphore semaphore;

    private final ExecutorService delegate;

    private final AtomicInteger concurrencyLimit;

    private final AtomicLong runningTasks = new AtomicLong();

    private final AtomicLong submittedTasks = new AtomicLong();

    private final AtomicLong completedTasks = new AtomicLong();

    private final AtomicLong failedTasks = new AtomicLong();

    private final AtomicLong rejectedTasks = new AtomicLong();

    public BoundedVirtualThreadExecutor(String threadNamePrefix, int concurrencyLimit) {
        if (concurrencyLimit <= 0) {
            throw new IllegalArgumentException("concurrencyLimit must be positive");
        }
        this.threadNamePrefix = Objects.requireNonNull(threadNamePrefix, "threadNamePrefix");
        this.concurrencyLimit = new AtomicInteger(concurrencyLimit);
        this.semaphore = new AdjustableSemaphore(concurrencyLimit);
        this.delegate = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name(threadNamePrefix + "-", 0).factory()
        );
    }

    @Override
    public void execute(Runnable command) {
        Objects.requireNonNull(command, "command");
        submittedTasks.incrementAndGet();
        if (!semaphore.tryAcquire()) {
            rejectedTasks.incrementAndGet();
            throw new RejectedExecutionException("Virtual executor concurrency limit exceeded");
        }
        Runnable wrapped = DtpRunnable.wrap(command);
        boolean accepted = false;
        try {
            delegate.execute(() -> {
                runningTasks.incrementAndGet();
                try {
                    wrapped.run();
                    completedTasks.incrementAndGet();
                } catch (Throwable e) {
                    failedTasks.incrementAndGet();
                    throw e;
                } finally {
                    runningTasks.decrementAndGet();
                    semaphore.release();
                }
            });
            accepted = true;
        } catch (RejectedExecutionException e) {
            rejectedTasks.incrementAndGet();
            throw e;
        } finally {
            if (!accepted) {
                semaphore.release();
            }
        }
    }

    public int concurrencyLimit() {
        return concurrencyLimit.get();
    }

    public long runningTasks() {
        return runningTasks.get();
    }

    public long submittedTasks() {
        return submittedTasks.get();
    }

    public long completedTasks() {
        return completedTasks.get();
    }

    public long failedTasks() {
        return failedTasks.get();
    }

    public long rejectedTasks() {
        return rejectedTasks.get();
    }

    public int availablePermits() {
        return semaphore.availablePermits();
    }

    public String threadNamePrefix() {
        return threadNamePrefix;
    }

    public void updateConcurrencyLimit(int concurrencyLimit) {
        if (concurrencyLimit <= 0) {
            throw new IllegalArgumentException("concurrencyLimit must be positive");
        }
        int previous = this.concurrencyLimit.getAndSet(concurrencyLimit);
        int delta = concurrencyLimit - previous;
        if (delta > 0) {
            semaphore.release(delta);
        } else if (delta < 0) {
            semaphore.reduce(-delta);
        }
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

    private static final class AdjustableSemaphore extends Semaphore {

        private AdjustableSemaphore(int permits) {
            super(permits);
        }

        private void reduce(int reduction) {
            reducePermits(reduction);
        }

    }

}
