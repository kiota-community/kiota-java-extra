package io.kiota.http.jdk;

import com.microsoft.kiota.RequestAdapter;
import io.apicurio.registry.rest.client.v2.RegistryClient;
import io.apicurio.registry.rest.client.v2.models.CreateGroupMetaData;
import io.apicurio.registry.rest.client.v2.models.SystemInfo;
import java.time.Duration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test using Apicurio Registry in Testcontainers to reproduce
 * https://github.com/kiota-community/kiota-java-extra/issues/206.
 *
 * <p>This follows the exact sequence reported in the issue:
 *
 * <pre>
 * RequestAdapter adapter = new JDKRequestAdapter();
 * adapter.setBaseUrl("http://localhost:8080/apis/registry/v3");
 * var client = new RegistryClient(adapter);
 * SystemInfo systemInfo = client.system().info().get();
 * CreateGroup createGroup = new CreateGroup();
 * createGroup.setGroupId("test-group");
 * client.groups().post(createGroup);
 * </pre>
 */
@Testcontainers
public class JDKRequestAdapterApicurioReproducerIT {

    @Container
    static GenericContainer<?> registry =
            new GenericContainer<>("apicurio/apicurio-registry:3.1.7")
                    .withExposedPorts(8080)
                    .waitingFor(Wait.forHttp("/apis/registry/v2/system/info").forStatusCode(200))
                    .withStartupTimeout(Duration.ofMinutes(2));

    @Test
    public void apicurioPostUsingJdkAdapterDoesNotHang() {
        String baseUrl =
                String.format(
                        "http://%s:%d/apis/registry/v2",
                        registry.getHost(), registry.getMappedPort(8080));

        RequestAdapter adapter = new JDKRequestAdapter();
        adapter.setBaseUrl(baseUrl);

        RegistryClient client = new RegistryClient(adapter);

        // First call: GET /system/info
        SystemInfo systemInfo =
                Assertions.assertTimeoutPreemptively(
                        Duration.ofSeconds(30), () -> client.system().info().get());
        Assertions.assertNotNull(systemInfo);

        // Second call: POST /groups with JSON body
        CreateGroupMetaData createGroup = new CreateGroupMetaData();
        createGroup.setId("test-group");

        // The original bug report indicates that this POST may hang or time out.
        // We express that as a timeout expectation so the current buggy behavior
        // shows up as a failing test instead of an infinite hang.
        Assertions.assertTimeoutPreemptively(
                Duration.ofSeconds(30), () -> client.groups().post(createGroup));
    }
}
