package io.kiota.maven.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * This plugin will run Kiota to generate sources.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class KiotaMojo extends AbstractMojo {
    private final Log log = new SystemStreamLog();

    /**
     * Skip the execution of the goal
     */
    @Parameter(defaultValue = "false", property = "kiota.skip")
    private boolean skip;

    /**
     * Use system provided kiota executable (needs to be available on the PATH)
     */
    @Parameter(defaultValue = "false", property = "kiota.system")
    private boolean useSystemKiota;

    /**
     * Kiota executable target binary folder
     */
    @Parameter(required = true, defaultValue = "${project.build.directory}/kiota/")
    private File targetBinaryFolder;

    /**
     * OS name
     */
    @Parameter(defaultValue = "${os.name}")
    private String osName;

    // Eventually make this configurable
    private String osArch = "x64";

    /**
     * Base URL to be used for the download
     */
    @Parameter(defaultValue = "https://github.com/microsoft/kiota/releases/download")
    private String baseURL;

    /**
     * Version of Kiota to be used
     */
    // TODO: fix me when Kiota 1.9.0 is out
    @Parameter(defaultValue = "1.9.0-preview.202311230001")
    private String kiotaVersion;

    // Kiota Options
    /**
     * The openapi specification to be used for generating code
     */
    @Parameter() private File file;

    /**
     * The URL to be used to download an API spec from a remote location
     */
    @Parameter() URL url;

    /**
     * The Download target folder for CRDs downloaded from remote URLs
     *
     */
    @Parameter(defaultValue = "${basedir}/target/openapi-spec")
    File downloadTarget;

    /**
     * Location where to generate the Java code
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/kiota")
    private File targetDirectory;

    /**
     * The serializers to be used by kiota
     */
    @Parameter(
            defaultValue =
                    "io.kiota.serialization.json.JsonSerializationWriterFactory,com.microsoft.kiota.serialization.TextSerializationWriterFactory,com.microsoft.kiota.serialization.FormSerializationWriterFactory,com.microsoft.kiota.serialization.MultipartSerializationWriterFactory")
    private List<String> serializers;

    /**
     * The deserializers to be used by kiota
     */
    @Parameter(
            defaultValue =
                    "io.kiota.serialization.json.JsonParseNodeFactory,com.microsoft.kiota.serialization.TextParseNodeFactory,com.microsoft.kiota.serialization.FormParseNodeFactory")
    private List<String> deserializers;

    private File finalTargetDirectory() {
        Path namespaceResolver = targetDirectory.toPath();

        for (String part : namespace.split("\\.")) {
            namespaceResolver = namespaceResolver.resolve(part);
        }
        return namespaceResolver.toFile();
    }

    /**
     * Language to generate the code for:
     * <CSharp|Go|Java|PHP|Python|Ruby|Shell|Swift|TypeScript>
     */
    @Parameter(defaultValue = "Java")
    private String language;

    /**
     * The class name to use for the core client class. [default: ApiClient]
     *
     */
    @Parameter(defaultValue = "ApiClient")
    private String clientClass;

    /**
     * The namespace to use for the core client class specified with the --class-name option. [default: ApiSdk]
     *
     */
    @Parameter(defaultValue = "com.apisdk")
    private String namespace;

    /**
     * Clean output before generating
     *
     */
    @Parameter(defaultValue = "false")
    private String cleanOutput;

    /**
     * Clear cache before generating
     *
     */
    @Parameter(defaultValue = "false")
    private String clearCache;

    /**
     * Kiota timeout in seconds
     *
     */
    @Parameter(defaultValue = "30")
    private int kiotaTimeout;

    /**
     * The log level of Kiota to use when logging messages to the main output. [default: Warning]
     * <Critical|Debug|Error|Information|None|Trace|Warning>
     */
    @Parameter(defaultValue = "Warning")
    private String kiotaLogLevel;

    /**
     * The current Maven project.
     */
    @Parameter(property = "project", required = true, readonly = true)
    private MavenProject project;

    // Used only internally
    private static final String NEW_LINE = "\n";
    // TODO, evaluate if we should make this configurable
    private final String infoMatchPrefix = "com.microsoft.kiota:microsoft-kiota-abstractions:";

    @Override
    public void execute() {
        if (skip) {
            return;
        }

        KiotaParams kp = new KiotaParams(osName, osArch);

        String executable = "kiota";
        if (!useSystemKiota) {
            String executablePath =
                    Path.of(targetBinaryFolder.getAbsolutePath(), kiotaVersion)
                            .toFile()
                            .getAbsolutePath();
            downloadAndExtract(
                    baseURL + "/v" + kiotaVersion + "/" + kp.downloadArtifact() + ".zip",
                    executablePath,
                    kp);
            executable = Paths.get(executablePath, kp.binary()).toFile().getAbsolutePath();
        }

        File openApiSpec = null;
        if (file == null && url == null) {
            throw new IllegalArgumentException("Please provide one of a file or a url.");
        } else if (file != null && url != null) {
            throw new IllegalArgumentException("Please provide ONLY one of a file or a url.");
        } else if (file != null) {
            openApiSpec = new File(file.getAbsolutePath());
        } else if (url != null) {
            openApiSpec = downloadSpec(url);
        }

        executeKiota(executable, openApiSpec);
        // TODO: check if the dependency check is still useful
    }

    private File downloadSpec(URL url) {
        downloadTarget.mkdirs();
        final File finalDestination = new File(downloadTarget, new File(url.getFile()).getName());
        if (finalDestination.exists()) {
            log.warn(
                    "Skipping download of "
                            + url
                            + " because it already exists at "
                            + finalDestination);
            return finalDestination;
        }
        try (ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
                FileOutputStream fileOutputStream = new FileOutputStream(finalDestination)) {
            fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            return finalDestination;
        } catch (IOException e) {
            throw new IllegalArgumentException("Error downloading OpenAPI from URL: " + url, e);
        }
    }

    private String runProcess(List<String> cmd, boolean returnOutput) {
        log.info("Going to execute the command: " + cmd.stream().collect(Collectors.joining(" ")));
        Process ps = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(new File("."));

            if (!returnOutput) {
                // TODO: STDERR is not correctly redirected
                pb.inheritIO();
            }
            ps = pb.start();
            ps.waitFor(kiotaTimeout, TimeUnit.SECONDS);

            if (ps.exitValue() != 0) {
                throw new RuntimeException(
                        "Error executing the Kiota command, exit code is " + ps.exitValue());
            }
            File kiotaLockFile = new File(finalTargetDirectory(), "kiota-lock.json");
            if (!kiotaLockFile.exists()) {
                throw new RuntimeException(
                        "Error executing the Kiota command, no output found, cannot find the"
                                + " generated lock file.");
            }

            if (returnOutput) {
                BufferedReader br = new BufferedReader(new InputStreamReader(ps.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line + NEW_LINE);
                String result = sb.toString();
                log.info("Returned output is:\n" + result);
                return result;
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to execute kiota", e);
        } catch (InterruptedException e) {
            throw new RuntimeException("Failed to execute kiota", e);
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

    private void executeKiota(String binary, File openApiSpec) {
        if (!openApiSpec.exists()) {
            throw new IllegalArgumentException(
                    "Spec file not found on the path: " + openApiSpec.getAbsolutePath());
        }
        List<String> cmd = new ArrayList<>();
        finalTargetDirectory().mkdirs();
        String finalTargetDirectory = finalTargetDirectory().getAbsolutePath();

        cmd.add(binary);
        cmd.add("generate");
        // process command line options
        cmd.add("--openapi");
        cmd.add(openApiSpec.getAbsolutePath());
        cmd.add("--output");
        cmd.add(finalTargetDirectory);
        cmd.add("--language");
        cmd.add(language);
        cmd.add("--class-name");
        cmd.add(clientClass);
        cmd.add("--namespace-name");
        cmd.add(namespace);
        for (String serializer : serializers) {
            cmd.add("--serializer");
            cmd.add(serializer);
        }
        for (String deserializer : deserializers) {
            cmd.add("--deserializer");
            cmd.add(deserializer);
        }
        cmd.add("--clean-output");
        cmd.add(cleanOutput);
        cmd.add("--clear-cache");
        cmd.add(clearCache);
        cmd.add("--log-level");
        cmd.add(kiotaLogLevel);

        runProcess(cmd, false);
        project.addCompileSourceRoot(targetDirectory.getAbsolutePath());
    }

    private void downloadAndExtract(String url, String dest, KiotaParams kp) {
        try {
            URL s = new URL(url);
            File zipFile = Paths.get(dest, "kiota.zip").toFile();
            File finalDestination = Paths.get(dest, kp.binary()).toFile();

            if (!finalDestination.exists()) {
                new File(dest).mkdirs();
                ReadableByteChannel readableByteChannel = Channels.newChannel(s.openStream());
                try (FileOutputStream fileOutputStream = new FileOutputStream(zipFile)) {
                    fileOutputStream
                            .getChannel()
                            .transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                }
                try (FileSystem fileSystem =
                        FileSystems.newFileSystem(
                                zipFile.toPath(), this.getClass().getClassLoader())) {
                    Path fileToExtract = fileSystem.getPath("/" + kp.binary());
                    Files.copy(fileToExtract, finalDestination.toPath());
                }
                finalDestination.setExecutable(true, false);
                zipFile.delete();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error downloading the Kiota release: " + url, e);
        }
    }

    private static class KiotaParams {
        final String osName;
        final String osArch;

        public KiotaParams(String osName, String osArch) {
            this.osName = osName.toLowerCase(Locale.ROOT);
            this.osArch = osArch;
        }

        public String downloadArtifact() {
            if (osName.startsWith("mac") || osName.startsWith("osx")) {
                return "osx-x64";
            } else if (osName.startsWith("windows")) {
                return "win-x64";
            } else if (osName.startsWith("linux")) {
                return "linux-x64";
            } else {
                throw new IllegalArgumentException("Detected OS is not supported: " + osName);
            }
        }

        public String binary() {
            if (osName.startsWith("windows")) {
                return "kiota.exe";
            } else {
                return "kiota";
            }
        }
    }
}
