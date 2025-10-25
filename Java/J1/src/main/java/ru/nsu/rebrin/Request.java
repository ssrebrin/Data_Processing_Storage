package ru.nsu.rebrin;

import java.util.List;

public class Request {
    private final String name;        // имя для ключа

    public Request(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
