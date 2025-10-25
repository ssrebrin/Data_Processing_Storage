package ru.nsu.rebrin;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;

/**
 *   java Client <host> <port> <name> [--delay N] [--exit-before-read]
 */
public class Client {

    public static void main(String[] args) {
        if (args.length < 3) {
            usageAndExit();
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String name = args[2];

        // parse optional flags
        int delaySeconds = 0;
        boolean exitBeforeRead = false;

        List<String> extra = new LinkedList<>();
        for (int i = 3; i < args.length; i++) extra.add(args[i]);

        for (int i = 0; i < extra.size(); i++) {
            String t = extra.get(i);
            if ("--delay".equals(t) || "-d".equals(t)) {
                if (i + 1 >= extra.size()) {
                    System.err.println("Missing value for --delay");
                    usageAndExit();
                }
                try {
                    delaySeconds = Integer.parseInt(extra.get(++i));
                } catch (NumberFormatException e) {
                    System.err.println("Invalid delay: " + extra.get(i));
                    usageAndExit();
                }
            } else if ("--exit-before-read".equals(t)) {
                exitBeforeRead = true;
            } else {
                System.err.println("Unknown option: " + t);
                usageAndExit();
            }
        }

        try {
            runClient(host, port, name, delaySeconds, exitBeforeRead);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(2);
        }
    }

    private static void usageAndExit() {
        System.out.println("Usage: java KeyClient <host> <port> <name> [--delay N] [--exit-before-read]");
        System.out.println("  --delay N           wait N seconds after sending request before reading response");
        System.out.println("  --exit-before-read  close socket immediately after sending request (simulate crash)");
        System.exit(1);
    }

    private static void runClient(String host, int port, String name, int delaySeconds, boolean exitBeforeRead) throws IOException, InterruptedException {
        System.out.printf("Connecting to %s:%d, requesting name='%s'%n", host, port, name);
        try (Socket socket = new Socket(host, port)) {
            socket.setSoTimeout(0);

            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            byte[] nameBytes = name.getBytes("US-ASCII");
            out.write(nameBytes);
            out.write('\n');
            System.out.println("Sent name");

            if (exitBeforeRead) {
                System.out.println("--exit-before-read set: closing socket and exiting.");
                return;
            }

            if (delaySeconds > 0) {
                System.out.printf("Delaying %d second(s) before reading response...%n", delaySeconds);
                Thread.sleep(delaySeconds * 1000L);
            }

            DataInputStream dis = new DataInputStream(new BufferedInputStream(in));

            int certLen;
            try {
                certLen = dis.readInt();
            } catch (EOFException eof) {
                throw new IOException("Connection closed by server before certificate length received");
            }

            if (certLen < 0) {
                throw new IOException("Server returned negative cert length: " + certLen);
            }

            byte[] certDer = new byte[certLen];
            readFully(dis, certDer, 0, certLen);

            int privLen;
            try {
                privLen = dis.readInt();
            } catch (EOFException eof) {
                throw new IOException("Connection closed by server before private key length received");
            }

            if (privLen < 0) {
                throw new IOException("Server returned negative private key length: " + privLen);
            }

            byte[] privDer = new byte[privLen];
            readFully(dis, privDer, 0, privLen);

            int pubLen;
            try {
                pubLen = dis.readInt();
            } catch (EOFException eof) {
                throw new IOException("Connection closed by server before public key length received");
            }

            if (pubLen < 0) {
                throw new IOException("Server returned negative public key length: " + privLen);
            }

            byte[] pubDer = new byte[privLen];
            readFully(dis, pubDer, 0, pubLen);

            Path certPath = Path.of(name + ".crt");
            Path keyPath = Path.of(name + ".key");

            String certPem = derToPem(certDer, "CERTIFICATE");
            String keyPem = derToPem(privDer, "PRIVATE KEY")
                    + "\n"
                    + derToPem(pubDer, "PUBLIC KEY");

            Files.writeString(certPath, certPem);
            Files.writeString(keyPath, keyPem);

            System.out.println("Saved certificate to: " + certPath.toAbsolutePath());
            System.out.println("Saved keys to: " + keyPath.toAbsolutePath());
        }
    }

    /** read exactly len bytes or throw IOException */
    private static void readFully(InputStream in, byte[] buf, int off, int len) throws IOException {
        int n = 0;
        while (n < len) {
            int r = in.read(buf, off + n, len - n);
            if (r < 0) throw new EOFException("Unexpected EOF while reading data (read " + n + " of " + len + ")");
            n += r;
        }
    }

    private static String derToPem(byte[] der, String type) {
        String b64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der);
        StringBuilder sb = new StringBuilder();
        sb.append("-----BEGIN ").append(type).append("-----\n");
        sb.append(b64).append("\n");
        sb.append("-----END ").append(type).append("-----\n");
        return sb.toString();
    }
}
