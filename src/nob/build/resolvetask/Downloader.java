/* ======================================
 * File: Downloader.java
 * Date: 2026-07-01
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

import nob.NobException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.Flow;
import java.util.concurrent.CompletionStage;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.ByteBuffer;
import java.net.URI;
import java.time.Duration;

public class Downloader {
    private static final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    static Path downloadToGlobalCache(Dependency dep, String extension, Context ctx) {
        Path path = ctx.globalCache
            .resolve(dep.groupId.replace(".", "/"))
            .resolve(dep.artifactId)
            .resolve(dep.version)
            .resolve(fileName(dep, extension));

        if (Files.isRegularFile(path)) {
            Logger.debug(path.getFileName() + " found in cache.");
            return path;
        }

        Logger.debug("Downloading: " + path.getFileName());

        HttpResponse<Path> response;

        try {
            Files.createDirectories(path.getParent());

            URI fileURI = getURI(dep, extension);
            HttpRequest req = HttpRequest.newBuilder().uri(fileURI).GET().build();
            response = client.send(req, HttpResponse.BodyHandlers.ofFile(path));

        } catch (Exception e) {
            throw new NobException("Failed to send fetch request for " + dep, e);
        }

        int status = response.statusCode();
        if (status == 404) throw new NobException("Dependency not found: " + dep);
        if (status == 429) throw new NobException("Rate limited by Maven Central");
        if (status / 100 != 2) throw new NobException("Failed to fetch " + dep + " status: " + status);

        return path;
    }

    static URI getURI(Dependency dep, String ext) throws URISyntaxException {
        return new URI(String.format("https://repo1.maven.org/maven2/%s/%s/%s/%s", dep.groupId.replace(".", "/"), dep.artifactId, dep.version, fileName(dep, ext)));
    }

    static String fileName(Dependency dep, String ext) {
        return dep.artifactId + "-" + dep.version + ext;
    }
}

class DownloadSubscriber implements HttpResponse.BodySubscriber<Path> {
    String name;
    long totalSize;
    long received;
    HttpResponse.BodySubscriber<Path> downstream;

    public DownloadSubscriber(String name, long totalSize, HttpResponse.BodySubscriber<Path> downstream) {
        this.name = name;
        this.totalSize = totalSize;
        this.downstream = downstream;
    }

    public void onComplete() {
        downstream.onComplete();
    } 

    public void onError(Throwable throwable) {
        downstream.onError(throwable);
    }

    public void onNext(List<ByteBuffer> buffers) {
        received += buffers.stream().mapToLong(ByteBuffer::remaining).sum();
        float pct = received * 100F / totalSize;
        ResolveTask.progress.put(name, pct);
        downstream.onNext(buffers);
    }

    public void onSubscribe(Flow.Subscription subscription) {
        downstream.onSubscribe(subscription);
    }

    public CompletionStage<Path> getBody() {
        return downstream.getBody();
    }
}
