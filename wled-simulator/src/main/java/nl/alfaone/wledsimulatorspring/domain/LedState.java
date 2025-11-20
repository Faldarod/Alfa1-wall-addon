package nl.alfaone.wledsimulatorspring.domain;

import java.util.ArrayList;
import java.util.List;

public class LedState {

    private static final LedState INSTANCE = new LedState();
    private final List<Led> leds;

    private LedState() {
        leds = new ArrayList<>();
        // Initialize with 10 black LEDs
        for (int i = 0; i < 10; i++) {
            leds.add(new Led(0, 0, 0));
        }
    }

    public static LedState getInstance() {
        return INSTANCE;
    }

    public List<Led> getLeds() {
        return leds;
    }

    public void setLeds(List<Led> leds) {
        this.leds.clear();
        this.leds.addAll(leds);
    }
}
