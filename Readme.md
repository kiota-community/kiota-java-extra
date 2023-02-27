# Kiota Java Extra

[![](https://jitpack.io/v/andreaTP/kiota-utils.svg)](https://jitpack.io/#andreaTP/kiota-utils)

This repository contains utilities to work with Kiota and Java.

## Maven Plugin

The Kiota Maven plugin ease the usage of the Kiota CLI from Maven projects.
To use the plugin add this section to your `pom.xml`:

```xml
  <build>
    <plugins>
      <plugin>
        <artifactId>kiota-maven-plugin</artifactId>
        <groupId>com.redhat.cloud</groupId>
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

the available options, as of today, are (output of `mvn help:describe -DgroupId=com.github.andreaTP.kiota-utils -DartifactId=kiota-maven-plugin -Dversion=0.1.7 -Ddetail`):

```
Name: kiota-maven-plugin
Description: A Maven plugin to generate code with Kiota
Group Id: com.redhat.cloud
Artifact Id: kiota-maven-plugin
Version: 0.1.7
Goal Prefix: kiota

This plugin has 1 goal:

kiota:generate
  Description: This plugin will run Kiota to generate sources.
  Implementation: com.redhat.cloud.kiota.maven.KiotaMojo
  Language: java
  Bound to phase: generate-sources

  Available parameters:

    baseURL (Default:
    https://github.com/microsoft/kiota/releases/download)
      Base URL to be used for the download

    cleanOutput (Default: false)
      Clean output before generating

    clearCache (Default: false)
      Clear cache before generating

    clientClass (Default: ApiClient)
      The class name to use for the core client class. [default: ApiClient]

    downloadTarget (Default: ${basedir}/target/openapi-spec)
      The Download target folder for CRDs downloaded from remote URLs

    file
      The openapi specification to be used for generating code

    kiotaLogLevel (Default: Warning)
Unknown

    kiotaTimeout (Default: 30)
      Kiota timeout in seconds

    kiotaVersion (Default: 0.11.1)
      Version of Kiota to be used

    language (Default: Java)
Unknown

    namespace (Default: ApiSdk)
      The namespace to use for the core client class specified with the
      --class-name option. [default: ApiSdk]

    osName (Default: ${os.name})
      OS name

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

## RedHat SSO Auth

We publish a Kiota compatible `TokenProvider` to enable ease access to RedHat services.

Import the dependency in your `pom.xml` with:

```xml
<dependency>
    <groupId>com.redhat.cloud</groupId>
    <artifactId>kiota-rh-auth</artifactId>
    <version>${version}</version>
</dependency>
```

Now you can easily authenticate through RedHat SSO using an [offline token](https://access.redhat.com/articles/3626371) to access Red Hat APIs:

```java
OkHttpRequestAdapter adapter = new OkHttpRequestAdapter(
        new BaseBearerTokenAuthenticationProvider(
                new RHAccessTokenProvider(offline_token)
        ));

var client = new ApiClient(adapter);
```
