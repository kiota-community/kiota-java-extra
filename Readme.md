# Kiota Java Extra

[![Release](https://img.shields.io/github/v/release/kiota-community/kiota-java-extra)](https://search.maven.org/search?q=g:io.kiota.maven%20a:kiota-maven-plugin)

Integrations, utilities and alternative implementations to work with [Kiota](https://github.com/microsoft/kiota) in Java.

## Maven Plugin

The Kiota Maven plugin ease the usage of the Kiota CLI from Maven projects.
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
```

## Quarkus extension

If you have a supersonic, subatomic [Quarkus](https://quarkus.io/) project you can use this extension to generate code with Kiota:

```xml
<dependency>
  <groupId>io.kiota</groupId>
  <artifactId>quarkus-kiota</artifactId>
  <version>VERSION</version>
</dependency>
```

remember to enable the code generation in the `quarkus-maven-plugin` configuration, if not already present, add `<goal>generate-code</goal>`:

```xml
<plugin>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-maven-plugin</artifactId>
  <executions>
    <execution>
      <goals>
        <goal>build</goal>
        <goal>generate-code</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

now you can drop any Open API specification in the `src/<scope>/openapi` folder and configure the extension as usual with Quarkus configuration.
We highly encourage you to pin `quarkus.kiota.version` to a specific version and not relying on the "latest detection" built-in mechanism for production code.

| config | description |
|---|---|
| quarkus.kiota.os | Override the detected Operating System |
| quarkus.kiota.arch | Override the detected Architecture |
| quarkus.kiota.provided | Specify the path to an available Kiota CLI to be used |
| quarkus.kiota.release.url | Define an alternative URL to be used to download the Kiota CLI |
| quarkus.kiota.version | Define a specific Kiota version to be used |
| quarkus.kiota.timeout | Global timeout over the execution of the Kiota CLI |

To fine tune the generation you can define additional properties after the Open API spec file name:

| config | description |
|---|---|
| quarkus.kiota.<filename>.class-name | Specify the name for the generated client class |
| quarkus.kiota.<filename>.package-name | Specify the name of the package for the generated sources |
| quarkus.kiota.<filename>.include-path | Glob expression to identify the endpoint to be included in the generation |
| quarkus.kiota.<filename>.exclude-path | Glob expression to identify the endpoint to be excluded in the generation |
| quarkus.kiota.<filename>.serializer | Overwrite the serializers for the generation |
| quarkus.kiota.<filename>.deserializer | Overwrite the deserializers for the generation |

Using the extension, by default, the Json serializer and deserializer will be based on Jackson instead of the official one based on Gson.

## Libraries

In this project you have a few alternative implementations of the [Core libraries](https://learn.microsoft.com/en-us/openapi/kiota/design#kiota-abstractions)

### Serialization Jackson

This is a [Jackson](https://github.com/FasterXML/jackson) based implementation of the Json serialization/deserialization APIs exposed by the core libraries.

To use it, add the dependency to your project:

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

This is an `RequestAdapter` implementation based on the [Vert.X Web Client](https://vertx.io/docs/vertx-web-client/java/).

To use it, add the dependency to your project:

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

To configure authorization we expect you to tweak the `VertX.WebClient` instance before passing it to the constructor, for OIDC with Client Id and secret looks as follows:

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

This is an `RequestAdapter` implementation based on the [Java standard library Http Client](https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpClient.html).

To use it, add the dependency to your project:

```xml
<dependency>
  <groupId>io.kiota</groupId>
  <artifactId>kiota-http-jdk</artifactId>
  <version>VERSION</version>
</dependency>
```

and make sure to remove from the classpath the default implementation `com.microsoft.kiota:microsoft-kiota-http-okHttp`.
