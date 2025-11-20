package nl.alfaone.wledsimulatorspring.service;

import nl.alfaone.wledsimulatorspring.domain.Segment;
import nl.alfaone.wledsimulatorspring.domain.WledInfo;
import nl.alfaone.wledsimulatorspring.domain.WledState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Service to manage WLED device state and info
 */
@Service
public class WledService {

    private static final Logger logger = LoggerFactory.getLogger(WledService.class);

    private WledState currentState;
    private WledInfo deviceInfo;

    // WebSocket handler for broadcasting updates (set via setter to avoid circular dependency)
    private Object webSocketHandler;

    // WLED effect names
    private final List<String> effects = Arrays.asList(
        "Solid", "Blink", "Breathe", "Wipe", "Wipe Random", "Random Colors", "Sweep",
        "Dynamic", "Colorloop", "Rainbow", "Scan", "Dual Scan", "Fade", "Theater",
        "Theater Rainbow", "Running", "Saw", "Twinkle", "Dissolve", "Dissolve Rnd",
        "Sparkle", "Dark Sparkle", "Sparkle+", "Strobe", "Strobe Rainbow", "Mega Strobe",
        "Blink Rainbow", "Android", "Chase", "Chase Random", "Chase Rainbow", "Chase Flash",
        "Chase Flash Rnd", "Rainbow Runner", "Colorful", "Traffic Light", "Sweep Random",
        "Running 2", "Red & Blue", "Stream", "Scanner", "Lighthouse", "Fireworks",
        "Rain", "Merry Christmas", "Fire Flicker", "Gradient", "Loading", "In Out",
        "In In", "Out Out", "Out In", "Circus", "Halloween", "Tri Chase", "Tri Wipe",
        "Tri Fade", "Lightning", "ICU", "Multi Comet", "Dual Scanner", "Random Chase",
        "Oscillate", "Pride 2015", "Juggle", "Palette", "Fire 2012", "Colorwaves",
        "BPM", "Fill Noise", "Noise 1", "Noise 2", "Noise 3", "Noise 4", "Colortwinkle",
        "Lake", "Meteor", "Smooth Meteor", "Railway", "Ripple", "Twinklefox", "Twinklecat",
        "Halloween Eyes", "Solid Pattern", "Solid Pattern Tri", "Spots", "Spots Fade",
        "Glitter", "Candle", "Fireworks Starburst", "Fireworks 1D", "Bouncing Balls",
        "Sinelon", "Sinelon Dual", "Sinelon Rainbow", "Popcorn", "Drip", "Plasma",
        "Percent", "Ripple Rainbow", "Heartbeat", "Pacifica", "Candle Multi", "Solid Glitter",
        "Sunrise", "Phased", "Twinkleup", "Noise Pal", "Sine", "Phased Noise", "Flow",
        "Chunchun", "Dancing Shadows", "Washing Machine", "Blends", "TV Simulator",
        "Dynamic Smooth"
    );

    // WLED palette names
    private final List<String> palettes = Arrays.asList(
        "Default", "Random Cycle", "Primary Color", "Based on Primary", "Set Colors",
        "Based on Set", "Party", "Cloud", "Lava", "Ocean", "Forest", "Rainbow",
        "Rainbow Bands", "Sunset", "Rivendell", "Breeze", "Red & Blue", "Yellowout",
        "Analogous", "Splash", "Pastel", "Sunset 2", "Beech", "Vintage", "Departure",
        "Landscape", "Beach", "Sherbet", "Hult", "Hult 64", "Drywet", "Jul", "Grintage",
        "Rewhi", "Tertiary", "Fire", "Icefire", "Cyane", "Light Pink", "Autumn",
        "Magenta", "Magred", "Yelmag", "Yelblu", "Orange & Teal", "Tiamat", "April Night",
        "Orangery", "C9", "Sakura", "Aurora", "Atlantica", "C9 2", "C9 New", "Temperature",
        "Aurora 2", "Retro Clown", "Candy", "Toxy Reaf", "Fairy Reaf", "Semi Blue",
        "Pink Candy", "Red Reaf", "Aqua Flash", "Yelblu Hot", "Lite Light", "Red Flash",
        "Blink Red", "Red Shift", "Red Tide", "Candy2"
    );

