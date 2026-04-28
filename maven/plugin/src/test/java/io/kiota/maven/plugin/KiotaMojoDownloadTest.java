package io.kiota.maven.plugin;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class KiotaMojoDownloadTest {

    private MockWebServer server;
    private KiotaMojo mojo;

    @TempDir Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();

        mojo = new KiotaMojo();
        mojo.downloadMaxRetries = 2;
        mojo.downloadRetryDelayMs = 10L;
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void downloadAndExtract_retriesOnFailureThenSucceeds() throws Exception {
        byte[] zipBytes = createKiotaZip("kiota");
        server.enqueue(new MockResponse().setResponseCode(503));
        server.enqueue(new MockResponse().setResponseCode(500));
        server.enqueue(new MockResponse().setBody(new Buffer().write(zipBytes)));

        String dest = tempDir.resolve("extract").toString();
        mojo.downloadAndExtract(
                server.url("/kiota.zip").toString(),
                dest,
                new KiotaMojo.KiotaParams("Linux", "amd64"));

        assertTrue(new File(dest, "kiota").exists());
        assertEquals(3, server.getRequestCount());
    }

    @Test
    void downloadAndExtract_exhaustsRetriesWithHttpCodeInError() {
        server.enqueue(new MockResponse().setResponseCode(500));
        server.enqueue(new MockResponse().setResponseCode(500));
        server.enqueue(new MockResponse().setResponseCode(404));

        String dest = tempDir.resolve("extract").toString();
        IllegalStateException ex =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                mojo.downloadAndExtract(
                                        server.url("/kiota.zip").toString(),
                                        dest,
                                        new KiotaMojo.KiotaParams("Linux", "amd64")));

        assertTrue(
                ex.getMessage().contains("3 attempt(s)"),
                "Expected '3 attempt(s)' in: " + ex.getMessage());
        assertTrue(
                ex.getCause().getMessage().contains("HTTP 404"),
                "Expected HTTP 404 in cause: " + ex.getCause().getMessage());
        assertEquals(3, server.getRequestCount());
    }

    @Test
    void downloadFile_noAuthorizationHeaderWhenEnvTokenDisabled() throws Exception {
        mojo.downloadUseTokenFromEnv = false;

        byte[] content = "test content".getBytes();
        server.enqueue(new MockResponse().setBody(new Buffer().write(content)));

        File dest = tempDir.resolve("download.zip").toFile();
        mojo.downloadFile(server.url("/test.zip").toString(), dest);

        assertNull(server.takeRequest().getHeader("Authorization"));
    }

    @Test
    void downloadFile_fileUrl_copiesFile() throws Exception {
        Path source = tempDir.resolve("source.zip");
        byte[] content = "zip-content".getBytes();
        Files.write(source, content);

        File dest = tempDir.resolve("downloaded.zip").toFile();
        mojo.downloadFile(source.toUri().toString(), dest);

        assertArrayEquals(content, Files.readAllBytes(dest.toPath()));
    }

    @Test
    void downloadAndExtract_fileUrlZip() throws Exception {
        byte[] zipBytes = createKiotaZip("kiota");
        Path zipFile = tempDir.resolve("kiota.zip");
        Files.write(zipFile, zipBytes);
        String dest = tempDir.resolve("extract").toString();
        mojo.downloadAndExtract(
                zipFile.toUri().toString(), dest, new KiotaMojo.KiotaParams("Linux", "amd64"));
        assertTrue(new File(dest, "kiota").exists());
    }

    @Test
    void downloadAndExtract_httpUrlViaDownloadUrl() throws Exception {
        byte[] zipBytes = createKiotaZip("kiota");
        server.enqueue(new MockResponse().setBody(new Buffer().write(zipBytes)));

        mojo.downloadURL = server.url("/custom/kiota-v1.zip").toString();

        String dest = tempDir.resolve("extract").toString();
        mojo.downloadAndExtract(
                mojo.downloadURL, dest, new KiotaMojo.KiotaParams("Linux", "amd64"));

        assertTrue(new File(dest, "kiota").exists());
        assertEquals(1, server.getRequestCount());
    }

    @Test
    void downloadAndExtract_nonZipFile_fails() {
        byte[] binaryContent = "#!/bin/sh\necho kiota".getBytes();
        Path binarySource = tempDir.resolve("not-a-zip");
        assertDoesNotThrow(() -> Files.write(binarySource, binaryContent));

        String dest = tempDir.resolve("extract").toString();
        assertThrows(
                IllegalStateException.class,
                () ->
                        mojo.downloadAndExtract(
                                binarySource.toUri().toString(),
                                dest,
                                new KiotaMojo.KiotaParams("Linux", "amd64")));
    }

    private byte[] createKiotaZip(String binaryName) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry(binaryName));
            zos.write("#!/bin/sh\necho kiota".getBytes());
            zos.closeEntry();
        }
        return baos.toByteArray();
    }
}
