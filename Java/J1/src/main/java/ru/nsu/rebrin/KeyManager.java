package ru.nsu.rebrin;

import javax.crypto.*;
import java.security.*;
import java.security.cert.Certificate;

public class KeyManager {
    KeyPair pair;
    Certificate cert;

    public KeyManager(KeyPair pair, Certificate cert) {
        this.pair = pair;
        this.cert = cert;
    }

    public void genPair() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(8192);
        this.pair = generator.generateKeyPair();
    }

    public byte[] eCipherWithKey(String message, PublicKey key)
            throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(message.getBytes());
    }

    public String dCipher(byte[] message)
            throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, this.pair.getPrivate());
        return new String(cipher.doFinal(message));
    }
}
