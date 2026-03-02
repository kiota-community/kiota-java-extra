package io.kiota.maven.plugin;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
        setField(mojo, "downloadMaxRetries", 2);
        setField(mojo, "downloadRetryDelayMs", 10L);
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
        invokeDownloadAndExtract(server.url("/kiota.zip").toString(), dest, "Linux", "amd64");

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
                                invokeDownloadAndExtract(
                                        server.url("/kiota.zip").toString(),
                                        dest,
                                        "Linux",
                                        "amd64"));

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
        setField(mojo, "downloadUseTokenFromEnv", false);

        byte[] content = "test content".getBytes();
        server.enqueue(new MockResponse().setBody(new Buffer().write(content)));

        File dest = tempDir.resolve("download.zip").toFile();
        invokeDownloadFile(server.url("/test.zip").toString(), dest);

        assertNull(server.takeRequest().getHeader("Authorization"));
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

    private void setField(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    private void invokeDownloadFile(String url, File destination) throws Exception {
        Method method = KiotaMojo.class.getDeclaredMethod("downloadFile", String.class, File.class);
        method.setAccessible(true);
        try {
            method.invoke(mojo, url, destination);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw e;
        }
    }

    private void invokeDownloadAndExtract(String url, String dest, String os, String arch)
            throws Exception {
        Class<?> kpClass = Class.forName("io.kiota.maven.plugin.KiotaMojo$KiotaParams");
        Constructor<?> ctor = kpClass.getDeclaredConstructor(String.class, String.class);
        ctor.setAccessible(true);
        Object kp = ctor.newInstance(os, arch);

        Method method =
                KiotaMojo.class.getDeclaredMethod(
                        "downloadAndExtract", String.class, String.class, kpClass);
        method.setAccessible(true);
        try {
            method.invoke(mojo, url, dest, kp);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof IllegalStateException) {
                throw (IllegalStateException) e.getCause();
            }
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw e;
        }
    }
}
