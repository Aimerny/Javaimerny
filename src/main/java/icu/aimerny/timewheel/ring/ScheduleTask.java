package icu.aimerny.timewheel.ring;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.concurrent.Callable;

@Getter
@AllArgsConstructor
public class ScheduleTask<T> {

    // 任务唯一标识
    private String key;
    // 执行的task
    private Callable<T> task;
    // 在时间轮中的位置
    private int pos;
    // 剩余循环数
    private int loopCount;

    public void skipLoop() {
        this.loopCount --;
    }

}
