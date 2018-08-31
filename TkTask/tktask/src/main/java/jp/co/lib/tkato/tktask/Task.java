package jp.co.lib.tkato.tktask;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.concurrent.ExecutorService;

import jp.co.lib.tkato.tktask.interfaces.ITask;
import jp.co.lib.tkato.tktask.latch.AbortableCountDownLatch;
import timber.log.Timber;

public class Task<T> extends AbstractTask implements ITask<T> {

    // region inner interface/class

    public interface Executable {
        void executable(ITask self) throws Exception;
    }

    public interface Callable<T> {
        T call(ITask self) throws Exception;
    }

    public interface Completionable {
        void completionable(ITask self) throws Exception;
    }

    interface OnSuccessListener {
    }

    public interface OnSuccessNonResultListener extends OnSuccessListener {
        void success();
    }

    public interface OnSuccessResultListener<T> extends OnSuccessListener {
        void success(@Nullable T result);
    }

    public interface OnFailureListener {
        void failure(Exception e);
    }

    abstract static class TaskExecutorRunnable<T> implements Runnable {

        Task<T> task;

        TaskExecutorRunnable(Task<T> task) {
            this.task = task;
        }

        @Override
        public abstract void run();
    }

    // endregion inner interface/class

    // region property

    private Callable<T> callable;
    private Executable  executable;

    private OnSuccessListener success;
    private OnFailureListener failure;
    private Completionable    completion;

    @Nullable
    public OnSuccessResultListener<T> getOnSuccessResultListener() {
        return (OnSuccessResultListener<T>) success;
    }

    @Nullable
    public OnSuccessNonResultListener getOnSuccessNonResultListener() {
        return (OnSuccessNonResultListener) success;
    }

    private T result;

    @Nullable
    public T getResult() {
        return result;
    }

    // region parent counter

    private AbortableCountDownLatch parentCounter;

    private AbortableCountDownLatch getParentCounter() {
        synchronized (this) {
            return parentCounter;
        }
    }

    ITask setParentCounter(AbortableCountDownLatch parentCounter) {
        synchronized (this) {
            this.parentCounter = parentCounter;
        }
        return this;
    }

    // endregion count down latch

    // region implements ITask

    private ITask _onSuccess(OnSuccessListener success) {
        this.success = success;
        return this;
    }

    @Override
    public ITask onSuccess(OnSuccessNonResultListener success) {
        return _onSuccess(success);
    }

    @Override
    public ITask onSuccess(OnSuccessResultListener success) {
        return _onSuccess(success);
    }

    @Override
    public ITask onFailure(OnFailureListener failure) {
        this.failure = failure;
        return this;
    }

    @Override
    public ITask onCompletion(Completionable completion) {
        this.completion = completion;
        return this;
    }

    // endregion implements ITask

    // endregion property

    // region constructor

    public Task() {
        super();
    }

    public Task(Executable executable) {
        super();
        this.executable = executable;
    }

    public Task(Callable<T> callable) {
        super();
        this.callable = callable;
    }

    public Task(@NonNull ExecutorService executorService) {
        super(executorService);
    }

    public Task(@NonNull ExecutorService executorService, Executable executable) {
        super(executorService);
        this.executable = executable;
    }

    public Task(@NonNull ExecutorService executorService, Callable<T> callable) {
        super(executorService);
        this.callable = callable;
    }

    // endregion constructor

    // region implements ITask

    @Override
    public ITask onExecute(Executable executable) {
        this.executable = executable;
        return this;
    }

    @Override
    public ITask onExecute(Callable<T> callable) {
        this.callable = callable;
        return this;
    }

