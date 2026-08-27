Java 线程的状态、流转及对应的调试检查方法整理如下：

线程的 6 种状态

Java 线程状态定义在 java.lang.Thread.State 枚举中，共 6 种：
状态   含义   触发方式
NEW   线程对象已创建，但尚未调用 start()   new Thread()

RUNNABLE   可运行（包含操作系统的就绪+运行中）   start() / 被唤醒后重新竞争到 CPU

BLOCKED   等待获取 synchronized 监视器锁   尝试进入已被其他线程持有的同步块

WAITING   无限期等待，需被显式唤醒   Object.wait() / Thread.join() / LockSupport.park()

TIMED_WAITING   超时等待，到期后自动恢复   Thread.sleep(ms) / Object.wait(ms) / Thread.join(ms)

TERMINATED   线程执行完毕或异常退出   run() 方法正常结束或抛出未捕获异常

状态流转图

                    start()
    NEW ──────────────────────► RUNNABLE ◄──────────────┐
                                    │  │                 │
                    等待获取锁       │  │ sleep(ms)       │ 锁释放 / 被唤醒
                                    ▼  │ wait(ms)        │ / join超时 / 中断
                                 BLOCKED│ join(ms)       │
                                    ▲  │                 │
                                    │  ▼                 │
                                    │ TIMED_WAITING ─────┘
                                    │
                    等待获取锁       │  │ wait()          │
                                    ▼  │ join()          │
                                 BLOCKED│ park()         │
                                    ▲  │                 │
                                    │  ▼                 │
                                    │ WAITING ───────────┘
                                    │
                                    │  run()结束 / 异常
                                    ▼
                               TERMINATED

关键区分：BLOCKED 是"主动竞争锁"，WAITING 是"主动释放锁后被动等待唤醒"。

代码验证示例

public class ThreadStateDemo {
public static void main(String[] args) throws InterruptedException {
Object lock = new Object();

        Thread t = new Thread(() -> {
            synchronized (lock) {
                try {
                    Thread.sleep(1000);   // RUNNABLE → TIMED_WAITING
                    lock.wait();          // TIMED_WAITING → WAITING（释放锁）
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        // 1. NEW
        System.out.println("创建后: " + t.getState());          // NEW

        t.start();
        Thread.sleep(100);
        // 2. TIMED_WAITING（sleep 中）
        System.out.println("sleep中: " + t.getState());         // TIMED_WAITING

        Thread.sleep(1000);
        // 3. WAITING（wait 中）
        System.out.println("wait中: " + t.getState());          // WAITING

        synchronized (lock) {
            lock.notify();
            Thread.sleep(100);
            // 4. BLOCKED（被唤醒但锁被主线程持有）
            System.out.println("notify后: " + t.getState());    // BLOCKED
        }

        Thread.sleep(100);
        // 5. TERMINATED
        System.out.println("结束后: " + t.getState());          // TERMINATED
    }
}

调试检查方法

方法一：代码中打印状态

System.out.println(thread.getState());  // 直接获取线程状态枚举

方法二：jstack 命令行（生产环境首选）

获取 Java 进程 PID
jps

导出线程转储
jstack <PID> > thread_dump.txt

输出示例及解读：

"main" #1 prio=5 os_prio=0 tid=0x00007f... nid=0x1234 waiting on condition [0x00007f...]
java.lang.Thread.State: TIMED_WAITING (sleeping)
at java.lang.Thread.sleep(Native Method)
at com.example.MyClass.run(MyClass.java:25)

"Thread-0" #2 prio=5 os_prio=0 tid=0x00007f... nid=0x5678 waiting for monitor entry [0x00007f...]
java.lang.Thread.State: BLOCKED (on object monitor)
waiting to lock <0x000000076ab90e20> (a java.lang.Object)
at com.example.MyClass.syncMethod(MyClass.java:30)

关键标识解读：
waiting on condition → WAITING / TIMED_WAITING
waiting for monitor entry → BLOCKED（在等锁）
parking to wait for → WAITING（LockSupport.park）

方法三：jconsole / VisualVM（可视化）

jconsole    # 打开后选择目标进程 → Threads 标签页
jvisualvm   # 打开后附加进程 → Threads 标签页

可以实时看到每个线程的状态、堆栈和锁持有情况。

方法四：IDE 调试器

IntelliJ IDEA：
调试模式下打开 Threads 面板，查看每个线程的状态图标和堆栈
右键 → Get Thread Dump 导出线程转储
支持冻结/解冻线程，控制执行顺序

Eclipse：
调试视图中的 Thread 视图 实时显示线程状态
支持条件断点（按线程 ID 或变量值暂停）

Visual Studio：
Parallel Stacks 窗口可视化线程调用栈
Threads 窗口查看线程列表和状态
支持冻结/解冻线程排查死锁

方法五：Arthas（线上诊断神器）

查看所有线程状态概览
thread

查看指定线程详情
thread <线程ID>

死锁检测
thread -b

按状态过滤（如只看 BLOCKED）
thread --state BLOCKED

常见问题排查对照表
现象   可能状态   排查方向
程序卡死不响应   BLOCKED / WAITING   jstack 看锁竞争链

CPU 飙高   RUNNABLE   jstack 找持续 RUNNABLE 的线程，看是否在死循环

线程数持续增长   NEW → RUNNABLE 泄漏   检查线程池配置，是否有线程未正确回收

响应越来越慢   TIMED_WAITING 堆积   检查连接池/资源池是否耗尽

两个线程互相等待   BLOCKED（死锁）   jstack 会自动检测并报告死锁

💡 实战建议：遇到线程问题，第一步永远是 jstack <PID>，看线程转储中各线程的状态和堆栈，90% 的并发问题都能快速定位。

需要我针对某个具体场景（如死锁排查、线程池调优）展开详细说明吗？