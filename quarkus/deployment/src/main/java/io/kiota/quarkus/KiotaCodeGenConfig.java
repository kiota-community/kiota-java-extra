package io.kiota.quarkus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import org.eclipse.microprofile.config.Config;

// This configuration is read in codegen phase (before build time), the annotation is for
// documentation
// purposes and to avoid quarkus warnings
@ConfigRoot(name = "kiota", phase = ConfigPhase.BUILD_TIME)
public class KiotaCodeGenConfig {
    static final String KIOTA_CONFIG_PREFIX = "quarkus.kiota";
    // overwrite the automatically detected Operating System
    private static final String OS = KIOTA_CONFIG_PREFIX + ".os";
    // overwrite the automatically detected system architecture
    private static final String ARCH = KIOTA_CONFIG_PREFIX + ".arch";
    // Path to a kiota executable file to be used
    private static final String PROVIDED = KIOTA_CONFIG_PREFIX + ".provided";
    // Kiota release url
    private static final String DEFAULT_RELEASE_URL = "https://github.com/microsoft/kiota/releases";
    private static final String RELEASE_URL = KIOTA_CONFIG_PREFIX + ".release.url";
    // Kiota version, will try to resolve latest if not provided
    private static final String VERSION = KIOTA_CONFIG_PREFIX + ".version";
    // Timout, in seconds, used when executing the Kiota CLI
    private static final int DEFAULT_TIMEOUT = 30;
    private static final String TIMEOUT = KIOTA_CONFIG_PREFIX + ".timeout";

    // Kiota generate parameters
    private static final String DEFAULT_CLIENT_NAME = "ApiClient";
    private static final String CLIENT_CLASS_NAME = ".class-name";

    private static final String DEFAULT_CLIENT_PACKAGE = "io.apisdk";
    private static final String CLIENT_PACKAGE_NAME = ".package-name";
    private static final String INCLUDE_PATH = ".include-path";
    private static final String EXCLUDE_PATH = ".exclude-path";
    private static final List<String> DEFAULT_SERIALIZER =
            List.of(
                    "io.kiota.serialization.json.quarkus.JsonSerializationWriterFactory",
                    "com.microsoft.kiota.serialization.TextSerializationWriterFactory",
                    "com.microsoft.kiota.serialization.FormSerializationWriterFactory",
                    "com.microsoft.kiota.serialization.MultipartSerializationWriterFactory");
    private static final String SERIALIZER = ".serializer";

    private static final List<String> DEFAULT_DESERIALIZER =
            List.of(
                    "io.kiota.serialization.json.quarkus.JsonParseNodeFactory",
                    "com.microsoft.kiota.serialization.TextParseNodeFactory",
                    "com.microsoft.kiota.serialization.FormParseNodeFactory");
    private static final String DESERIALIZER = ".deserializer";

    /**
     * Overrides the detected Operating System
     */
    @ConfigItem(name = "os")
    public Optional<String> os;

    public static io.quarkus.utilities.OS getOs(final Config config) {
        String os = config.getConfigValue(OS).getValue();
        if (os == null) {
            return io.quarkus.utilities.OS.determineOS();
        }
        return io.quarkus.utilities.OS.valueOf(os);
    }

    /**
     * Overrides the detected Architecture
     */
    @ConfigItem(name = "arch")
    public Optional<String> arch;

    public static String getArch(final Config config) {
        String arch = config.getConfigValue(ARCH).getValue();
        if (arch == null) {
            return io.quarkus.utilities.OS.getArchitecture();
        }
        return arch;
    }

    /**
     * The path to a kiota executable location to be used.
     * When set, the kiota version is not going to be checked/used.
     */
    @ConfigItem(name = "provided")
    public Optional<String> provided;

    public static String getProvided(final Config config) {
        return config.getConfigValue(PROVIDED).getValue();
    }

    /**
     * The path to a kiota executable location to be used
     */
    @ConfigItem(name = "release.url", defaultValue = DEFAULT_RELEASE_URL)
    public Optional<String> releaseUrl;

    public static String getReleaseUrl(final Config config) {
        String releaseUrl = config.getConfigValue(RELEASE_URL).getValue();
        if (releaseUrl != null) {
            return releaseUrl;
        }
        return DEFAULT_RELEASE_URL;
    }

    /**
     * The kiota version to be used.
     * If not provided we are going to try to resolve "latest" from the GitHub API.
     * Please, set this property in any production grade project.
     */
    @ConfigItem(name = "version")
    public Optional<String> version;

