package ru.nsu.rebrin.ArrList;

import ru.nsu.rebrin.Self.StepCounter;

public class ArrayListWorker extends Thread {
    private final SyncArrayList list;
    private final int delayMs;
    private final StepCounter counter;

    public ArrayListWorker(SyncArrayList list, int delayMs, StepCounter counter) {
        this.list = list;
        this.delayMs = delayMs;
        this.counter = counter;
    }

    @Override
    public void run() {
        while (true) {
            boolean swapped = false;
            int n = list.size();
            for (int i = 0; i < n - 1; i++) {
                String a = list.get(i);
                String b = list.get(i + 1);
                if (a.compareTo(b) > 0) {
                    list.swap(i, i + 1);
                    counter.inc();
                    swapped = true;
                }
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ignored) {}
            }
            if (!swapped) {
                try { Thread.sleep(delayMs); } catch (InterruptedException ignored) {}
            }
        }
    }
}
