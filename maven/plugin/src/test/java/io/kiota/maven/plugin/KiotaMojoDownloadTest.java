package io.kiota.maven.plugin;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
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
        File source = tempDir.resolve("source.zip").toFile();
        Files.writeString(source.toPath(), "zip-content");

        File dest = tempDir.resolve("dest.zip").toFile();
        mojo.downloadFile(source.toURI().toString(), dest);

        assertEquals("zip-content", Files.readString(dest.toPath()));
    }

    @Test
    void downloadAndExtract_fileUrlZip() throws Exception {
        byte[] zipBytes = createKiotaZip("kiota");
        File zipFile = tempDir.resolve("kiota-local.zip").toFile();
        try (FileOutputStream fos = new FileOutputStream(zipFile)) {
            fos.write(zipBytes);
        }

        String dest = tempDir.resolve("extract-file").toString();
        mojo.downloadAndExtract(
                zipFile.toURI().toString(),
                dest,
                new KiotaMojo.KiotaParams("Linux", "amd64"));

        assertTrue(new File(dest, "kiota").exists());
    }

    @Test
    void downloadAndExtract_directHttpZipUrl() throws Exception {
        byte[] zipBytes = createKiotaZip("kiota");
        server.enqueue(new MockResponse().setBody(new Buffer().write(zipBytes)));

        String dest = tempDir.resolve("extract-direct").toString();
        mojo.downloadAndExtract(
                server.url("/custom-path/kiota-linux-x64.zip").toString(),
                dest,
                new KiotaMojo.KiotaParams("Linux", "amd64"));

        assertTrue(new File(dest, "kiota").exists());
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
