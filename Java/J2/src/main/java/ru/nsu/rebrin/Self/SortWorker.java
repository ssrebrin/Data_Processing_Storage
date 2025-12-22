package ru.nsu.rebrin.Self;

public class SortWorker extends Thread {

    private final SyncLinkedList list;
    private final int delayMs;
    private final StepCounter counter;

    public SortWorker(SyncLinkedList list, int delayMs, StepCounter counter) {
        this.list = list;
        this.delayMs = delayMs;
        this.counter = counter;
    }

    @Override
    public void run() {
        while (true) {
            SyncLinkedList.Node start = list.stealWork();
            bubbleFrom(start);
        }
    }

    private void bubbleFrom(SyncLinkedList.Node start) {
        SyncLinkedList.Node cur = start;

        while (true) {


            synchronized (cur.lock) {
                if (cur.next == null) {
                    return;
                }
                synchronized (cur.next.lock) {

                        SyncLinkedList.Node next = cur.next;
                        if (next == null) return;

                        if (cur.value.compareTo(next.value) <= 0) {
                            return;
                        }

                        boolean swapped = list.trySwap(cur, next);


                        if (!swapped) {
                            return;
                        }
                        counter.inc();

                        try {
                            Thread.sleep(delayMs);
                        } catch (InterruptedException ignored) {
                        }
                }
            }

        }
    }

}

