package ru.nsu.rebrin;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Queues {
    private final BlockingQueue<Request> requestQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<Send> sendQueue = new LinkedBlockingQueue<>();

    public BlockingQueue<Request> getRequestQueue() {
        return requestQueue;
    }
    public BlockingQueue<Send> getSendQueue() {return sendQueue;}
}
