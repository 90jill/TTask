package jp.co.lib.tkato.tktask;

import android.support.annotation.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import jp.co.lib.tkato.tktask.latch.AbortableCountDownLatch;

abstract class AbstractTask {

    // region count down latch

    protected AbortableCountDownLatch counter;

    protected final AbortableCountDownLatch getCounter() {
        synchronized (this) {
            return counter;
        }
    }

    protected final void setCounter(AbortableCountDownLatch counter) {
        synchronized (this) {
            this.counter = counter;
        }
    }

    // endregion count down latch

    // ExecutorService について
    // https://qiita.com/amay077/items/b5f4e98b50d7fbcbaaec
    // https://qiita.com/mmmm/items/f33b757119fc4dbd6aa1
    // https://java.keicode.com/lang/multithreading-cancel.php

    ExecutorService executor;
    Future          future;

    AbstractTask() {
//        this.executor = Executors.newSingleThreadExecutor(); // シングルスレッドなので並列に処理されない
        this.executor = Executors.newCachedThreadPool(); // 必要に応じて複数スレッド生成
    }

    AbstractTask(@NonNull final ExecutorService executor) {
        this.executor = executor;
    }
}
