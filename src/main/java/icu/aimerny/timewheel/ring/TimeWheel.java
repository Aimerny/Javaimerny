package icu.aimerny.timewheel.ring;

import icu.aimerny.timewheel.exceptions.SubmitFailedException;
import icu.aimerny.timewheel.util.DateFormatUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


@Slf4j
public class TimeWheel implements Runnable{

    // 避免出现并发问题
    private final ReadWriteLock lock;
    // 定时触发的时间
    private final int interval;
    // 当前执行的slot
    private int curSlot;
    // 时间轮,环状数组
    private final TaskSlot[] slots;
    // 映射集合
    private final Map<String, ScheduleTask> taskMap;
    // 是否停止
    private boolean stop;

    // 本tick实际触发时间
    private long nowTriggerTime;

    // 异步执行线程池
    private final ExecutorService scheduleTaskExecutor;


    public TimeWheel(int slotSize, int interval) {
        this(slotSize, interval,null);
    }

    public TimeWheel(int slotSize, int interval, ExecutorService executor) {
        if (slotSize < 0) {
            slotSize = 10;
        }
        if(interval < 0) {
            interval = 1;
        }
        this.slots = new TaskSlot[slotSize];
        this.taskMap = new ConcurrentHashMap<>();
        this.interval = interval * 1000;
        //从1开始,因为启动的时候已经不是整秒
        this.curSlot = 0;
        this.stop = false;
        this.lock = new ReentrantReadWriteLock();
        // 初始化TaskSlot数组
        for (int i = 0; i < slotSize; i++) {
            slots[i] = new TaskSlot();
        }
        // 如果没有传入执行线程池,默认给一个5线程的线程池
        if(null != executor) {
            scheduleTaskExecutor = executor;
        }else {
            scheduleTaskExecutor =  Executors.newFixedThreadPool(5);
        }
        // 初始化完成后启动时间轮
        // 启动的时候设置为整秒的时候启动
        long now = System.currentTimeMillis();
        long fixMill = now % 1000 > 0 ? now % 1000: 0;
        nowTriggerTime = now - fixMill;
        Executors.newSingleThreadExecutor(r -> new Thread(r, "time-wheel-thread")).submit(this);
    }

    public void run() {
        while(!stop) {
            // 加锁防止并发添加任务失败
            lock.readLock().lock();
            try {
                log.debug("Time wheel trigger at:{}", DateFormatUtils.stdDateFormat(nowTriggerTime));
                promoteTimeWheel();
                tick();
                // 休眠到下次触发
                sleep();
            }catch (InterruptedException e){
                log.error("Time wheel interrupted, now stop", e);
                stop = true;
            }
            finally {
                lock.readLock().unlock();
            }
        }
    }

    /**
     *  转动时间轮,进入下一个tick
     */
    private void promoteTimeWheel() {
        curSlot = (++curSlot) % (slots.length);
    }

    private void sleep() throws InterruptedException {
        // 修正已经经过的时间
        long now = System.currentTimeMillis();
        long passedMill = now - nowTriggerTime;
        Thread.sleep(interval - passedMill);
        now = System.currentTimeMillis();
        nowTriggerTime = now - now % 1000;
    }

    private void tick() {
        // 拿到当前的slot
        TaskSlot nowSlot = this.slots[curSlot];
        // 取得其中待执行的任务
        LinkedList<ScheduleTask> tasks = nowSlot.getScheduleTasks();
        if(CollectionUtils.isEmpty(tasks)) {
            return;
        }
        // 依次执行
        Iterator<ScheduleTask> iter = tasks.iterator();
        while(iter.hasNext()) {
            ScheduleTask task = iter.next();
            // 判断是否本次触发
            if(task.getLoopCount() > 0) {
                // 非本次触发,loop数-1
                task.skipLoop();
                continue;
            }
            // 执行!
            log.info("task [{}] will be triggered]", task.getKey());
            scheduleTaskExecutor.submit(task.getTask());
            // 提交执行后,从时间轮中移除掉
            taskMap.remove(task.getKey());
            iter.remove();
        }
    }

    /**
     * 向时间轮中提交任务,并在指定的时间执行
     * @param key 任务唯一id
     * @param task 待执行task
     * @param time 执行时间点
     */
    public <T> void submitTask(String key, Callable<T> task, LocalDateTime time) throws SubmitFailedException {
        // 计算timestamp
        Long timeMill = DateFormatUtils.localDateTime2Mills(time);
        submit(key, task, timeMill);
    }

    /**
     * 向时间轮中提交任务,并在指定的time unit后执行.注意,如果delay为0,则会在下一个周期触发
     * @param key 任务唯一id
     * @param task 待执行task
     * @param delayTime 延迟时间
     * @param unit 时间单位
     */
    public <T> void submitTask(String key, Callable<T> task, int delayTime, ChronoUnit unit) throws SubmitFailedException {
        LocalDateTime nowTriggerTime = DateFormatUtils.timestamp2LocalDatetime(this.nowTriggerTime);
        // 计算一个tick有多少秒, 进行周期换算
        long tickCount = interval / (unit.getDuration().getSeconds() * 1000);
        LocalDateTime targetTriggerTime = nowTriggerTime.plus((delayTime + 1) * tickCount , unit);
        log.info("target trigger time :{}", targetTriggerTime);
        submit(key, task, DateFormatUtils.localDateTime2Mills(targetTriggerTime));
    }

    /**
     * 向时间轮提交任务,在指定的时间戳执行
     * @param key 任务唯一id
     * @param task 待执行task
     * @param triggerTime 运行的具体时间戳
     * @throws SubmitFailedException 任务提交失败异常,任务信息可能不合法
     */
    private <T> void submit(String key, Callable<T> task, Long triggerTime) throws SubmitFailedException {

        if(StringUtils.isEmpty(key)) {
            throw new SubmitFailedException("Submit task failed cause by task key is empty");
        }
        if(null == task || null == triggerTime) {
            log.error("Pre submit task or trigger time is null! task key :{}", key);
            throw new SubmitFailedException(String.format("Submit task [%s] failed cause by task or trigger time is null", key));
        }

        // 当前执行的tick和具体执行相差的时间
        long delay = triggerTime - nowTriggerTime;
        if(delay <= 0){
            log.error("the trigger time of task [{}] has passed: {}",key, DateFormatUtils.stdDateFormat(triggerTime));
            throw new SubmitFailedException("Submit task [%s] failed cause by the trigger time has passed");
        }
        // 对delay进行修正,必须是interval的整数倍,如果有超出则后延
        if(delay % interval > 0) {
            delay += interval - delay % interval;
        }
        // 时间轮转一圈所需要的时间
        long loopTime = (long) interval * slots.length;
        // 计算出相差多少轮
        int loopCount = (int) (delay / loopTime);
        // 具体要插入到以当前执行的slot开始向后数 delay与前面周期的差值 / 每个slot需要的时间 的索引处
        int pos = (curSlot + (int)(delay - loopTime * loopCount) / interval) % slots.length;

        ScheduleTask<T> scheduleTask = new ScheduleTask<>(key, task, pos, loopCount);
        LinkedList<ScheduleTask> tasks = this.slots[pos].getScheduleTasks();

        try {
            // 在修改时不允许读,防止到了下个周期导致misfire
            this.lock.writeLock().lock();
            tasks.add(scheduleTask);
            taskMap.put(key, scheduleTask);

        }finally {
            this.lock.writeLock().unlock();
        }
    }

}
