package ru.nsu.rebrin;

import java.security.KeyPair;
import java.security.cert.Certificate;

public class ArchiveData {
    KeyPair keyPair;
    Certificate certificate;
    KeyStatus status;

    public ArchiveData(KeyPair keyPair, Certificate certificate, KeyStatus status) {
        this.keyPair = keyPair;
        this.certificate = certificate;
        this.status = status;
    }

}
