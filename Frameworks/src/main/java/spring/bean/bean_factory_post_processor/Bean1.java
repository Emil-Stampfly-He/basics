package spring.bean.bean_factory_post_processor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Bean1 {
    public Bean1() {log.info("Managed by Spring");}
}
