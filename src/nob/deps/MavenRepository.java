/* ======================================
 * File: MavenRepository.java
 * Date: 2026-06-10
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.deps;

import nob.NobException;
import nob.build.Context;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.file.Files;
import java.nio.file.Path;
import nob.build.Logger;

public class MavenRepository {
    private static final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public static Path fetchPom(Dependency dep, Context ctx) {
        Path pomPath = ctx.globalCache
            .resolve(dep.groupId.replace(".", "/"))
            .resolve(dep.artifactId)
            .resolve(dep.version)
            .resolve(fileName(dep, ".pom"));

        if (Files.isRegularFile(pomPath)) {
            Logger.debug("POM " + pomPath.getFileName() + " found in cache.");
            return pomPath;
        }

        Logger.debug("Downloading: " + pomPath.getFileName());
        downloadFile(pomPath, dep, ".pom");

        return pomPath;
    }

    public static Path downloadJar(Dependency dep, Context ctx) {
        Path jarPath = ctx.globalCache
            .resolve(dep.groupId.replace(".", "/"))
            .resolve(dep.artifactId)
            .resolve(dep.version)
            .resolve(fileName(dep, ".jar"));

        if (Files.isRegularFile(jarPath)) {
            Logger.debug("Jar " + jarPath.getFileName() + " found in cache.");
            return jarPath;
        }
        
        Logger.debug("Downloading: " + jarPath.getFileName());
        downloadFile(jarPath, dep, ".jar");

        return jarPath;
    }

    private static void downloadFile(Path filePath, Dependency dep, String ext) {
        HttpResponse<Path> response;

        try {
            Files.createDirectories(filePath.getParent());

            URI fileURI = getURI(dep, ext);
            HttpRequest req = HttpRequest.newBuilder().uri(fileURI).GET().build();
            response = client.send(req, HttpResponse.BodyHandlers.ofFile(filePath));

        } catch (Exception e) {
            throw new NobException("Failed to send fetch request for " + dep, e);
        }

        int status = response.statusCode();
        if (status == 404) throw new NobException("Dependency not found: " + dep);
        if (status == 429) throw new NobException("Rate limited by Maven Central");
        if (status / 100 != 2) throw new NobException("Failed to fetch " + dep + " status: " + status);
    }

    private static URI getURI(Dependency dep, String ext) throws URISyntaxException {
        return new URI(String.format("https://repo1.maven.org/maven2/%s/%s/%s/%s", dep.groupId.replace(".", "/"), dep.artifactId, dep.version, fileName(dep, ext)));
    }

    private static String fileName(Dependency dep, String ext) {
        return dep.artifactId + "-" + dep.version + ext;
    }
}

