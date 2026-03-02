# Kiota Java Extra

[![Release](https://img.shields.io/github/v/release/kiota-community/kiota-java-extra)](https://search.maven.org/search?q=g:io.kiota%20a:kiota-maven-plugin)

Integrations, utilities and alternative implementations to work with [Kiota](https://github.com/microsoft/kiota) in Java.

## Maven Plugin

The Kiota Maven plugin eases the usage of the Kiota CLI from Maven projects.
To use the plugin add this section to your `pom.xml`:

```xml
  <build>
    <plugins>
      <plugin>
        <artifactId>kiota-maven-plugin</artifactId>
        <groupId>io.kiota</groupId>
        <version>${version}</version>
        <executions>
          <execution>
            <goals>
              <goal>generate</goal>
            </goals>
          </execution>
        </executions>
        <configuration>
          <file>openapi.yaml</file>
            ... or ...
          <url>https://raw.githubusercontent.com/OpenAPITools/openapi-petstore/master/src/main/resources/openapi.yaml</url>
          ...
        </configuration>
      </plugin>
    </plugins>
  </build>
```

the available options, as of today, are (output of `mvn help:describe -DgroupId=io.kiota -DartifactId=kiota-maven-plugin -Dversion=999-SNAPSHOT -Ddetail`):

```
Available parameters:

    baseURL (Default:
    https://github.com/andreaTP/kiota-prerelease/releases/download)
      Base URL to be used for the download

    cleanOutput (Default: false)
      Clean output before generating

    clearCache (Default: false)
      Clear cache before generating

    clientClass (Default: ApiClient)
      The class name to use for the core client class. [default: ApiClient]

    deserializers (Default:
    io.kiota.serialization.json.JsonParseNodeFactory,com.microsoft.kiota.serialization.TextParseNodeFactory,com.microsoft.kiota.serialization.FormParseNodeFactory)
      The deserializers to be used by kiota

    downloadTarget (Default: ${basedir}/target/openapi-spec)
      The Download target folder for CRDs downloaded from remote URLs

    file
      The openapi specification to be used for generating code

    kiotaLogLevel (Default: Warning)
      The log level of Kiota to use when logging messages to the main output.
      [default: Warning]

    kiotaTimeout (Default: 30)
      Kiota timeout in seconds

    kiotaVersion (Default: 0.0.0-pre+microsoft.main.f84da5a)
      Version of Kiota to be used

    language (Default: Java)
      Language to generate the code for:

    namespace (Default: com.apisdk)
      The namespace to use for the core client class specified with the
      --class-name option. [default: ApiSdk]

    osName (Default: ${os.name})
      OS name

    serializers (Default:
    io.kiota.serialization.json.JsonSerializationWriterFactory,com.microsoft.kiota.serialization.TextSerializationWriterFactory,com.microsoft.kiota.serialization.FormSerializationWriterFactory,com.microsoft.kiota.serialization.MultipartSerializationWriterFactory)
      The serializers to be used by kiota

    skip (Default: false)
      User property: kiota.skip
      Skip the execution of the goal

    targetBinaryFolder (Default:
    ${project.build.directory}/kiota/)
      Required: true
      Kiota executable target binary folder

    targetDirectory (Default:
    ${project.build.directory}/generated-sources/kiota)
      Location where to generate the Java code

    url
      The URL to be used to download an API spec from a remote location

    useSystemKiota (Default: false)
      User property: kiota.system
      Use system provided kiota executable (needs to be available on the PATH)

    downloadMaxRetries (Default: 3)
      User property: kiota.download.maxRetries
      Maximum number of retry attempts for downloading the Kiota binary.

    downloadRetryDelayMs (Default: 1000)
      User property: kiota.download.retryDelayMs
      Initial delay in milliseconds between download retry attempts.
      The delay doubles with each subsequent retry (exponential backoff),
      up to a maximum of 256 seconds.

    downloadToken
      User property: kiota.download.token
      GitHub token for authenticating download requests. Useful for private
      repositories or to avoid rate limiting. If not set and
      downloadUseTokenFromEnv is true, the plugin checks the following
      environment variables in order: GITHUB_TOKEN, GH_TOKEN.

    downloadUseTokenFromEnv (Default: true)
      User property: kiota.download.useTokenFromEnv
      Whether to look for a download token in environment variables
      (GITHUB_TOKEN, GH_TOKEN) when no explicit token
      is configured. Set to false to disable this behavior.
```

## Quarkus extension

The Quarkus extension now lives in the Quarkiverse organization:

https://github.com/quarkiverse/quarkus-kiota

## Libraries

In this project you have a few alternative implementations of the Kiota [Core libraries](https://learn.microsoft.com/en-us/openapi/kiota/design#kiota-abstractions)

### Serialization Jackson

This is a [Jackson](https://github.com/FasterXML/jackson) based implementation of the Json serialization/deserialization APIs exposed by the core libraries.

To use it, add the following dependency to your project:

```xml
<dependency>
  <groupId>io.kiota</groupId>
  <artifactId>kiota-serialization-jackson</artifactId>
  <version>VERSION</version>
</dependency>
```

and make sure to remove from the classpath the default implementation `com.microsoft.kiota:microsoft-kiota-serialization-json`.
When generating the client code you need to change the defaults as well to use the provided implementations:

- `serializer`: `io.kiota.serialization.json.JsonSerializationWriterFactory`
- `deserializer`: `io.kiota.serialization.json.JsonParseNodeFactory`

### Http Vert.X

This is a `RequestAdapter` implementation based on the [Vert.X Web Client](https://vertx.io/docs/vertx-web-client/java/).

To use it, add the following dependency to your project:

```xml
<dependency>
  <groupId>io.kiota</groupId>
  <artifactId>kiota-http-vertx</artifactId>
  <version>VERSION</version>
</dependency>
```

and make sure to remove from the classpath the default implementation `com.microsoft.kiota:microsoft-kiota-http-okHttp`.
You can now use it in your codebase:

```java
var adapter = new VertXRequestAdapter(vertx);
```

To configure authorization we expect you to tweak the `VertX.WebClient` instance before passing it to the constructor.
For example, using OIDC with Client Id and secret, your code might look like this:

```java
OAuth2Options options =
        new OAuth2Options()
                .setFlow(OAuth2FlowType.CLIENT)
                .setClientId(CLIENT_ID)
                .setTokenPath(keycloakUrl + "token")
                .setClientSecret(CLIENT_SECRET);

OAuth2Auth oAuth2Auth = OAuth2Auth.create(vertx, options);

Oauth2Credentials oauth2Credentials = new Oauth2Credentials();

OAuth2WebClient oAuth2WebClient =
        OAuth2WebClient.create(WebClient.create(vertx), oAuth2Auth)
                .withCredentials(oauth2Credentials);

var adapter = new VertXRequestAdapter(oAuth2WebClient);
```

### Http JDK

This is a `RequestAdapter` implementation based on the [Java standard library Http Client](https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpClient.html).

To use it, add the dependency to your project:

```xml
<dependency>
  <groupId>io.kiota</groupId>
  <artifactId>kiota-http-jdk</artifactId>
  <version>VERSION</version>
</dependency>
```

and make sure to remove from the classpath the default implementation `com.microsoft.kiota:microsoft-kiota-http-okHttp`.
