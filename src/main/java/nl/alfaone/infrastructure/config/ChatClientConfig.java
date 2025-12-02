package nl.alfaone.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for optional ChatClient and ChatClient.Builder beans.
 * Only creates beans if ChatModel is available.
 */
@Configuration
@Slf4j
public class ChatClientConfig {

    /**
     * Provide ChatClient.Builder as a bean for agents that need to register tools dynamically.
     * EmployeeCollectorAgent uses this to register @Tool methods with .defaultFunctions(this)
     */
    @Bean
    @ConditionalOnBean(ChatModel.class)
    public ChatClient.Builder chatClientBuilder(ChatModel chatModel) {
        log.info("ChatModel detected - creating ChatClient.Builder for LLM tool selection");
        return ChatClient.builder(chatModel);
    }

    /**
     * Provide pre-built ChatClient for simple LLM interactions without tool registration.
     * PrivacyOfficerAgent uses this for GDPR analysis.
     */
    @Bean
    @ConditionalOnBean(ChatModel.class)
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder) {
        log.info("Creating ChatClient for LLM-based GDPR analysis");
        return chatClientBuilder.build();
    }
}
