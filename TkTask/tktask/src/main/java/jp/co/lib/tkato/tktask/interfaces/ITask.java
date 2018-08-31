package jp.co.lib.tkato.tktask.interfaces;

import jp.co.lib.tkato.tktask.Task;

public interface ITask<T> {

    ITask onSuccess(Task.OnSuccessNonResultListener success);
    ITask onSuccess(Task.OnSuccessResultListener success);

    ITask onFailure(Task.OnFailureListener failure);

    ITask onCompletion(Task.Completionable completion);

    ITask onExecute(Task.Executable executable);
    ITask onExecute(Task.Callable<T> callable);

    // Task の処理完了を待つ
    ITask await();

    ITask start();

    void abort();
}
