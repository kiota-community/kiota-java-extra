/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.kiota.quarkus.it;

import io.apisdk.example.yaml.ApiClient;
import io.kiota.http.vertx.VertXRequestAdapter;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/quarkus-kiota2")
@ApplicationScoped
public class QuarkusKiotaResource2 {

    @Inject private Vertx vertx;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Greeting hello() {
        var adapter = new VertXRequestAdapter(vertx);
        adapter.setBaseUrl("http://localhost:8081");
        ApiClient client = new ApiClient(adapter);

        io.apisdk.example.yaml.models.Greeting result =
                client.quarkusKiota().get(config -> config.queryParameters.name = "myself");

        return new Greeting(result.getValue());
    }
}
