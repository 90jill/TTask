package jp.co.lib.tkato.tktask.latch;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AbortableCountDownLatch extends CountDownLatch {

    // region Exception

    public static class AbortedException extends InterruptedException {

        public AbortedException() {
        }

        public AbortedException(String detailMessage) {
            super(detailMessage);
        }
    }

    // endregion Exception

    protected boolean isAborted = false;

    public AbortableCountDownLatch(int count) {
        super(count);
    }

    public void abort() {
        if (0 == getCount()) {
            return;
        }

        isAborted = true;
        while (0 < getCount()) {
            countDown();
        }
    }

    public boolean await(long timeoutMsec) throws InterruptedException {
        return await(timeoutMsec, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        final boolean result = super.await(timeout,unit);
        if (isAborted) {
            throw new AbortedException();
        }
        return result;
    }

    @Override
    public void await() throws InterruptedException {
        super.await();
        if (isAborted) {
            throw new AbortedException();
        }
    }
}
