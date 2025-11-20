package nl.alfaone.wledsimulatorspring.controller;

import nl.alfaone.wledsimulatorspring.domain.WledInfo;
import nl.alfaone.wledsimulatorspring.domain.WledState;
import nl.alfaone.wledsimulatorspring.service.WledService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WLED API Controller - mimics real WLED JSON API for Home Assistant integration
 */
@RestController
@RequestMapping("/json")
public class WledController {

    private final WledService wledService;

    public WledController(WledService wledService) {
        this.wledService = wledService;
    }

    /**
     * GET /json - Combined endpoint with all data
     * Home Assistant queries this for full device state
     */
    @GetMapping
    public Map<String, Object> getAll() {
        Map<String, Object> response = new HashMap<>();
        response.put("state", wledService.getCurrentState());
        response.put("info", wledService.getDeviceInfo());
        response.put("effects", wledService.getEffectsMap());
        response.put("palettes", wledService.getPalettesMap());
        return response;
    }

    /**
     * GET /json/state - Get current state
     */
    @GetMapping("/state")
    public WledState getState() {
        return wledService.getCurrentState();
    }

    /**
     * POST /json/state - Update state
     * Home Assistant uses this to control the lights
     */
    @PostMapping("/state")
    public WledState updateState(@RequestBody WledState state) {
        wledService.updateState(state);
        return wledService.getCurrentState();
    }

    /**
     * POST /json - Also accepts state updates
     */
    @PostMapping
    public Map<String, Object> updateJson(@RequestBody Map<String, Object> body) {
        // Extract state if present
        if (body.containsKey("state")) {
            Object stateObj = body.get("state");
            // Handle state update
            // For simplicity, we'll just return current state
        }

        Map<String, Object> response = new HashMap<>();
        response.put("state", wledService.getCurrentState());
        response.put("info", wledService.getDeviceInfo());
        return response;
    }

    /**
     * GET /json/info - Get device information
     * Home Assistant uses this for device discovery and identification
     */
    @GetMapping("/info")
    public WledInfo getInfo() {
        return wledService.getDeviceInfo();
    }

    /**
     * GET /json/eff - Get list of available effects
     */
    @GetMapping("/eff")
    public Map<Integer, String> getEffects() {
        return wledService.getEffectsMap();
    }

    /**
     * GET /json/pal - Get list of available palettes
     */
    @GetMapping("/pal")
    public Map<Integer, String> getPalettes() {
        return wledService.getPalettesMap();
    }

    /**
     * GET /json/si - Simplified info (same as /json/info for compatibility)
     */
    @GetMapping("/si")
    public WledInfo getSimplifiedInfo() {
        return wledService.getDeviceInfo();
    }
}
