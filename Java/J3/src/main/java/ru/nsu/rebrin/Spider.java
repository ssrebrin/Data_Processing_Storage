package ru.nsu.rebrin;

import java.net.URI;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;

public class Spider {

    private final HttpClient client = HttpClient.newHttpClient();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final Set<String> visited = ConcurrentHashMap.newKeySet();
    private final List<String> messages = Collections.synchronizedList(new ArrayList<>());
    private final String baseUrl;

    public Spider(String host, int port) {
        this.baseUrl = "http://" + host + ":" + port;
    }

    public List<String> run() {
        crawl("/")
                .join();

        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        List<String> result = new ArrayList<>(messages);
        Collections.sort(result);
        return result;
    }

    private CompletableFuture<Void> crawl(String path) {
        if (!visited.add(path)) return CompletableFuture.completedFuture(null);

        return CompletableFuture.supplyAsync(() -> fetchJson(path), executor)
                .thenCompose(json -> {
                    String msg = (String) json.get("message");
                    if (msg != null) messages.add(msg);

                    List<String> successors = (List<String>) json.get("successors");
                    if (successors == null) successors = Collections.emptyList();

                    List<CompletableFuture<Void>> childFutures = new ArrayList<>();
                    for (String next : successors) {
                        if (next != null && !next.isEmpty()) {
                            childFutures.add(crawl(next));
                        }
                    }

                    return CompletableFuture.allOf(childFutures.toArray(new CompletableFuture[0]));
                });
    }

    private Map<String, Object> fetchJson(String path) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + (Objects.equals(path, "/") ? "": "/") + path))
                    .GET()
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) {
                throw new RuntimeException("Bad response: " + resp.statusCode());
            }

            return parseJson(resp.body());

        } catch (Exception e) {
            throw new RuntimeException("Error fetching " + path + ": " + e.getMessage(), e);
        }
    }

    private Map<String, Object> parseJson(String body) {
        Map<String, Object> map = new HashMap<>();

        String msg = body.replaceAll("(?s).*\"message\"\\s*:\\s*\"([^\"]*)\".*", "$1");
        map.put("message", msg);

        List<String> successors = new ArrayList<>();
        String succ = body.replaceAll("(?s).*\"successors\"\\s*:\\s*\\[(.*?)].*", "$1");
        if (!succ.trim().isEmpty()) {
            for (String s : succ.split(",")) {
                String v = s.trim().replace("\"", "");
                if (!v.isEmpty()) successors.add(v);
            }
        }
        map.put("successors", successors);

        return map;
    }

    public static void main(String[] args) {
        String host = "localhost";
        int port = 8080;

        Spider spider = new Spider(host, port);
        List<String> result = spider.run();

        System.out.println("Всего сообщений: " + result.size());
        result.forEach(System.out::println);
    }
}