    @Override
    public ITask await() {

        if (null == getCounter()) {
            setCounter(new AbortableCountDownLatch(1));
        }

        if (null == future) {
            Timber.w(getClass().getSimpleName(), "await fail: not called execute");
            return this;
        }

        try {
            getCounter().await();

            if (null != completion) {
                completion.completionable(this);
            }

        } catch (InterruptedException e) {
            e.printStackTrace();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return this;
    }

    @Override
    public ITask start() {
        // TODO:
//        ITask self = null;
//        if (null != executable) {
//            self = _startExecutable(this.executable);
//        } else if (null != callable) {
//            self = _startCallable(this.callable);
//        }

        // TaskGroup を使用した祭に ANR が発生するため、現在、空 Executable を挿入している
//        if (null == self) {
//            Timber.w(getClass().getSimpleName(), "start fail: not working, null executable and callable");
//            return this;
//        }

        if (null != executable) {
            _startExecutable(this.executable);
        } else if (null != callable) {
            _startCallable(this.callable);
        } else {
            _startExecutable((task) -> {});
        }

        if (null != getCounter()) {
            await();
        }
        return this;
    }

    @Override
    public void abort() {
        try {
            final AbortableCountDownLatch counter = getCounter();
            if (null != counter) {
                counter.abort();
            }
            if (null != future) {
                future.cancel(true); // true でタスク内容を実行しない
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // endregion implements ITask

    // region private

    private ITask _startExecutable(Executable executable) {

        future = executor.submit(new TaskExecutorRunnable(this) {
            @Override
            public void run() {
                try {
                    if (Thread.interrupted()) {
                        Timber.d(task.getClass().getSimpleName(), "execute: interrupted by abort before executable");
                        return;
                    }

                    task.executable.executable(task);

                    if (Thread.interrupted()) {
                        Timber.d(task.getClass().getSimpleName(), "execute: interrupted by abort after executable");
                        return;
                    }

                    if (null != task.success) {
                        if (task.success instanceof OnSuccessResultListener) {
                            task.getOnSuccessResultListener().success(null);
                        } else if (task.success instanceof OnSuccessNonResultListener) {
                            task.getOnSuccessNonResultListener().success();
                        }
                    }

                    // region count down and completion

                    final AbortableCountDownLatch c = task.getCounter();
                    if (null != c) {
                        c.countDown();
                    }

                    final AbortableCountDownLatch p = task.getParentCounter();
                    if (null != p) {
                        p.countDown();
                    }

                    if (null == c) {
                        if (null != task.completion) {
                            task.completion.completionable(task);
                        }
                    }

                    // endregion count down and completion

                } catch (InterruptedException e) {
                    // when abort
                    Timber.w(task.getClass().getSimpleName(), "execute: interrupted = " + e);
                    // e.printStackTrace();

                } catch (Exception e) {
                    e.printStackTrace();
                    if (null != failure) {
                        task.failure.failure(e);
                    }

                    // region count down

                    final AbortableCountDownLatch c = task.getCounter();
                    if (null != c) {
                        c.countDown();
                    }
                    final AbortableCountDownLatch p = task.getParentCounter();
                    if (null != p) {
                        p.countDown();
                    }

                    // endregion count down
                }
            }
        });

        return this;
    }

    private ITask _startCallable(Callable<T> callable) {

        future = executor.submit(new TaskExecutorRunnable<T>(this) {
            @Override
            public void run() {
                try {
                    if (Thread.interrupted()) {
                        Timber.d(task.getClass().getSimpleName(), "execute: interrupted by abort before callable");
                        return;
                    }

                    task.result = task.callable.call(task);

                    if (Thread.interrupted()) {
                        Timber.d(task.getClass().getSimpleName(), "execute: interrupted by abort after callable");
                        return;
                    }

                    if (null != task.success) {
                        if (task.success instanceof OnSuccessResultListener) {
                            task.getOnSuccessResultListener().success(task.result);
                        } else if (task.success instanceof OnSuccessNonResultListener) {
                            task.getOnSuccessNonResultListener().success();
                        }
                    }

                    // region count down and completion

                    final AbortableCountDownLatch c = task.getCounter();
                    if (null != c) {
                        c.countDown();
                    }

                    final AbortableCountDownLatch p = task.getParentCounter();
                    if (null != p) {
                        p.countDown();
                    }

                    if (null == c) {
                        if (null != task.completion) {
                            task.completion.completionable(task);
                        }
                    }

                    // endregion count down and completion

                } catch (Exception e) {
                    e.printStackTrace();
                    if (null != task.failure) {
                        task.failure.failure(e);
                    }

                    // region count down

                    final AbortableCountDownLatch c = task.getCounter();
                    if (null != c) {
                        c.countDown();
                    }
                    final AbortableCountDownLatch p = task.getParentCounter();
                    if (null != p) {
                        p.countDown();
                    }

                    // endregion count down
                }
            }
        });

        return this;
    }

    // endregion private
}
