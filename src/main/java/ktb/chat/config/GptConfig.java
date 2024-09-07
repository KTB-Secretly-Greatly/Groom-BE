package ktb.chat.config;

import com.theokanning.openai.service.OpenAiService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
public class GptConfig {

    @Value("${openai.secret-key}")
    private String token;

    @Bean
    public OpenAiService openAiService(){
        log.info("token : {}을 활용한 OpenAiService", token);
        return new OpenAiService(token, Duration.ofSeconds(60));
        //REST API로 구현했을 때 Response가 전체 답변이 입력된 후 호출이 되기에 wait time을 걸어두지 않으면 time out 이슈가 발생한다.
    }

}
