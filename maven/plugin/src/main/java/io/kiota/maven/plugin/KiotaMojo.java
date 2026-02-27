package io.kiota.maven.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
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

    /**
     * OS arch
     */
    @Parameter(defaultValue = "${os.arch}")
    private String osArch;

    /**
     * Base URL to be used for the download
     */
    @Parameter(
            defaultValue = "https://github.com/microsoft/kiota/releases/download",
            property = "kiota.baseURL")
    private String baseURL;

    /**
     * Version of Kiota to be used
     */
    @Parameter(defaultValue = "1.22.2")
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

    /**
     * The includePath to be used by kiota
     */
    @Parameter() private List<String> includePath;

    /**
     * The excludePath to be used by kiota
     */
    @Parameter() private List<String> excludePath;

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
     * The namespace to use for the core client class specified with the --class-name option.
     * [default: ApiSdk]
     *
     */
    @Parameter(defaultValue = "com.apisdk")
    private String namespace;

    /**
     * The type access modifier to use for the client types, specified with --type-access-modifier.
     * [default: Public]
     */
    @Parameter(defaultValue = "Public")
    private KiotaAccessModifier typeAccessModifier;

    /**
     * Enables backing store for models, specified with --backing-store. [default: False]
     */
    @Parameter(defaultValue = "false")
    private boolean backingStore;

    /**
     * Excludes backward compatible and obsolete assets from the generated result. Should be used
     * for new clients. Specified with --exclude-backward-compatible. [default: False]
     */
    @Parameter(defaultValue = "false")
    private boolean excludeBackwardCompatible;

    /**
     * Will include the 'AdditionalData' property for models. Specified with --additional-data.
     * [default: True]
     */
    @Parameter(defaultValue = "true")
    private boolean additionalData;

    /**
     * The MIME types with optional priorities as defined in RFC9110 Accept header
     * to use for structured data model generation. Accepts multiple values.
     * Specified with --structured-mime-types. [default:
     * application/json|text/plain;q=0.9|application/x-www-form-urlencoded;q=0.2|
     * multipart/form-data;q=0.1]
     */
    @Parameter() private List<String> structuredMimeTypes;

    /**
     * The OpenAPI description validation rules to disable. Accepts multiple values.
     * Specified with --disable-validation-rules. [default: none]
     */
    @Parameter() private List<KiotaValidationRule> disableValidationRules;

    /**
     * Disables SSL certificate validation. Specified with --disable-ssl-validation.
     * [default: False]
     */
    @Parameter(defaultValue = "false")
    private boolean disableSslValidation;

    /**
     * Removes all files from the output directory before generating the code files. [default: False]
     *
     */
    @Parameter(defaultValue = "false")
    private boolean cleanOutput;

    /**
     * Clears any cached data for the current command. [default: False]
     *
     */
    @Parameter(defaultValue = "false")
    private boolean clearCache;

    /**
     * Kiota timeout in seconds
     *
     */
    @Parameter(defaultValue = "30")
    private int kiotaTimeout;

    /**
     * Maximum number of retry attempts for downloading the Kiota binary.
     */
    @Parameter(defaultValue = "3", property = "kiota.download.maxRetries")
    private int downloadMaxRetries;

    /**
     * Initial delay in milliseconds between download retry attempts.
     * The delay doubles with each subsequent retry (exponential backoff).
     */
    @Parameter(defaultValue = "1000", property = "kiota.download.retryDelayMs")
    private long downloadRetryDelayMs;

    /**
     * The log level of Kiota to use when logging messages to the main output. [default: Warning]
     * <Critical|Debug|Error|Information|None|Trace|Warning>
     */
    @Parameter(defaultValue = "Warning")
    private KiotaLogLevel kiotaLogLevel;

    /**
     * Any additional arguments that need to be passed to Kiota. [default: none]
     */
    @Parameter() private List<String> extraArguments;

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
        cmd.add("--type-access-modifier");
        cmd.add(typeAccessModifier.name());
        for (String serializer : serializers) {
            cmd.add("--serializer");
            cmd.add(serializer);
        }
        for (String deserializer : deserializers) {
            cmd.add("--deserializer");
            cmd.add(deserializer);
        }
        if (includePath != null) {
            for (String i : includePath) {
                cmd.add("--include-path");
                cmd.add(i);
            }
        }
        if (excludePath != null) {
            for (String e : excludePath) {
                cmd.add("--exclude-path");
                cmd.add(e);
            }
        }
        if (backingStore) {
            cmd.add("--backing-store");
        }
        if (excludeBackwardCompatible) {
            cmd.add("--exclude-backward-compatible");
        }
        if (additionalData) {
            cmd.add("--additional-data");
        }
        if (structuredMimeTypes != null) {
            for (String s : structuredMimeTypes) {
                cmd.add("--structured-mime-types");
                cmd.add(s);
            }
        }
        if (disableValidationRules != null) {
            for (KiotaValidationRule r : disableValidationRules) {
                cmd.add("--disable-validation-rules");
                cmd.add(r.name());
            }
        }
        if (disableSslValidation) {
            cmd.add("--disable-ssl-validation");
        }
        if (cleanOutput) {
            cmd.add("--clean-output");
        }
        if (clearCache) {
            cmd.add("--clear-cache");
        }
        cmd.add("--log-level");
        cmd.add(kiotaLogLevel.name());
        if (extraArguments != null) {
            cmd.addAll(extraArguments);
        }

        runProcess(cmd, false);
        project.addCompileSourceRoot(targetDirectory.getAbsolutePath());
    }

    private void downloadAndExtract(String url, String dest, KiotaParams kp) {
        File zipFile = Paths.get(dest, "kiota.zip").toFile();
        File finalDestination = Paths.get(dest, kp.binary()).toFile();

        if (finalDestination.exists()) {
            return;
        }

        new File(dest).mkdirs();

        IOException lastException = null;
        int maxAttempts = downloadMaxRetries + 1;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                downloadFile(url, zipFile);
                try (FileSystem fileSystem =
                        FileSystems.newFileSystem(
                                zipFile.toPath(), this.getClass().getClassLoader())) {
                    Path fileToExtract = fileSystem.getPath("/" + kp.binary());
                    Files.copy(fileToExtract, finalDestination.toPath());
                }
                finalDestination.setExecutable(true, false);
                zipFile.delete();
                return;
            } catch (IOException e) {
                lastException = e;
                // Clean up partial downloads
                zipFile.delete();
                finalDestination.delete();

                if (attempt < maxAttempts) {
                    long delay = Math.min(downloadRetryDelayMs * (1L << (attempt - 1)), 256_000L);
                    log.warn(
                            "Download attempt "
                                    + attempt
                                    + " of "
                                    + maxAttempts
                                    + " failed: "
                                    + e.getMessage()
                                    + ". Retrying in "
                                    + delay
                                    + "ms ...");
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(
                                "Download interrupted while retrying: " + url, e);
                    }
                }
            }
        }
        throw new IllegalStateException(
                "Error downloading the Kiota release after " + maxAttempts + " attempt(s): " + url,
                lastException);
    }

    private void downloadFile(String url, File destination) throws IOException {
        URL s = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) s.openConnection();
        connection.setInstanceFollowRedirects(true);
        try {
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException(
                        "HTTP "
                                + responseCode
                                + " "
                                + connection.getResponseMessage()
                                + " when downloading "
                                + url);
            }
            try (InputStream inputStream = connection.getInputStream();
                    ReadableByteChannel readableByteChannel = Channels.newChannel(inputStream);
                    FileOutputStream fileOutputStream = new FileOutputStream(destination)) {
                fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
            }
        } finally {
            connection.disconnect();
        }
    }

    private enum Os {
        LINUX("linux", "kiota"),
        OSX("osx", "kiota"),
        WINDOWS("win", "kiota.exe");

        private final String osString;
        private final String binary;

        Os(String osString, String binary) {
            this.osString = osString;
            this.binary = binary;
        }

        public static Os parse(String rawOsString) {
            rawOsString = rawOsString.toLowerCase(Locale.ROOT);
            if (rawOsString.startsWith("linux")) {
                return LINUX;
            }
            if (rawOsString.startsWith("osx") || rawOsString.startsWith("mac")) {
                return OSX;
            }
            if (rawOsString.startsWith("win")) {
                return WINDOWS;
            }
            throw new IllegalArgumentException(
                    "Detected OS is not recognized or supported: " + rawOsString);
        }

        public String getOsString() {
            return osString;
        }

        public String getBinary() {
            return binary;
        }
    }

    private enum Arch {
        ARM64("arm64"),
        X64("x64"),
        X86("x86");

        private final String archString;

        Arch(String arch) {
            this.archString = arch;
        }

        public static Arch parse(String rawArchString) {
            // See
            // https://github.com/trustin/os-maven-plugin/blob/os-maven-plugin-1.7.1/src/main/java/kr/motd/maven/os/Detector.java#L192 (Apache License 2.0).
            rawArchString = rawArchString.toLowerCase(Locale.ROOT);
            if ("aarch64".equals(rawArchString)) {
                return ARM64;
            }
            if (rawArchString.matches("^(x8664|x86_64|amd64|ia32e|em64t|x64)$")) {
                return X64;
            }
            if (rawArchString.matches("^(x8632|x86|i[3-6]86|ia32|x32)$")) {
                return X86;
            }
            throw new IllegalArgumentException(
                    "Detected processor architecture is not recognized or supported: "
                            + rawArchString);
        }

        public String getArchString() {
            return archString;
        }
    }

    private static class KiotaParams {

        private final Os os;
        private final Arch arch;

        public KiotaParams(String rawOsString, String rawArchString) {
            os = Os.parse(rawOsString);
            arch = Arch.parse(rawArchString);
        }

        public String downloadArtifact() {
            return os.getOsString() + "-" + arch.getArchString();
        }

        public String binary() {
            return os.getBinary();
        }
    }
}
