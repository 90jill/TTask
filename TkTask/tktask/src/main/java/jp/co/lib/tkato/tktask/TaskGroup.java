package jp.co.lib.tkato.tktask;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

import jp.co.lib.tkato.tktask.interfaces.ITask;
import jp.co.lib.tkato.tktask.interfaces.ITaskGroup;
import jp.co.lib.tkato.tktask.latch.AbortableCountDownLatch;

public class TaskGroup extends AbstractTask implements ITaskGroup {

    private static class InternalAwait extends AbortableCountDownLatch {
        InternalAwait(int count) {
            super(count);
        }
    }

    public interface Completionable {
        void completionable(TaskGroup self) throws Exception;
    }

    abstract static class TaskGroupExecutorRunnable implements Runnable {

        TaskGroup taskGroup;

        TaskGroupExecutorRunnable(TaskGroup taskGroup) {
            this.taskGroup = taskGroup;
        }

        @Override
        public abstract void run();
    }

    private List<ITask> taskList = Collections.synchronizedList(new ArrayList<>());

    public List<ITask> getTaskList() {
        return taskList;
    }

    private Completionable completion;

    public TaskGroup() {
        super();
    }

    public TaskGroup(@NonNull ExecutorService executorService) {
        super(executorService);
    }

    // region implements ITaskGroup

    @Override
    public ITaskGroup add(ITask task) {
        taskList.add(task);
        return this;
    }

    @Override
    public ITaskGroup onCompletion(Completionable completion) {
        this.completion = completion;
        return this;
    }

    @Override
    public ITaskGroup start() {

        future = executor.submit(new TaskGroupExecutorRunnable(this) {
            @Override
            public void run() {
                try {
                    if (Thread.interrupted()) {
                        Log.d(taskGroup.getClass().getSimpleName(), "execute: interrupted by abort before runnable");
                        return;
                    }

                    for (ITask task : taskGroup.taskList) {
                        task.start();
                    }

                    if (Thread.interrupted()){
                        Log.d(taskGroup.getClass().getSimpleName(), "execute: interrupted by abort after runnable");
                    }

                } catch (Exception e) {
                    e.printStackTrace();

                } finally {

                    final AbortableCountDownLatch c = taskGroup.getCounter();

                    try {
                        if (null == c) {
                            if (null != taskGroup.completion) {
                                taskGroup.completion.completionable(taskGroup);
                            }

                        } else if (c instanceof InternalAwait) {
                            try {
                                c.await();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if (null != taskGroup.completion) {
                                taskGroup.completion.completionable(taskGroup);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        final AbortableCountDownLatch c = getCounter();
        if (null != c && !(c instanceof InternalAwait)) {
            await();
        }

        return this;
    }

    @Override
    public ITaskGroup await() {

        if (null == getCounter()) {
            setCounter(new AbortableCountDownLatch(taskList.size()));
            for (ITask task : taskList) {
                ((Task) task).setParentCounter(getCounter());
            }
        }

        if (null == future) {
            // Timber.w(getClass().getSimpleName(), "await fail: not called execute");
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
    public ITaskGroup internalAwait() {

        if (null == getCounter()) {
            setCounter(new InternalAwait(taskList.size()));

            for (ITask task : taskList) {
                ((Task) task).setParentCounter(getCounter());
            }
        }

        return this;
    }

    @Override
    public void abort() {
        try {
            getCounter().abort();
            for (ITask task : taskList) {
                task.abort();
            }
            if (null != future) {
                future.cancel(true); // true 指定で、タスク内容を実行しない
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // endregion implements ITaskGroup
}
