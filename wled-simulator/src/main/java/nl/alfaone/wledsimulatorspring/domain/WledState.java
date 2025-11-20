package nl.alfaone.wledsimulatorspring.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * WLED /json/state response
 * Contains the current state of the WLED device
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WledState {
    @JsonProperty("on")
    @Builder.Default
    private Boolean on = true;  // Master power

    @JsonProperty("bri")
    @Builder.Default
    private Integer brightness = 128;  // Master brightness (0-255)

    @JsonProperty("transition")
    @Builder.Default
    private Integer transition = 7;  // Transition time in 100ms units

    @JsonProperty("ps")
    @Builder.Default
    private Integer preset = -1;  // Preset ID (-1 = none)

    @JsonProperty("pl")
    @Builder.Default
    private Integer playlist = -1;  // Playlist ID (-1 = none)

    @JsonProperty("nl")
    private Nightlight nightlight;

    @JsonProperty("udpn")
    private UdpSync udpSync;

    @JsonProperty("lor")
    @Builder.Default
    private Integer ledOutputRange = 0;  // LED output range

    @JsonProperty("mainseg")
    @Builder.Default
    private Integer mainSegment = 0;  // Main segment ID

    @JsonProperty("seg")
    private List<Segment> segments;  // LED segments

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Nightlight {
        @JsonProperty("on")
        @Builder.Default
        private Boolean on = false;

        @JsonProperty("dur")
        @Builder.Default
        private Integer duration = 60;  // Duration in minutes

        @JsonProperty("mode")
        @Builder.Default
        private Integer mode = 1;  // Nightlight mode

        @JsonProperty("tbri")
        @Builder.Default
        private Integer targetBrightness = 0;  // Target brightness

        @JsonProperty("rem")
        @Builder.Default
        private Integer remaining = -1;  // Remaining time in minutes
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UdpSync {
        @JsonProperty("send")
        @Builder.Default
        private Boolean send = false;  // Send UDP sync

        @JsonProperty("recv")
        @Builder.Default
        private Boolean receive = true;  // Receive UDP sync
    }
}
