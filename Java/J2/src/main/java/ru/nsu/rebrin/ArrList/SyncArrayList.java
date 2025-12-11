package ru.nsu.rebrin.ArrList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SyncArrayList {
    private final List<String> list = Collections.synchronizedList(new ArrayList<>());

    public void addFirst(String value) {
        synchronized (list) {
            list.add(0, value);
        }
    }

    public int size() {
        synchronized (list) {
            return list.size();
        }
    }

    public String get(int index) {
        synchronized (list) {
            return list.get(index);
        }
    }

    public void swap(int i, int j) {
        synchronized (list) {
            String temp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, temp);
        }
    }

    public List<String> snapshot() {
        synchronized (list) {
            return new ArrayList<>(list);
        }
    }
}

