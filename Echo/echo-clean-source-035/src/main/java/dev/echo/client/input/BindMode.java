package dev.echo.client.input;

/** Label reconstruction; accessor key-flow and enum array proof remain pending. */
public enum BindMode {
    TOGGLE("Toggle"),
    HOLD("Hold");

    private final String label;

    BindMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
