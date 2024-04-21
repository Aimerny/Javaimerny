package icu.aimerny.ring;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ScheduleTask {

    // 执行的task
    private Runnable task;
    // 在时间轮中的位置
    private int pos;
    // 剩余循环数
    private int loopCount;
    // 任务唯一标识
    private String key;

    public void run(){
        task.run();
    }

}
