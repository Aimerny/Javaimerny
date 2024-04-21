package icu.aimerny.ring;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TimeWheel {
    // 定时触发的时间
    private int interval;
    // 当前执行的slot
    private int curSlot;
    // 时间轮,环状数组
    private TaskSlot[] slots;
    // 映射集合
    private Map<String, ScheduleTask> taskMap;

    public TimeWheel(int slotSize, int interval) {
        if (slotSize < 0) {
            slotSize = 10;
        }
        if(interval < 0) {
            interval = 1000;
        }
        this.slots = new TaskSlot[slotSize];
        this.taskMap = new ConcurrentHashMap<>();
        this.interval = interval;
        this.curSlot = 0;
        // 初始化TaskSlot数组
        for (TaskSlot slot : this.slots) {
            slot = new TaskSlot();
        }
    }



}
