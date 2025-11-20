package nl.alfaone.wledsimulatorspring.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * WLED /json/info response
 * Contains device information for Home Assistant discovery
 */
@Data
@Builder
public class WledInfo {
    @JsonProperty("ver")
    private String version;  // e.g. "0.14.1"

    @JsonProperty("vid")
    private Integer versionId;  // e.g. 2309200

    @JsonProperty("leds")
    private LedInfo leds;

    @JsonProperty("name")
    private String name;  // Device name

    @JsonProperty("udpport")
    private Integer udpPort;

    @JsonProperty("live")
    private Boolean live;

    @JsonProperty("lm")
    private String ledModel;  // e.g. "WS281x"

    @JsonProperty("lip")
    private String localIp;

    @JsonProperty("ws")
    private Integer websocketClients;

    @JsonProperty("fxcount")
    private Integer effectCount;

    @JsonProperty("palcount")
    private Integer paletteCount;

    @JsonProperty("arch")
    private String architecture;  // e.g. "esp32"

    @JsonProperty("core")
    private String core;  // Core version

    @JsonProperty("freeheap")
    private Integer freeHeap;  // Free heap memory

    @JsonProperty("uptime")
    private Integer uptime;  // Uptime in seconds

    @JsonProperty("opt")
    private Integer options;  // Option flags

    @JsonProperty("brand")
    private String brand;  // Brand name

    @JsonProperty("product")
    private String product;  // Product name

    @JsonProperty("btype")
    private String buildType;  // Build type

    @JsonProperty("mac")
    private String macAddress;  // MAC address

    @JsonProperty("fs")
    private Filesystem filesystem;  // Filesystem info

    @Data
    @Builder
    public static class Filesystem {
        @JsonProperty("u")
        private Integer used;  // Used space in KB

        @JsonProperty("t")
        private Integer total;  // Total space in KB

        @JsonProperty("pmt")
        private Integer presetMountedTime;  // Preset mounted timestamp
    }

    @Data
    @Builder
    public static class LedInfo {
        @JsonProperty("count")
        private Integer count;  // Total LED count

        @JsonProperty("rgbw")
        private Boolean rgbw;  // RGBW or RGB

        @JsonProperty("wv")
        private Boolean whiteValue;  // Has white channel

        @JsonProperty("cct")
        private Boolean cct;  // Color temperature control

        @JsonProperty("pin")
        private java.util.List<Integer> pin;  // GPIO pins

        @JsonProperty("pwr")
        private Integer power;  // Estimated power in mW

        @JsonProperty("fps")
        private Integer fps;  // Frames per second

        @JsonProperty("maxpwr")
        private Integer maxPower;  // Maximum power limit

        @JsonProperty("maxseg")
        private Integer maxSegments;  // Max number of segments

        @JsonProperty("seglc")
        private java.util.List<Integer> segmentLightCapabilities;  // Segment light capabilities bitmask
    }
}
