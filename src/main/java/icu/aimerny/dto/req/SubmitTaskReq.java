package icu.aimerny.dto.req;

import lombok.Data;

@Data
public class SubmitTaskReq {

    private String key;
    private String printCommand;
    private Integer delay;

}
