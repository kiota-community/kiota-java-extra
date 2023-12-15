package io.kiota.quarkus.deployment;

import io.kiota.quarkus.KiotaCodeGenConfig;
import io.quarkus.bootstrap.prebuild.CodeGenException;
import io.quarkus.deployment.CodeGenContext;
import io.quarkus.deployment.CodeGenProvider;
import io.quarkus.logging.Log;
import io.quarkus.utilities.OS;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.microprofile.config.Config;

/**
 * Code generation for Kiota. Generates java classes from OpenAPI files placed in either src/main/openapi or src/test/openapi
 * Implementation inspired by: https://github.com/quarkusio/quarkus/blob/f0841e02edbc2a1c1fc5b18c8b6cfecadff42a51/extensions/grpc/codegen/src/main/java/io/quarkus/grpc/deployment/GrpcCodeGen.java
 * Kiota does implement a Json RPC protocol, eventually we can use it as a long-running process: https://github.com/microsoft/kiota/blob/main/vscode/microsoft-kiota/src/kiotaInterop.ts
 */
public abstract class KiotaCodeGen implements CodeGenProvider {

    private static final String EXE = "exe";
    private static final String KIOTA = "kiota";

    @Override
    public String providerId() {
        return "kiota";
    }

    @Override
    public abstract String inputExtension();

    @Override
    public String inputDirectory() {
        return "openapi";
    }

    private Stream<Path> findDescriptions(Path sourceDir) {
        try {
            return Files.find(
                            sourceDir,
                            Integer.MAX_VALUE,
                            (filePath, fileAttr) ->
                                    filePath.toString().endsWith(inputExtension())
                                            && fileAttr.isRegularFile(),
                            FileVisitOption.FOLLOW_LINKS)
                    .distinct();
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to visit the folder: " + sourceDir.toFile().getAbsolutePath(), e);
        }
    }

