package io.kiota.http.jdk;

import com.microsoft.kiota.HttpMethod;
import com.microsoft.kiota.RequestInformation;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
public class JDKRequestAdapterPostReproducerTest {

    private static HttpServer server;
    private static String baseUrl;

    @BeforeAll
    public static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(
                "/system/info",
                new HttpHandler() {
                    @Override
                    public void handle(HttpExchange exchange) throws IOException {
                        try {
                            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                                byte[] body =
                                        "{\"name\":\"test\",\"description\":\"test\"}"
                                                .getBytes(StandardCharsets.UTF_8);
                                exchange.getResponseHeaders()
                                        .add("Content-Type", "application/json");
                                exchange.sendResponseHeaders(200, body.length);
                                exchange.getResponseBody().write(body);
                            } else {
                                exchange.sendResponseHeaders(405, -1);
                            }
                        } finally {
                            exchange.close();
                        }
                    }
                });
        server.createContext(
                "/groups",
                new HttpHandler() {
                    @Override
                    public void handle(HttpExchange exchange) throws IOException {
                        try {
                            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                                exchange.getRequestBody().readAllBytes();
                                exchange.sendResponseHeaders(204, -1);
                            } else {
                                exchange.sendResponseHeaders(405, -1);
                            }
                        } finally {
                            exchange.close();
                        }
                    }
                });
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();
        int port = server.getAddress().getPort();
        baseUrl = "http://localhost:" + port;
    }

    @AfterAll
    public static void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    public void postRequestDoesNotHang() {
        JDKRequestAdapter adapter = new JDKRequestAdapter();
        adapter.setBaseUrl(baseUrl);

        RequestInformation getInfo = new RequestInformation();
        getInfo.setUri(URI.create(baseUrl + "/system/info"));
        getInfo.httpMethod = HttpMethod.GET;
        getInfo.headers.add("Accept", "application/json");
        adapter.sendPrimitive(getInfo, null, Void.class);

        RequestInformation requestInfo = new RequestInformation();
        requestInfo.setUri(URI.create(baseUrl + "/groups"));
        requestInfo.httpMethod = HttpMethod.POST;
        byte[] body = "{\"groupId\":\"test-group\"}".getBytes(StandardCharsets.UTF_8);
        requestInfo.content = new ByteArrayInputStream(body);
        requestInfo.headers.add("Content-Type", "application/json");
        requestInfo.headers.add("Accept", "application/json");

        Assertions.assertTimeoutPreemptively(
                Duration.ofSeconds(10), () -> adapter.sendPrimitive(requestInfo, null, Void.class));
    }
}
