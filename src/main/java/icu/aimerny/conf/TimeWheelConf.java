package icu.aimerny.conf;

import icu.aimerny.timewheel.ring.TimeWheel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootConfiguration
public class TimeWheelConf {

    @Value("${timeWheel.slotSize}")
    private Integer slotSize;

    @Value("${timeWheel.interval}")
    private Integer interval;

    @Bean
    public TimeWheel getTimeWheel() {
        return new TimeWheel(slotSize, interval);
    }

}
