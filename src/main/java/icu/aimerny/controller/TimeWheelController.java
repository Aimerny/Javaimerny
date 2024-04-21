package icu.aimerny.controller;

import com.fasterxml.jackson.databind.JsonNode;
import icu.aimerny.dto.req.SubmitTaskReq;
import icu.aimerny.service.TimeWheelService;
import icu.aimerny.timewheel.exceptions.SubmitFailedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@Slf4j
public class TimeWheelController {

    @Resource
    private TimeWheelService timeWheelService;

    @PostMapping("/timeWheel/print")
    public String submitPrintTask(@RequestBody SubmitTaskReq req){
        try {
            timeWheelService.submitTask(req.getKey(), req.getPrintCommand(), req.getDelay());
        }catch (Exception e){
            log.error("Submit error: ", e);
            return "failed";
        }
        return "success";
    }


}
