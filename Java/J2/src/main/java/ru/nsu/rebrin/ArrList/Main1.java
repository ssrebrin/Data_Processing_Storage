package ru.nsu.rebrin.ArrList;

import ru.nsu.rebrin.Self.SortWorker;
import ru.nsu.rebrin.Self.StepCounter;
import ru.nsu.rebrin.Self.SyncLinkedList;

public class Main1 {
    public static void main(String[] args) throws InterruptedException {
        SyncArrayList list = new SyncArrayList();
        StepCounter counter = new StepCounter();

        list.addFirst("a");
        list.addFirst("b");
        list.addFirst("c");
        list.addFirst("d");
        list.addFirst("e");
        list.addFirst("f");
        list.addFirst("g");
        list.addFirst("h");
        ArrayListWorker w1 = new ArrayListWorker(list, 10, counter);
        ArrayListWorker w2 = new ArrayListWorker(list, 10, counter);

        w1.start();
        w2.start();


        SyncLinkedList list2 = new SyncLinkedList();
        StepCounter counter2 = new StepCounter();

        list2.addFirst("a");
        list2.addFirst("b");
        list2.addFirst("c");
        list2.addFirst("d");
        list2.addFirst("e");
        list2.addFirst("f");
        list2.addFirst("g");
        list2.addFirst("h");

        SortWorker w3 = new SortWorker(list2, 10, counter2);
        SortWorker w4 = new SortWorker(list2, 10, counter2);

        w3.start();
        w4.start();

        Thread.sleep(5000);

        System.out.println("Arr 1: " + list.snapshot());
        System.out.println("Steps 1: " + counter.get());
        w1.interrupt();
        w2.interrupt();

        System.out.println("Arr 2: " + list2.snapshot());
        System.out.println("Steps 2: " + counter2.get());

        w3.interrupt();
        w4.interrupt();
    }
}
