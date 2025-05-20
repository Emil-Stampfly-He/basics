package spring.bean.init_destroy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class InitAndDestroy {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(InitAndDestroy.class, args);
        context.close();
    }

    @Bean(initMethod = "init3")
    public Bean1 bean1() {return new Bean1();}

    @Bean(destroyMethod = "destroy3")
    public Bean2 bean2() {return new Bean2();}
}
