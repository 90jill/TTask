package jp.co.lib.tkato.tktask;

import android.support.annotation.NonNull;

import java.util.concurrent.ExecutorService;

import jp.co.lib.tkato.tktask.interfaces.ITask;

public abstract class ExecutableTask extends Task implements Task.Executable {

    // region constructor

    public ExecutableTask() {
        super();
        this.onExecute(this);
    }

    public ExecutableTask(Executable executable) {
        super(executable);
        this.onExecute(this);
    }

    public ExecutableTask(@NonNull ExecutorService executorService) {
        super(executorService);
        this.onExecute(this);
    }

    public ExecutableTask(@NonNull ExecutorService executorService, Executable executable) {
        super(executorService, executable);
    }

    // endregion constructor

    @Override
    public abstract void executable(ITask self) throws Exception;
}