    public static String getVersion(final Config config) {
        String version = config.getConfigValue(VERSION).getValue();
        if (version == null) {
            // Dynamically retrieve latest for convenience
            Log.warn("No Kiota version specified, trying to retrieve it from the GitHub API");
            try {
                URI releaseURI = new URI(getReleaseUrl(config));
                URI latestVersionURI =
                        new URI(
                                releaseURI.getScheme(),
                                "api." + releaseURI.getHost(),
                                "/repos" + releaseURI.getPath() + "/latest",
                                releaseURI.getFragment());

                HttpRequest request = HttpRequest.newBuilder().uri(latestVersionURI).GET().build();

                HttpResponse<String> response =
                        HttpClient.newBuilder()
                                .build()
                                .send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    Log.warn(
                            "Failed to retrieve the latest Kiota version, please provide it"
                                    + " explicitly.");
                    return null;
                }

                ObjectMapper mapper = new ObjectMapper();
                JsonNode latestVersionJson = mapper.readTree(response.body());
                String latestVersion = latestVersionJson.get("name").asText();
                if (latestVersion.startsWith("v")) {
                    latestVersion = latestVersion.substring(1);
                }
                return latestVersion;
            } catch (URISyntaxException e) {
                Log.warn(
                        "Failed to retrieve the latest Kiota version, please provide it"
                                + " explicitly.",
                        e);
                return null;
            } catch (IOException e) {
                Log.warn(
                        "Failed to retrieve the latest Kiota version, please provide it"
                                + " explicitly.",
                        e);
                return null;
            } catch (InterruptedException e) {
                Log.warn(
                        "Failed to retrieve the latest Kiota version, please provide it"
                                + " explicitly.",
                        e);
                return null;
            }
        }
        return version;
    }

    /**
     * The timeout to be used when running the kiota CLI.
     */
    @ConfigItem(name = "timeout", defaultValue = "" + DEFAULT_TIMEOUT)
    public Optional<String> timeout;

    public static int getTimeout(final Config config) {
        String timeout = config.getConfigValue(TIMEOUT).getValue();
        if (timeout != null) {
            return Integer.valueOf(timeout);
        }
        return DEFAULT_TIMEOUT;
    }

    /**
     * Configuration resolved based on the OpenAPI description file name
     */
    @ConfigItem(name = "spec-name")
    public Optional<SpecConfig> specName;

    @ConfigGroup
    public static final class SpecConfig {

        /**
         * The generated API client class name.
         */
        @ConfigItem(name = "class-name", defaultValue = DEFAULT_CLIENT_NAME)
        public Optional<String> className;

        /**
         * The generated API client package name.
         */
        @ConfigItem(name = "package-name", defaultValue = DEFAULT_CLIENT_PACKAGE)
        public Optional<String> packageName;

        /**
         * The glob expression to be used to identify the endpoints to be included in the generation.
         */
        @ConfigItem(name = "include-path")
        public Optional<String> includePath;

        /**
         * The glob expression to be used to identify the endpoints to be excluded from the generation.
         */
        @ConfigItem(name = "exclude-path")
        public Optional<String> excludePath;

        /**
         * ADVANCED:
         * The serializers to be used in the generated code.
         */
        @ConfigItem(name = "serializer")
        public Optional<List<String>> serializer;

        /**
         * ADVANCED:
         * The deserializers to be used in the generated code.
         */
        @ConfigItem(name = "deserializer")
        public Optional<List<String>> deserializer;
    }

    public static String getClientClassName(final Config config, String filename) {
        String clientName =
                config.getConfigValue(KIOTA_CONFIG_PREFIX + "." + filename + CLIENT_CLASS_NAME)
                        .getValue();
        if (clientName != null) {
            return clientName;
        }
        return DEFAULT_CLIENT_NAME;
    }

    public static String getClientPackageName(final Config config, String filename) {
        String packageName =
                config.getConfigValue(KIOTA_CONFIG_PREFIX + "." + filename + CLIENT_PACKAGE_NAME)
                        .getValue();
        if (packageName != null) {
            return packageName;
        }
        return DEFAULT_CLIENT_PACKAGE + "." + filename;
    }

    public static String getIncludePath(final Config config, String filename) {
        return config.getConfigValue(KIOTA_CONFIG_PREFIX + "." + filename + INCLUDE_PATH)
                .getValue();
    }

    public static String getExcludePath(final Config config, String filename) {
        return config.getConfigValue(KIOTA_CONFIG_PREFIX + "." + filename + EXCLUDE_PATH)
                .getValue();
    }

    public static List<String> getSerializer(final Config config, String filename) {
        String serializer =
                config.getConfigValue(KIOTA_CONFIG_PREFIX + "." + filename + SERIALIZER).getValue();
        if (serializer != null) {
            return List.of(serializer.split(","));
        }
        return DEFAULT_SERIALIZER;
    }

    public static List<String> getDeserializer(final Config config, String filename) {
        String deserializer =
                config.getConfigValue(KIOTA_CONFIG_PREFIX + "." + filename + DESERIALIZER)
                        .getValue();
        if (deserializer != null) {
            return List.of(deserializer.split(","));
        }
        return DEFAULT_DESERIALIZER;
    }
}
