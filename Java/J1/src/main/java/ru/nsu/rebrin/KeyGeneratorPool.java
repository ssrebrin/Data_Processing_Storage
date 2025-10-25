package ru.nsu.rebrin;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KeyGeneratorPool {
    private final ExecutorService executor;

    public KeyGeneratorPool(int threadCount, Queues queues, PublicKey publicKey, PrivateKey privateKey) {
        this.executor = Executors.newFixedThreadPool(threadCount);
        SecureRandom RANDOM = new SecureRandom();
        for (int i = 0; i < threadCount; i++) {
            executor.submit(new KeyGeneratorWorker(
                    queues.getRequestQueue(),
                    queues.getSendQueue(),
                    privateKey,
                    RANDOM,
                    publicKey
            ));
        }
    }

}
