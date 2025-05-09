package spring.bean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class Component1 {

    @Async
    @EventListener
    public void listen(Object event) {
        log.info("Event: {}", event);
    }
}
