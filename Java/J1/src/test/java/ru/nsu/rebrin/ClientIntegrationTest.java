package ru.nsu.rebrin;

import org.junit.Test;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.Assert.*;

public class ClientIntegrationTest {

    private static final String HOST = "localhost";
    private static final int PORT = 9090;

    private Process startClient(String name, String... extraArgs) throws IOException {
        List<String> args = new ArrayList<>();
        args.add("java");
        args.add("-cp");
        args.add(System.getProperty("java.class.path"));
        args.add("ru.nsu.rebrin.Client");
        args.add(HOST);
        args.add(String.valueOf(PORT));
        args.add(name);
        args.addAll(Arrays.asList(extraArgs));

        ProcessBuilder pb = new ProcessBuilder(args);
        pb.redirectErrorStream(true);
        return pb.start();
    }

    private String readOutput(Process process, int timeoutSec) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder sb = new StringBuilder();

        long endTime = System.currentTimeMillis() + timeoutSec * 1000L;
        while (System.currentTimeMillis() < endTime) {
            while (reader.ready()) {
                String line = reader.readLine();
                if (line != null) {
                    sb.append(line).append("\n");
                }
            }
            try {
                int exitValue = process.exitValue();
                break;
            } catch (IllegalThreadStateException e) {
            }
            Thread.sleep(100);
        }

        try {
            process.exitValue();
        } catch (IllegalThreadStateException e) {
            process.destroy();
            throw new AssertionError("Client timed out");
        }

        return sb.toString();
    }


    @Test
    public void testSingleClient() throws Exception {
        Process client = startClient("User1");
        String output = readOutput(client, 30);
        assertTrue(output, output.contains("Saved certificate"));
        Thread.sleep(2000);
    }

    @Test
    public void testMultiSameNameClients() throws Exception {

        Process c1 = startClient("Aboba");
        Process c2 = startClient("Aboba");
        Process c3 = startClient("Aboba");

        String out1 = readOutput(c1, 30);
        String out2 = readOutput(c2, 30);
        String out3 = readOutput(c3, 30);

        assertTrue(out1.contains("Saved certificate"));
        assertTrue(out2.contains("Saved certificate"));
        assertTrue(out3.contains("Saved certificate"));
    }

    @Test
    public void testClientWithDelay() throws Exception {
        Process client = startClient("User2", "--delay", "3");
        String output = readOutput(client, 30);
        assertTrue(output, output.contains("Saved certificate"));
    }

    @Test
    public void testExitBeforeRead() throws Exception {
        Process client = startClient("User3", "--exit-before-read");
        String output = readOutput(client, 30);
        assertTrue(output, output.contains("exiting"));
    }

    @Test
    public void testMultipleClients() throws Exception {
        Process c1 = startClient("Alice");
        Process c2 = startClient("Bob", "--delay", "2");
        Process c3 = startClient("Charlie", "--exit-before-read");

        String out1 = readOutput(c1, 30);
        String out2 = readOutput(c2, 30);
        String out3 = readOutput(c3, 30);

        assertTrue(out1.contains("Saved certificate"));
        assertTrue(out2.contains("Saved certificate"));
        assertTrue(out3.contains("exit"));
    }

    @Test
    public void testMultiSlowClients() throws Exception {

        Process c1 = startClient("Ab", "--delay", "2");
        Process c2 = startClient("Abo", "--delay", "2");
        Process c3 = startClient("Abob", "--delay", "2");

        String out1 = readOutput(c1, 30);
        String out2 = readOutput(c2, 30);
        String out3 = readOutput(c3, 30);

        assertTrue(out1.contains("Saved certificate"));
        assertTrue(out2.contains("Saved certificate"));
        assertTrue(out3.contains("Saved certificate"));
    }
}


