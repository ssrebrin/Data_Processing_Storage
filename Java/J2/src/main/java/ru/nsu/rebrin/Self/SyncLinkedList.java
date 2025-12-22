package ru.nsu.rebrin.Self;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SyncLinkedList implements Iterable<String> {

    static class Node {
        String value;
        Node next;
        public Node prev;
        final Object lock = new Object();
        final Object nextLock = new Object();

        Node(String value) {
            this.value = value;
        }
    }

    private Node head;
    private Node tail;

    private Node lastUnprocessed;

    private final Object processLock = new Object();
    private final Object listLock = new Object();

    public void addFirst(String value) {
        Node n = new Node(value);

        synchronized (listLock) {
            if (head == null) {
                head = new Node("head");
                n.prev = head;
                head.next = tail = n;
            } else {
                n.next = head.next;
                head.next.prev = n;
                head.next = n;
                n.prev = head;
            }
        }

        synchronized (processLock) {
            if (lastUnprocessed == head) {
                lastUnprocessed = head.next;
            }
            if (lastUnprocessed == null) {
                lastUnprocessed = tail;
            }
            processLock.notifyAll();
        }
    }

    public Node stealWork() {
        synchronized (processLock) {
            while (lastUnprocessed == null || lastUnprocessed == head) {
                try {
                    processLock.wait();
                } catch (InterruptedException ignored) {}
            }

            Node work = lastUnprocessed;
            lastUnprocessed = work.prev;
            return work;
        }
    }

    public boolean trySwap(Node a, Node b) {
        synchronized (listLock) {
            if (b == null) return false;

            if (a.value.compareTo(b.value) <= 0) {
                return false;
            }

            Node aPrev = a.prev;
            Node bNext = b.next;

            aPrev.next = b;
            b.prev = aPrev;

            b.next = a;
            a.prev = b;

            a.next = bNext;
            if (bNext != null) bNext.prev = a;

            if (b == tail) tail = a;

            return true;
        }
    }

    public List<String> snapshot() {
        ArrayList<String> list = new ArrayList<>();
        synchronized (listLock) {
            for (Node n = head.next; n != null; n = n.next) {
                list.add(n.value);
            }
            return new ArrayList<>(list);
        }
    }


    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {
            Node cur = head;
            public boolean hasNext() { return cur != null; }
            public String next() {
                String s;
                synchronized (listLock) {
                    s = cur.value;
                    cur = cur.next;
                }
                return s;
            }
        };
    }
}
