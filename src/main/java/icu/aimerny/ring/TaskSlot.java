package icu.aimerny.ring;

import java.util.LinkedList;

public class TaskSlot {
    // 每个时间轮的槽内都有一组待执行的任务
    private LinkedList<ScheduleTask> scheduleTasks;

    public TaskSlot() {
        this.scheduleTasks = new LinkedList<>();
    }
}
