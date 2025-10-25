package ru.nsu.rebrin;

import java.security.KeyPair;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;

public class KeyArchive {
    private final BlockingQueue<Request> requestQueue;
    private final BlockingQueue<Send> sendQueue;


    final HashMap<String, ArchiveData> map;

    public KeyArchive(BlockingQueue<Request> requestQueue, BlockingQueue<Send> sendQueue) {
        map = new HashMap<>();
        this.requestQueue = requestQueue;
        this.sendQueue = sendQueue;
    }

    public void SetArchiveData(KeyPair keyPair, Certificate certificate, String name) {
        map.put(name, new ArchiveData(keyPair, certificate, KeyStatus.DONE));
    }

    public void NewData(String name){
        map.put(name, new ArchiveData(null, null, KeyStatus.GENERATING));
    }

    public ArchiveData GetData(String name) throws InterruptedException {
            if (!map.containsKey(name)){
                return null;
            }

            Send snd;

            ArchiveData data = map.get(name);
            return data;
    }
}
