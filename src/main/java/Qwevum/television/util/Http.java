package Qwevum.television.util;

import net.dv8tion.jda.api.utils.data.DataObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public final class Http {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private Http() {}

    /** Fetches a URL and parses the body as JSON. Fails if the status isn't 2xx. */
    public static CompletableFuture<DataObject> getJson(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "Television Discord Bot")
                .header("Accept", "application/json")
                .build();

        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply((response) -> {
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("HTTP " + response.statusCode() + " from " + url);
            }

            return DataObject.fromJson(response.body());
        });
    }
}