    private String getFolderMd5(Path source) throws CodeGenException {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            Files.find(
                            source,
                            Integer.MAX_VALUE,
                            (filePath, fileAttr) ->
                                    filePath.toString().endsWith(inputExtension())
                                            && fileAttr.isRegularFile())
                    .distinct()
                    .sorted()
                    .forEachOrdered(
                            f -> {
                                try {
                                    digest.update(Files.readAllBytes(f));
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });
            return new String(digest.digest(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new CodeGenException(
                    "Failed to calculate the contentHash of generated sources", e);
        } catch (NoSuchAlgorithmException e) {
            throw new CodeGenException("Cannot find MD5 algorithm", e);
        }
    }

    @Override
    public boolean trigger(CodeGenContext context) throws CodeGenException {
        Log.debug("Running trigger logic.");
        String folderHashBefore = getFolderMd5(context.outDir());

        String executable = KiotaCodeGenConfig.getProvided(context.config());
        if (KiotaCodeGenConfig.getProvided(context.config()) == null) {
            String version = KiotaCodeGenConfig.getVersion(context.config());
            KiotaClassifier classifier =
                    new KiotaClassifier(
                            KiotaCodeGenConfig.getOs(context.config()),
                            KiotaCodeGenConfig.getArch(context.config()));
            URL downloadUrl;
            try {
                downloadUrl =
                        new URL(
                                KiotaCodeGenConfig.getReleaseUrl(context.config())
                                        + "/download/v"
                                        + URLEncoder.encode(version, StandardCharsets.UTF_8)
                                        + "/"
                                        + classifier.downloadArtifactName()
                                        + ".zip");
            } catch (MalformedURLException e) {
                throw new CodeGenException(
                        "Malformed release URL: "
                                + KiotaCodeGenConfig.getReleaseUrl(context.config())
                                + version,
                        e);
            }
            Path destFolder = context.workDir().resolve(KIOTA).resolve(version);

            if (Files.isDirectory(destFolder)) {
                Log.info("Kiota binary folder found, using the cached version.");
            } else {
                downloadAndExtract(downloadUrl, destFolder, classifier);
            }

            executable = destFolder.resolve(classifier.binaryName()).toFile().getAbsolutePath();
        } else {
            if (OS.determineOS() == OS.WINDOWS && !executable.endsWith(EXE)) {
                executable = executable + "." + EXE;
            }
        }

        String finalExecutable = executable;
        for (Path spec : findDescriptions(context.inputDir()).collect(Collectors.toList())) {
            executeKiota(
                    finalExecutable,
                    spec,
                    context.outDir(),
                    context.shouldRedirectIO(),
                    context.config());
            try {
                new FixClientClass(
                                KiotaCodeGenConfig.getClientClassName(
                                        context.config(), spec.toFile().getName()),
                                KiotaCodeGenConfig.getClientPackageName(
                                        context.config(), spec.toFile().getName()),
                                context.outDir())
                        .fix();
            } catch (IOException e) {
                throw new CodeGenException("Failed to fix-up the client class code", e);
            }
        }

        String folderHashAfter = getFolderMd5(context.outDir());
        return !folderHashBefore.equals(folderHashAfter);
    }

    @Override
    public boolean shouldRun(Path sourceDir, Config config) {
        Log.debug("Should run inspecting the source dir: " + sourceDir);
        if (Files.isDirectory(sourceDir)) {
            return findDescriptions(sourceDir).count() > 0;
        }
        return false;
    }

    private void downloadAndExtract(URL url, Path dest, KiotaClassifier kp) {
        try {
            File zipFile = dest.resolve(KIOTA + ".zip").toFile();
            File finalDestination = dest.resolve(kp.binaryName()).toFile();

            if (!finalDestination.exists()) {
                dest.toFile().mkdirs();
                try (FileOutputStream fileOutputStream = new FileOutputStream(zipFile);
                        InputStream stream = url.openStream();
                        ReadableByteChannel readableByteChannel = Channels.newChannel(stream)) {
                    fileOutputStream
                            .getChannel()
                            .transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                }
                try (FileSystem fileSystem =
                        FileSystems.newFileSystem(
                                zipFile.toPath(), this.getClass().getClassLoader())) {
                    Path fileToExtract = fileSystem.getPath("/" + kp.binaryName());
                    Files.copy(fileToExtract, finalDestination.toPath());
                }
                finalDestination.setExecutable(true, false);
                zipFile.delete();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error downloading the Kiota release: " + url, e);
        }
    }

    private void executeKiota(
            String binary, Path openApiSpec, Path outputDir, boolean redirectIO, Config config)
            throws CodeGenException {
        if (!openApiSpec.toFile().exists()) {
            throw new IllegalArgumentException(
                    "Spec file not found on the path: " + openApiSpec.toFile().getAbsolutePath());
        }
        List<String> cmd = new ArrayList<>();
        outputDir.toFile().mkdirs();
        String finalTargetDirectory =
                finalTargetDirectory(outputDir, config, openApiSpec.toFile().getName())
                        .getAbsolutePath();

        cmd.add(binary);
        cmd.add("generate");
        // process command line options
        cmd.add("--openapi");
        cmd.add(openApiSpec.toFile().getAbsolutePath());
        cmd.add("--output");
        cmd.add(finalTargetDirectory);
        cmd.add("--language");
        cmd.add("java");
        cmd.add("--class-name");
        cmd.add(KiotaCodeGenConfig.getClientClassName(config, openApiSpec.toFile().getName()));
        cmd.add("--namespace-name");
        cmd.add(KiotaCodeGenConfig.getClientPackageName(config, openApiSpec.toFile().getName()));
        for (String ser :
                KiotaCodeGenConfig.getSerializer(config, openApiSpec.toFile().getName())) {
            cmd.add("--serializer");
            cmd.add(ser);
        }
        for (String deser :
                KiotaCodeGenConfig.getDeserializer(config, openApiSpec.toFile().getName())) {
            cmd.add("--deserializer");
            cmd.add(deser);
        }
        cmd.add("--clean-output");
        cmd.add("true");
        cmd.add("--clear-cache");
        cmd.add("true");
        cmd.add("--log-level");
        cmd.add("Warning");

        String includePath =
                KiotaCodeGenConfig.getIncludePath(config, openApiSpec.toFile().getName());
        if (includePath != null) {
            cmd.add("--include-path");
            cmd.add(includePath);
        }

        String excludePath =
                KiotaCodeGenConfig.getExcludePath(config, openApiSpec.toFile().getName());
        if (includePath != null) {
            cmd.add("--exclude-path");
            cmd.add(excludePath);
        }

        runProcess(cmd, redirectIO, outputDir, config, openApiSpec.toFile().getName());
    }

    private File finalTargetDirectory(Path outDir, Config config, String filename) {
        Path namespaceResolver = outDir;

        for (String part : KiotaCodeGenConfig.getClientPackageName(config, filename).split("\\.")) {
            namespaceResolver = namespaceResolver.resolve(part);
        }
        return namespaceResolver.toFile();
    }

    private boolean lockFileExists(Path outDir, Config config, String filename) {
        return new File(
                        finalTargetDirectory(outDir, config, filename).getAbsolutePath(),
                        "kiota-lock.json")
                .exists();
    }

    private void runProcess(
            List<String> cmd, boolean redirectIO, Path outputDir, Config config, String filename)
            throws CodeGenException {
        Log.info("Going to execute the command: " + cmd.stream().collect(Collectors.joining(" ")));
        Process ps = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(new File("."));

            if (redirectIO) {
                pb.inheritIO();
            }
            ps = pb.start();
            ps.waitFor(KiotaCodeGenConfig.getTimeout(config), TimeUnit.SECONDS);

            if (ps.exitValue() != 0) {
                throw new CodeGenException(
                        "Error executing the Kiota command, exit code is " + ps.exitValue());
            }
            if (!lockFileExists(outputDir, config, filename)) {
                throw new CodeGenException(
                        "Error executing the Kiota command, no output found, cannot find the"
                                + " generated lock file.");
            }
        } catch (IOException e) {
            throw new CodeGenException("Failed to execute kiota", e);
        } catch (InterruptedException e) {
            throw new CodeGenException("Failed to execute kiota", e);
        } finally {
            if (ps != null) {
                try {
                    ps.getOutputStream().close();
                } catch (IOException e) {
                }
                try {
                    ps.getErrorStream().close();
                } catch (IOException e) {
                }
                try {
                    ps.getInputStream().close();
                } catch (IOException e) {
                }
            }
        }
    }

    private static class KiotaClassifier {
        final OS osName;
        final String osArch;

        public KiotaClassifier(OS osName, String osArch) {
            this.osName = osName;
            this.osArch = osArch;
        }

        public String downloadArtifactName() throws CodeGenException {
            String architecture = osArch;
            if (architecture.equals("x86_64")) {
                architecture = "x64";
            } else if (architecture.equals("aarch64")) {
                architecture = "arm64";
            } else {
                throw new CodeGenException(
                        "Unsupported architecture, please specify a supported architecture using"
                                + " properties.");
            }
            switch (osName) {
                case LINUX:
                    return "linux-" + architecture;
                case WINDOWS:
                    return "win-" + architecture;
                case MAC:
                    return "osx-" + architecture;
                default:
                    throw new CodeGenException(
                            "Unsupported architecture, please specify a supported os using"
                                    + " properties.");
            }
        }

        public String binaryName() {
            switch (OS.determineOS()) {
                case WINDOWS:
                    return KIOTA + "." + EXE;
                default:
                    return KIOTA;
            }
        }
    }
}
