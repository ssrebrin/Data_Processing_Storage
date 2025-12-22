package ru.nsu.rebrin.Self;

import java.util.concurrent.atomic.AtomicLong;

public class StepCounter {
    private final AtomicLong counter = new AtomicLong();

    public void inc() {
        counter.incrementAndGet();
    }

    public long get() {
        return counter.get();
    }
}
