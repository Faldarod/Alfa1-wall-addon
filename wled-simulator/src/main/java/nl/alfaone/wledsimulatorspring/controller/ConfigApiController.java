package nl.alfaone.wledsimulatorspring.controller;

import nl.alfaone.wledsimulatorspring.domain.Segment;
import nl.alfaone.wledsimulatorspring.service.WledService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API for WLED simulator configuration
 * Provides endpoints for managing segments and device settings
 */
@RestController
@RequestMapping("/api")
public class ConfigApiController {

    private final WledService wledService;

    public ConfigApiController(WledService wledService) {
        this.wledService = wledService;
    }

    /**
     * Add a new segment
     * POST /api/segments
     */
    @PostMapping("/segments")
    public ResponseEntity<Segment> addSegment(@RequestBody Segment segment) {
        try {
            // Generate new segment ID
            int newId = wledService.getCurrentState().getSegments().stream()
                    .mapToInt(Segment::getId)
                    .max()
                    .orElse(-1) + 1;

            segment.setId(newId);

            // Calculate length if not provided
            if (segment.getLength() == null) {
                segment.setLength(segment.getStop() - segment.getStart());
            }

            // Add segment to current state
            wledService.getCurrentState().getSegments().add(segment);

            return ResponseEntity.ok(segment);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete a segment
     * DELETE /api/segments/{id}
     */
    @DeleteMapping("/segments/{id}")
    public ResponseEntity<Void> deleteSegment(@PathVariable Integer id) {
        try {
            var segments = wledService.getCurrentState().getSegments();

            // Don't allow deleting the last segment
            if (segments.size() <= 1) {
                return ResponseEntity.badRequest().build();
            }

            boolean removed = segments.removeIf(s -> s.getId().equals(id));

            if (removed) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update device LED count
     * PUT /api/device/leds
     */
    @PutMapping("/device/leds")
    public ResponseEntity<Map<String, Object>> updateLedCount(@RequestBody Map<String, Integer> request) {
        try {
            Integer count = request.get("count");
            if (count == null || count < 1 || count > 300) {
                return ResponseEntity.badRequest().build();
            }

            // Update LED count in device info
            wledService.getDeviceInfo().getLeds().setCount(count);

            return ResponseEntity.ok(Map.of("count", count, "message", "LED count updated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get current configuration summary
     * GET /api/config/summary
     */
    @GetMapping("/config/summary")
    public ResponseEntity<Map<String, Object>> getConfigSummary() {
        return ResponseEntity.ok(Map.of(
                "totalLeds", wledService.getDeviceInfo().getLeds().getCount(),
                "segments", wledService.getCurrentState().getSegments().size(),
                "deviceName", wledService.getDeviceInfo().getName()
        ));
    }
}
