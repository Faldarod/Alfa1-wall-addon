package nl.alfaone.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for optional ChatClient bean.
 * Only creates ChatClient if ChatModel is available.
 */
@Configuration
@Slf4j
public class ChatClientConfig {

    @Bean
    @ConditionalOnBean(ChatModel.class)
    public ChatClient chatClient(ChatModel chatModel) {
        log.info("ChatModel detected - creating ChatClient for LLM-based GDPR analysis");
        return ChatClient.builder(chatModel).build();
    }
}
