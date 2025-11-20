package nl.alfaone.wledsimulatorspring.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * WLED Segment - represents a portion of the LED strip
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Segment {
    @JsonProperty("id")
    private Integer id;

    @JsonProperty("start")
    private Integer start;  // First LED in segment

    @JsonProperty("stop")
    private Integer stop;  // Last LED in segment (exclusive)

    @JsonProperty("len")
    private Integer length;  // Number of LEDs

    @JsonProperty("grp")
    @Builder.Default
    private Integer grouping = 1;  // LED grouping

    @JsonProperty("spc")
    @Builder.Default
    private Integer spacing = 0;  // LED spacing

    @JsonProperty("of")
    @Builder.Default
    private Integer offset = 0;  // Offset

    @JsonProperty("on")
    @Builder.Default
    private Boolean on = true;  // Segment power

    @JsonProperty("frz")
    @Builder.Default
    private Boolean freeze = false;  // Freeze effect

    @JsonProperty("bri")
    @Builder.Default
    private Integer brightness = 255;  // Segment brightness (0-255)

    @JsonProperty("cct")
    @Builder.Default
    private Integer colorTemperature = 127;  // Color temperature (0-255)

    @JsonProperty("col")
    private List<List<Integer>> colors;  // Colors [[R,G,B,W], [R,G,B,W], [R,G,B,W]]

    @JsonProperty("fx")
    @Builder.Default
    private Integer effect = 0;  // Effect ID

    @JsonProperty("sx")
    @Builder.Default
    private Integer speed = 128;  // Effect speed (0-255)

    @JsonProperty("ix")
    @Builder.Default
    private Integer intensity = 128;  // Effect intensity (0-255)

    @JsonProperty("pal")
    @Builder.Default
    private Integer palette = 0;  // Palette ID

    @JsonProperty("sel")
    @Builder.Default
    private Boolean selected = true;  // Is segment selected

    @JsonProperty("rev")
    @Builder.Default
    private Boolean reverse = false;  // Reverse direction

    @JsonProperty("mi")
    @Builder.Default
    private Boolean mirror = false;  // Mirror effect
}
