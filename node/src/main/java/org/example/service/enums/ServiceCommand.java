package org.example.service.enums;

public enum ServiceCommand {
    HELP("/help"),
    REGISTRATION("/registration"),
    CANCEL("/cancel"),
    START("/start"),
    ADD("/add"),
    DONE("/done"),
    DELETE("/delete"),
    TASKS("/tasks");

    private final String value;

    ServiceCommand(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static ServiceCommand fromValue(String v) {
        for (ServiceCommand c: ServiceCommand.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        return null;
    }
}
