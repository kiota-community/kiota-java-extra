package io.kiota.quarkus.it;

public class Greeting {
    private String value;

    public Greeting() {
        this.value = null;
    }

    public Greeting(String value) {
        this.value = value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
