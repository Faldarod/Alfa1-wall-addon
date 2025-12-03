package nl.alfaone.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for LLM tool selection feature.
 * Controls whether LLM-driven tool selection is enabled and its behavior.
 */
@Component
@ConfigurationProperties(prefix = "alfawall.llm-tool-selection")
@Data
public class LlmToolSelectionProperties {

    /**
     * Enable or disable LLM tool selection.
     * When disabled, the system falls back to keyword routing immediately.
     * Default: true
     */
    private boolean enabled = true;

    /**
     * Timeout for LLM tool selection in milliseconds.
     * If LLM takes longer than this, it will timeout and fall back to keyword routing.
     * Default: 5000ms (5 seconds)
     */
    private int timeoutMs = 5000;

    /**
     * Whether to fall back to keyword routing on LLM errors.
     * If false, LLM errors will be propagated to the caller.
     * Default: true
     */
    private boolean fallbackOnError = true;

    /**
     * Log performance metrics for LLM tool selection.
     * Logs execution time and tool usage statistics.
     * Default: true
     */
    private boolean logPerformance = true;
}
