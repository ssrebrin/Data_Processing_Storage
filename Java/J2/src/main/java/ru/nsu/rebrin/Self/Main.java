package ru.nsu.rebrin.Self;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        SyncLinkedList list = new SyncLinkedList();
        StepCounter counter = new StepCounter();

        int threadsCount = 10;
        int delayMs = 2000;

        for (int i = 0; i < threadsCount; i++) {
            new SortWorker(list, delayMs, counter).start();
        }

        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter:");

        while (true) {
            String line = scanner.nextLine();

            if (line.isEmpty()) {
                System.out.println("\nCurrent list:");
                for (String s : list) {
                    System.out.println(s);
                }
                System.out.println("Sort step: " + counter.get());
                continue;
            }


            for (int i = 0; i < line.length(); i += 80) {
                String part = line.substring(i, Math.min(i + 80, line.length()));
                list.addFirst(part);
            }
        }
    }
}
