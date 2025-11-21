package nl.alfaone;

import com.embabel.agent.config.annotation.EnableAgents;
import com.embabel.agent.config.annotation.LoggingThemes;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {
        org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration.class
})
@EnableAgents(loggingTheme = LoggingThemes.STAR_WARS)
public class AlfaWallApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlfaWallApplication.class, args);
    }

}
