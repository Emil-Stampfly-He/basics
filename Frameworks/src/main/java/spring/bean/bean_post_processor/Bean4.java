package spring.bean.bean_post_processor;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Data
@EnableConfigurationProperties(Bean4.class)
@ConfigurationProperties(prefix = "java")
public class Bean4 {
    private String home;
    private String version;
}
