package ru.nsu.rebrin;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;

public class SelectServer {
    //Usage: java KeyServer <ca_private_key_file> [issuerName] [--threads N]
    public static void main(String[] args) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        String caKeyFile = "ca_private.key";
        String issuerName = "CN=MyCA,O=ExampleCA,C=RU";
        int threads = 8;

        // Разбираем аргументы
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--ca-key":
                    if (i + 1 < args.length) {
                        caKeyFile = args[++i];
                    } else {
                        System.err.println("Missing value for --ca-key");
                        return;
                    }
                    break;
                case "--issuer":
                    if (i + 1 < args.length) {
                        issuerName = args[++i];
                    } else {
                        System.err.println("Missing value for --issuer");
                        return;
                    }
                    break;
                case "--threads":
                    if (i + 1 < args.length) {
                        try {
                            threads = Integer.parseInt(args[++i]);
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid number for --threads");
                            return;
                        }
                    } else {
                        System.err.println("Missing value for --threads");
                        return;
                    }
                    break;
                default:
                    System.err.println("Unknown argument: " + args[i]);
                    return;
            }
        }

        System.out.println("Using CA key file: " + caKeyFile);
        System.out.println("Issuer: " + issuerName);
        System.out.println("Threads: " + threads);

        KeyPair caPair = new File(caKeyFile).exists() ? loadKeyPair(caKeyFile) : generateAndSaveKeyPair(caKeyFile);
        PrivateKey caPrivateKey = caPair.getPrivate();
        PublicKey caPublicKey = caPair.getPublic();

        System.out.println("CA key ready!");

        Queues queues = new Queues();
        KeyGeneratorPool pool = new KeyGeneratorPool(threads, queues, caPublicKey, caPrivateKey);
        KeyArchive archive = new KeyArchive(queues.getRequestQueue(), queues.getSendQueue());

        Selector selector = Selector.open();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(9090));
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("Server waiting for connections...");

        while (true) {
            selector.select(100);

            Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                iter.remove();

                if (key.isAcceptable()) {
                    ServerSocketChannel srv = (ServerSocketChannel) key.channel();
                    SocketChannel client = srv.accept();
                    client.configureBlocking(false);
                    client.register(selector, SelectionKey.OP_READ, new ClientState(client));
                    System.out.println("Client connected: " + client.getRemoteAddress());
                } else if (key.isReadable()) {
                    SocketChannel client = (SocketChannel) key.channel();
                    ClientState state = (ClientState) key.attachment();
                    if (!state.readed) {
                        try {
                            int read = client.read(state.readBuffer);
                            if (read == -1) {
                                state.nameBytes.reset();
                                state.clientName = "";
                                client.close();
                                continue;
                            }
                            state.readBuffer.flip();
                            while (state.readBuffer.hasRemaining()) {
                                byte b = state.readBuffer.get();
                                if (b == '\n') {
                                    state.readBuffer.compact();
                                    ArchiveData data = handleClientName(state, queues.getRequestQueue(), archive);
                                    if (data != null && data.status == KeyStatus.DONE) {
                                        try {
                                            byte[] certDer = data.certificate.getEncoded();
                                            byte[] privDer = data.keyPair.getPrivate().getEncoded();
                                            byte[] pubDer = data.keyPair.getPublic().getEncoded();
                                            state.writeBuffer.put(intAndBytes(certDer));
                                            state.writeBuffer.put(intAndBytes(privDer));
                                            state.writeBuffer.put(intAndBytes(pubDer));
                                            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    state.readed=true;
                                    break;
                                } else {
                                    state.nameBytes.write(b);
                                }
                            }
                            state.readBuffer.compact();
                        } catch (IOException e) {
                            key.cancel();
                            client.close();
                            System.err.println("Client disconnected: " + e.getMessage());
                        }
                    }
                } else if (key.isWritable()) {
                    SocketChannel client = (SocketChannel) key.channel();
                    ClientState state = (ClientState) key.attachment();
                    try {
                        state.writeBuffer.flip();
                        client.write(state.writeBuffer);
                        state.writeBuffer.compact();
                        if (state.writeBuffer.position() == 0) {
                            key.interestOps(SelectionKey.OP_READ); // выключаем OP_WRITE
                        }
                    } catch (IOException e) {
                        key.cancel();
                        client.close();
                    }
                }
            }

            List<Send> ready = new ArrayList<>();
            queues.getSendQueue().drainTo(ready);
            for (Send data : ready) {
                for (SelectionKey key : selector.keys()) {
                    if (key.channel() instanceof SocketChannel) {
                        ClientState state = (ClientState) key.attachment();
                        if (state != null && state.clientName.equals(data.getName())) {
                            try {
                                byte[] certDer = data.getCertificate().getEncoded();
                                byte[] privDer = data.getKeyPair().getPrivate().getEncoded();
                                byte[] pubDer = data.getKeyPair().getPublic().getEncoded();
                                state.writeBuffer.put(intAndBytes(certDer));
                                state.writeBuffer.put(intAndBytes(privDer));
                                state.writeBuffer.put(intAndBytes(pubDer));
                                key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                archive.SetArchiveData(data.getKeyPair(), data.getCertificate(), data.getName());
            }
        }
    }

    private static byte[] intAndBytes(byte[] arr) {
        ByteBuffer buf = ByteBuffer.allocate(4 + arr.length);
        buf.putInt(arr.length);
        buf.put(arr);
        return buf.array();
    }

    private static ArchiveData handleClientName(ClientState state, BlockingQueue<Request> requestQueue, KeyArchive archive) throws IOException {
        state.clientName = state.nameBytes.toString();
        state.nameBytes.reset();

        ArchiveData data = null;
        try {
            data = archive.GetData(state.clientName);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (data == null) {
            archive.NewData(state.clientName);
            requestQueue.offer(new Request(state.clientName));
        }
        return data;
    }

    private static KeyPair generateAndSaveKeyPair(String filePath) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "BC");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        savePrivateKeyToPEM(pair.getPrivate(), filePath);
        return pair;
    }

    private static void savePrivateKeyToPEM(PrivateKey privateKey, String filePath) throws IOException {
        String encoded = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write("-----BEGIN PRIVATE KEY-----\n");
            for (int i = 0; i < encoded.length(); i += 64) {
                writer.write(encoded, i, Math.min(64, encoded.length() - i));
                writer.write("\n");
            }
            writer.write("-----END PRIVATE KEY-----\n");
        }
    }

    public static KeyPair loadKeyPair(String filePath) throws Exception {
        String pem = Files.readString(Path.of(filePath))
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] keyBytes = Base64.getDecoder().decode(pem);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
        RSAPrivateCrtKey priv = (RSAPrivateCrtKey) privateKey;
        RSAPublicKeySpec pubSpec = new RSAPublicKeySpec(priv.getModulus(), priv.getPublicExponent());
        PublicKey publicKey = keyFactory.generatePublic(pubSpec);

        return new KeyPair(publicKey, privateKey);
    }

    static class ClientState {
        final SocketChannel channel;
        final ByteBuffer readBuffer = ByteBuffer.allocate(4096);
        final ByteBuffer writeBuffer = ByteBuffer.allocate(8192);
        final ByteArrayOutputStream nameBytes = new ByteArrayOutputStream();
        String clientName = "";
        boolean readed = false;

        ClientState(SocketChannel ch) {
            this.channel = ch;
        }
    }
}
