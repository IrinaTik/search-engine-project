package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jsoup-setting")
public class JsoupConfig {
    private String userAgent;
    private String referrer;
    private int timeout;
    private int pauseBeforeParseMultiplier;
    private int pauseBeforeParseSummand;
    private int maxBodySize;
    private boolean ignoreContentType;
}
