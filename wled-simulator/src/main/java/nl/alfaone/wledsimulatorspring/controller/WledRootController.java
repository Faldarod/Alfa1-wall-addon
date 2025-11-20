package nl.alfaone.wledsimulatorspring.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Root-level WLED endpoints (not under /json)
 */
@RestController
public class WledRootController {

    /**
     * GET /presets.json - Get saved presets
     * Returns proper presets structure for Home Assistant
     */
    @GetMapping(value = "/presets.json", produces = "application/json")
    public Map<Integer, Object> getPresets() {
        // Return properly structured presets response
        // Real WLED devices always have at least preset 0
        // Home Assistant expects integer-keyed presets only
        return Map.of(
            0, Map.of(
                "n", "",  // Empty name for default preset
                "win", "A=255"  // Minimal window state
            ),
            1, Map.of(
                "n", "Preset 1",
                "win", "A=128"
            )
        );
    }
}
