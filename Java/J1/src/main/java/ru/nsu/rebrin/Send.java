package ru.nsu.rebrin;

import java.security.KeyPair;
import java.security.cert.Certificate;

public class Send {
    private final String name;
    private final KeyPair keyPair;
    private final Certificate certificate;

    public Send(String name, KeyPair keyPair, Certificate certificate) {
        this.name = name;
        this.keyPair = keyPair;
        this.certificate = certificate;
    }

    public String getName() {
        return name;
    }
    public KeyPair getKeyPair() {
        return keyPair;
    }
    public Certificate getCertificate() {
        return certificate;
    }
}
