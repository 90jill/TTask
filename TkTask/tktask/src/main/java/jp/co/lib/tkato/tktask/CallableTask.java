package jp.co.lib.tkato.tktask;

import android.support.annotation.NonNull;

import java.util.concurrent.ExecutorService;

import jp.co.lib.tkato.tktask.interfaces.ITask;

public abstract class CallableTask<T> extends Task<T> implements Task.Callable<T> {

    // region constructor

    public CallableTask() {
        super();
        onExecute(this);
    }

    public CallableTask(Callable<T> callable) {
        super(callable);
        onExecute(this);
    }

    public CallableTask(@NonNull ExecutorService executorService) {
        super(executorService);
        onExecute(this);
    }

    public CallableTask(@NonNull ExecutorService executorService, Task.Callable<T> callable) {
        super(executorService, callable);
    }

    // endregion constructor

    @Override
    public abstract T call(ITask self) throws Exception;
}