    @PostConstruct
    public void init() {
        // Initialize device info
        deviceInfo = WledInfo.builder()
                .version("0.14.1-simulator")
                .versionId(2309200)
                .name("AlfaWall LED Simulator")
                .udpPort(21324)
                .live(false)
                .ledModel("WS281x")
                .localIp("192.168.1.100")
                .websocketClients(0)
                .effectCount(effects.size())
                .paletteCount(palettes.size())
                .architecture("esp32")
                .core("3_3_6")
                .freeHeap(150000)
                .uptime(3600)
                .options(127)
                .brand("WLED")
                .product("AlfaWall Simulator")
                .buildType("simulator")
                .macAddress("AA:BB:CC:DD:EE:FF")
                .filesystem(WledInfo.Filesystem.builder()
                        .used(30)  // 30KB used
                        .total(1000)  // 1000KB total (1MB)
                        .presetMountedTime(0)
                        .build())
                .leds(WledInfo.LedInfo.builder()
                        .count(380)  // 380 LEDs for AlfaWall (5 segments x 76 LEDs)
                        .rgbw(false)
                        .whiteValue(false)
                        .cct(false)
                        .pin(List.of(2))  // GPIO pin 2
                        .power(0)
                        .fps(42)
                        .maxPower(850)
                        .maxSegments(16)
                        .segmentLightCapabilities(List.of(1, 1, 1, 1, 1))  // 5 segments, all support color (bitmask: 1 = RGB)
                        .build())
                .build();

        // Initialize state with 5 segments (76 LEDs each)
        List<Segment> segments = new ArrayList<>();

        // Define different colors for each segment
        List<List<List<Integer>>> segmentColors = Arrays.asList(
                Arrays.asList(Arrays.asList(255, 0, 0), Arrays.asList(0, 0, 0), Arrays.asList(0, 0, 0)),     // Segment 0: Red
                Arrays.asList(Arrays.asList(0, 255, 0), Arrays.asList(0, 0, 0), Arrays.asList(0, 0, 0)),     // Segment 1: Green
                Arrays.asList(Arrays.asList(0, 0, 255), Arrays.asList(0, 0, 0), Arrays.asList(0, 0, 0)),     // Segment 2: Blue
                Arrays.asList(Arrays.asList(255, 255, 0), Arrays.asList(0, 0, 0), Arrays.asList(0, 0, 0)),   // Segment 3: Yellow
                Arrays.asList(Arrays.asList(255, 0, 255), Arrays.asList(0, 0, 0), Arrays.asList(0, 0, 0))    // Segment 4: Magenta
        );

        for (int i = 0; i < 5; i++) {
            int startLed = i * 76;
            int stopLed = startLed + 76;

            Segment segment = Segment.builder()
                    .id(i)
                    .start(startLed)
                    .stop(stopLed)
                    .length(76)
                    .on(true)
                    .brightness(255)
                    .colors(segmentColors.get(i))
                    .effect(0)  // Solid
                    .speed(128)
                    .intensity(128)
                    .palette(0)  // Default
                    .build();

            segments.add(segment);
        }

        currentState = WledState.builder()
                .on(true)
                .brightness(128)
                .transition(7)
                .nightlight(WledState.Nightlight.builder().build())
                .udpSync(WledState.UdpSync.builder().build())
                .segments(segments)
                .build();
    }

    public WledInfo getDeviceInfo() {
        return deviceInfo;
    }

    public WledState getCurrentState() {
        return currentState;
    }

    public void updateState(WledState newState) {
        // Update state - merge with existing
        if (newState.getOn() != null) {
            currentState.setOn(newState.getOn());
        }
        if (newState.getBrightness() != null) {
            currentState.setBrightness(newState.getBrightness());
        }
        if (newState.getSegments() != null && !newState.getSegments().isEmpty()) {
            // Update segments
            for (Segment newSeg : newState.getSegments()) {
                updateSegment(newSeg);
            }
        }

        // Broadcast update via WebSocket
        broadcastUpdate();
    }

    private void updateSegment(Segment newSegment) {
        if (newSegment.getId() == null) {
            return;
        }

        // Find and update existing segment or add new one
        Segment existing = currentState.getSegments().stream()
                .filter(s -> s.getId().equals(newSegment.getId()))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            // Update existing segment
            if (newSegment.getOn() != null) existing.setOn(newSegment.getOn());
            if (newSegment.getBrightness() != null) existing.setBrightness(newSegment.getBrightness());
            if (newSegment.getColors() != null) existing.setColors(newSegment.getColors());
            if (newSegment.getEffect() != null) existing.setEffect(newSegment.getEffect());
            if (newSegment.getSpeed() != null) existing.setSpeed(newSegment.getSpeed());
            if (newSegment.getIntensity() != null) existing.setIntensity(newSegment.getIntensity());
            if (newSegment.getPalette() != null) existing.setPalette(newSegment.getPalette());
        } else {
            // Add new segment
            currentState.getSegments().add(newSegment);
        }
    }

    public List<String> getEffects() {
        return effects;
    }

    public Map<Integer, String> getEffectsMap() {
        Map<Integer, String> effectMap = new java.util.HashMap<>();
        for (int i = 0; i < effects.size(); i++) {
            effectMap.put(i, effects.get(i));
        }
        return effectMap;
    }

    public List<String> getPalettes() {
        return palettes;
    }

    public Map<Integer, String> getPalettesMap() {
        Map<Integer, String> paletteMap = new java.util.HashMap<>();
        for (int i = 0; i < palettes.size(); i++) {
            paletteMap.put(i, palettes.get(i));
        }
        return paletteMap;
    }

    /**
     * Set WebSocket handler for broadcasting updates
     * Uses setter injection to avoid circular dependency
     */
    public void setWebSocketHandler(Object handler) {
        this.webSocketHandler = handler;
        logger.info("WebSocket handler registered with WledService");
    }

    /**
     * Broadcast state update via WebSocket to all connected clients
     */
    private void broadcastUpdate() {
        if (webSocketHandler == null) {
            return;
        }

        try {
            // Use reflection to call broadcastStateUpdate() to avoid circular dependency
            webSocketHandler.getClass()
                .getMethod("broadcastStateUpdate")
                .invoke(webSocketHandler);
        } catch (Exception e) {
            logger.error("Error broadcasting WebSocket update: {}", e.getMessage());
        }
    }
}
