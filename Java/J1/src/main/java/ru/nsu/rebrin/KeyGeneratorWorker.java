package ru.nsu.rebrin;

import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.security.cert.X509Certificate;


import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.ContentSigner;

public class KeyGeneratorWorker implements Runnable {
    private final BlockingQueue<Request> requestQueue;
    private final BlockingQueue<Send> sendQueue;
    private static PrivateKey caPrivateKey;
    private static PublicKey caPublicKey;
    private static X500Name caSubject = new X500Name("CN=MyCA,O=Example,C=RU");
    private static SecureRandom random;

    public KeyGeneratorWorker(BlockingQueue<Request> requestQueue, BlockingQueue<Send> sendQueue, PrivateKey key, SecureRandom random, PublicKey caPublicKey) {
        this.requestQueue = requestQueue;
        this.sendQueue = sendQueue;
        this.caPrivateKey = key;
        this.caPublicKey = caPublicKey;
        this.random = random;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Request req = requestQueue.take();
                System.out.println(Thread.currentThread().getName() + " generating key for: " + req.getName());

                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(8192);
                KeyPair pair = generator.generateKeyPair();

                Certificate cert = signCertificateStub(pair);


                sendQueue.put(new Send(req.getName(), pair, cert));
                System.out.println("Key are generated");


            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
    }

    public static Certificate signCertificateStub(KeyPair clientPair) {
        try {
            System.out.println("Certificate signing");
            X500Name subject = new X500Name("CN=Client,O=Example,C=RU");

            Date notBefore = new Date(System.currentTimeMillis() - 1000L * 60);
            Date notAfter = new Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000);

            BigInteger serial = new BigInteger(64, random);


            SubjectPublicKeyInfo subjectPublicKeyInfo =
                    SubjectPublicKeyInfo.getInstance(clientPair.getPublic().getEncoded());
            X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
                    caSubject,
                    serial,
                    notBefore, notAfter,
                    subject,
                    subjectPublicKeyInfo
            );

            ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                    .build(caPrivateKey);

            X509Certificate cert = new JcaX509CertificateConverter()
                    .setProvider("BC")
                    .getCertificate(certBuilder.build(signer));

            cert.verify(caPublicKey);
            return cert;

        } catch (Exception e) {
            throw new RuntimeException("Ошибка при создании сертификата", e);
        }
    }
}
