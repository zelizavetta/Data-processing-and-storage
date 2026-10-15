package spider;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class Spider {

    private static final int MAX_TRIES = 3;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final String host;
    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final Gson gson = new Gson();

    private final Set<String> seenPaths = Collections.synchronizedSet(new HashSet<>());
    private final Map<String, String> collected = Collections.synchronizedMap(new HashMap<>());
    private final AtomicInteger activeTasks = new AtomicInteger();
    private final CountDownLatch finished = new CountDownLatch(1);

    private ExecutorService workers;

    public Spider(int port) {
        this.host = "http://localhost:" + port;
    }

    public List<String> run() throws InterruptedException {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            workers = executor;
            schedule("/");
            finished.await();
        }

        List<String> texts = new ArrayList<>(collected.values());
        Collections.sort(texts);
        return texts;
    }

    private void schedule(String path) {
        if (!seenPaths.add(path)) {
            return;
        }
        activeTasks.incrementAndGet();
        workers.submit(() -> process(path));
    }

    private void process(String path) {
        try {
            PageInfo page = download(path);
            collected.put(path, page.message());
            for (String link : page.successors()) {
                schedule(link.startsWith("/") ? link : "/" + link);
            }
        } catch (IOException | RuntimeException e) {
            System.err.println(path + ": " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (activeTasks.decrementAndGet() == 0) {
                finished.countDown();
            }
        }
    }

    private PageInfo download(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(host + path))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();

        int attempt = 1;
        while (true) {
            try {
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return gson.fromJson(response.body(), PageInfo.class);
                }
                throw new IOException("сервер вернул код " + response.statusCode());
            } catch (IOException e) {
                if (attempt == MAX_TRIES) {
                    throw e;
                }
                attempt++;
            }
        }
    }
}
