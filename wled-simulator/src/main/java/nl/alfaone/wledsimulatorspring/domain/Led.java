package nl.alfaone.wledsimulatorspring.domain;

import lombok.Data;

@Data
public class Led {
    private int r;
    private int g;
    private int b;

    public Led(int r, int g, int b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }
}
