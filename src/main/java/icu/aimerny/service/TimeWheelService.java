package icu.aimerny.service;

import icu.aimerny.timewheel.exceptions.SubmitFailedException;
import icu.aimerny.timewheel.ring.TimeWheel;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Callable;

@Service
public class TimeWheelService {

    @Resource
    private TimeWheel timeWheel;

    public void submitTask(String key, String printCommand, int delay) throws SubmitFailedException {
        Callable<String> callable = () -> {
            System.out.println(printCommand);
            return "Nice";
        };
        timeWheel.submitTask(key, callable, delay, ChronoUnit.SECONDS);
    }


}
