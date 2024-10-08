package dev.httpmarco.polocloud.node.update;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.httpmarco.polocloud.node.util.Downloader;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;

@Log4j2
@Getter
@Accessors(fluent = true)
public final class AutoUpdater {

    private static final String REPO_URL = "https://api.github.com/repos/HttpMarco/polocloud/releases";
    private boolean isInstalled = false;
    private String downloadName;

    public void notifyIfUpdateAvailable() {
        var release = latestRelease();
        if (release == null) {
            return;
        }

        var releaseVersion = releaseVersion(release);
        var currentVersion = System.getProperty("Polocloud-Version");

        if (!VersionVerifier.isNewerVersion(currentVersion, releaseVersion)) {
            return;
        }

        notifyUpdate(release.get("html_url").getAsString(), releaseVersion);
    }

    private void notifyUpdate(String releaseUrl, String releaseVersion) {
        log.warn(" ");
        log.warn("A new version of PoloCloud is available: &bv{}", releaseVersion);
        log.warn("You can download it via the \"node update\" command or from the official repository.");
        log.warn(releaseUrl);
        log.warn(" ");
    }

    private void notifyConfirm() {
        log.warn(" ");
        log.warn("The new version of PoloCloud has been downloaded successfully.");
        log.warn("Please confirm the installation by executing the \"node update confirm\" command.");
        log.warn(" ");
    }

    public void update() {
        var release = latestRelease();
        if (release == null) {
            return;
        }

        var releaseVersion = releaseVersion(release);
        var currentVersion = System.getProperty("Polocloud-Version");

        if (!VersionVerifier.isNewerVersion(currentVersion, releaseVersion)) {
            log.warn("You are already using the latest version of PoloCloud.");
            return;
        }

        var asset = findReleaseAsset(release, releaseVersion);
        if (asset == null) {
            log.warn("No suitable asset found for version: {}", releaseVersion);
            return;
        }

        var downloadUrl = asset.get("browser_download_url").getAsString();
        var downloadName = asset.get("name").getAsString();

        log.info("Downloading new Update...");
        Downloader.download(downloadUrl, Path.of(downloadName).toAbsolutePath());

        this.downloadName = downloadName;
        this.isInstalled = true;

        notifyConfirm();
    }

    private JsonObject findReleaseAsset(JsonObject release, String releaseVersion) {
        var assets = release.get("assets").getAsJsonArray();

        for (int i = 0; i < assets.size(); i++) {
            var asset = assets.get(i).getAsJsonObject();
            var assetName = asset.get("name").getAsString();

            if (assetName.equals("polocloud-" + releaseVersion + ".jar")) {
                return asset;
            }
        }

        return null;
    }

    public String releaseVersion(JsonObject release) {
        return release.get("tag_name").getAsString();
    }

    public JsonObject latestRelease() {
        try {
            var url = new URL(REPO_URL);
            var connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

            if (connection.getResponseCode() != 200) {
                log.error("Failed to fetch a new Update, Github request response code: {}", connection.getResponseCode());
                return null;
            }

            try (var input = connection.getInputStream(); var inputStream = new InputStreamReader(input); var reader = new BufferedReader(inputStream)) {
                var response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                var releases = JsonParser.parseString(response.toString()).getAsJsonArray();
                if (releases.isEmpty()) {
                    return null;
                }

                return releases.get(0).getAsJsonObject();
            }
        } catch (IOException e) {
            log.error("Failed to fetch a new Update");
            e.printStackTrace();
            return null;
        }
    }
}
