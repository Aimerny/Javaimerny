package icu.aimerny.timewheel.ring;

import lombok.Getter;

import java.util.LinkedList;

@Getter
public class TaskSlot {
    // 每个时间轮的槽内都有一组待执行的任务
    private final LinkedList<ScheduleTask> scheduleTasks;

    public TaskSlot() {
        this.scheduleTasks = new LinkedList<>();
    }
}
