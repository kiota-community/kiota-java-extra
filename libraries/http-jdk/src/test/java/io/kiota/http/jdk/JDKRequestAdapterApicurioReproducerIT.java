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

        SystemInfo systemInfo =
                Assertions.assertTimeoutPreemptively(
                        Duration.ofSeconds(30), () -> client.system().info().get());
        Assertions.assertNotNull(systemInfo);

        CreateGroupMetaData createGroup = new CreateGroupMetaData();
        createGroup.setId("test-group");

        Assertions.assertTimeoutPreemptively(
                Duration.ofSeconds(30), () -> client.groups().post(createGroup));
    }
}
