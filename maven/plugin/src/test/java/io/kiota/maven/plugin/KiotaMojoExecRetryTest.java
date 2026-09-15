package io.kiota.maven.plugin;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class KiotaMojoExecRetryTest {

    private final KiotaMojo mojo = new KiotaMojo();

    @TempDir Path tempDir;

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void startProcess_retriesUntilBinaryIsExecutable() throws Exception {
        File script = tempDir.resolve("kiota").toFile();
        Files.writeString(script.toPath(), "#!/bin/sh\nexit 0\n");
        Thread makeExecutable =
                new Thread(
                        () -> {
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            script.setExecutable(true);
                        });
        makeExecutable.start();

        Process process = mojo.startProcess(new ProcessBuilder(script.getAbsolutePath()));

        assertEquals(0, process.waitFor());
        makeExecutable.join();
    }

    @Test
    void startProcess_givesUpEventually() {
        ProcessBuilder pb = new ProcessBuilder(tempDir.resolve("missing-kiota").toString());

        assertThrows(IOException.class, () -> mojo.startProcess(pb));
    }
}
