package io.kiota.quarkus.deployment;

public class KiotaJsonCodeGen extends KiotaCodeGen {
    @Override
    public String inputExtension() {
        return "json";
    }
}
