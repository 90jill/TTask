package jp.co.lib.tkato.tktask.interfaces;

import jp.co.lib.tkato.tktask.TaskGroup;

public interface ITaskGroup {

    ITaskGroup add(ITask task);

    ITaskGroup onCompletion(TaskGroup.Completionable completion);

    ITaskGroup start();

    // TaskGroup 自体の処理完了を待つ
    ITaskGroup await();

    // TaskGroup に登録された Task 全体の処理を待つ
    ITaskGroup internalAwait();

    void abort();
}
