# Linux 命令面试总结

> 覆盖高频 Linux 命令、性能排查、Shell 脚本、及面试常考内容

---

## 目录

- [1. 文件与目录操作](#1-文件与目录操作)
- [2. 文本处理三剑客](#2-文本处理三剑客)
- [3. 权限管理](#3-权限管理)
- [4. 进程与性能排查](#4-进程与性能排查)
- [5. 网络命令](#5-网络命令)
- [6. 磁盘与内存](#6-磁盘与内存)
- [7. Shell 脚本基础](#7-shell-脚本基础)
- [8. 高频面试题](#8-高频面试题)

---

## 1. 文件与目录操作

### 1.1 基础命令

```bash
# 目录操作
ls -la              # 列出所有文件（含隐藏）
ls -lh              # 人类可读大小
ls -lt              # 按时间排序
cd -                # 回到上一个目录
pwd                 # 当前目录
mkdir -p a/b/c      # 递归创建目录
rm -rf dir          # 强制递归删除（慎用！）

# 文件操作
cp -r source dest   # 递归复制
mv old new          # 移动/重命名
touch file.txt      # 创建空文件或更新时间戳
ln -s target link   # 创建软链接
ln target link      # 创建硬链接

# 查看文件
cat file.txt        # 全量输出
head -n 20 file     # 前 20 行
tail -f log.txt     # 实时追踪日志
tail -n 100 file    # 最后 100 行
less file           # 分页查看（支持搜索）
more file           # 分页查看（仅向前）
```

### 1.2 查找命令

```bash
# find：按条件查找文件
find / -name "*.log"                      # 按名称
find . -type f -size +100M                # 大于 100M 的文件
find . -mtime -7                          # 最近 7 天修改的文件
find . -name "*.log" -exec rm {} \;       # 查找并删除
find . -type f -empty                     # 空文件

# which：查找命令位置
which java

# whereis：查找命令及手册位置
whereis java

# locate：基于数据库快速查找（需 updatedb）
locate nginx.conf
```

---

## 2. 文本处理三剑客

### 2.1 grep

```bash
grep "ERROR" app.log                      # 搜索关键字
grep -i "error" app.log                   # 忽略大小写
grep -v "DEBUG" app.log                   # 排除匹配
grep -c "ERROR" app.log                   # 统计匹配行数
grep -A 3 "ERROR" app.log                 # 显示匹配行后 3 行
grep -B 3 "ERROR" app.log                 # 显示匹配行前 3 行
grep -C 3 "ERROR" app.log                 # 显示匹配行前后各 3 行
grep -r "TODO" ./src/                     # 递归搜索目录
grep -n "ERROR" app.log                   # 显示行号
grep -E "ERROR|WARN" app.log              # 正则（多关键字）
grep -E "2024-01-1[5-9]" app.log          # 正则日期范围
```

### 2.2 awk

```bash
# 按列处理
awk '{print $1, $3}' file                 # 打印第 1、3 列
awk -F ':' '{print $1}' /etc/passwd       # 指定分隔符
awk '{if($3>100) print $0}' file          # 条件过滤

# 统计
awk '{sum+=$2} END {print sum}' file      # 求和
awk '{count++} END {print count}' file    # 计数
awk '{print $1}' access.log | sort | uniq -c | sort -rn | head -10  # Top N IP

# 实战：Nginx 日志分析
awk '{print $1}' access.log | sort | uniq -c | sort -rn | head -5
# 结果：Top 5 访问 IP

# 实战：统计接口耗时
awk '{print $NF}' access.log | sort -rn | head -10
```

### 2.3 sed

```bash
sed 's/old/new/g' file                    # 全局替换（仅输出）
sed -i 's/old/new/g' file                 # 直接修改文件
sed '/pattern/d' file                     # 删除匹配行
sed -n '5,10p' file                       # 打印第 5-10 行
sed -i 's/^#//g' file                     # 去掉注释
sed 's/[[:space:]]*$//' file              # 去掉行尾空格
```

### 2.4 组合实战

```bash
# 查找日志中 ERROR 出现次数最多的接口
grep ERROR app.log | awk '{print $5}' | sort | uniq -c | sort -rn | head -10

# 统计 JAVA 进程 CPU 使用率 Top 5
ps aux | grep java | sort -k3 -rn | head -5

# 查找所有包含 TODO 的 Java 文件
grep -r "TODO" --include="*.java" ./src/

# 统计代码行数
find . -name "*.java" | xargs wc -l | tail -1
```

---

## 3. 权限管理

```bash
# 查看权限
ls -la                     # rwx rwx rwx = 用户 组 其他
stat file                  # 详细权限信息

# 修改权限
chmod 755 script.sh        # rwx r-x r-x
chmod u+x script.sh        # 用户加执行权限
chmod -R 755 dir/          # 递归修改

# 修改所有者
chown user:group file
chown -R user:group dir/

# 数字权限对照
# 4=r  2=w  1=x
# 7=rwx  6=rw  5=rx  4=r
# 755 = rwxr-xr-x（目录/脚本）
# 644 = rw-r--r--（普通文件）
# 600 = rw-------（私钥文件）
```

---

## 4. 进程与性能排查

### 4.1 进程查看

```bash
ps aux                     # 所有进程
ps -ef | grep java         # 查找 Java 进程
top                        # 实时进程监控
htop                       # 增强版 top
pstree -p                  # 进程树

# 查看线程
top -H -p <pid>            # 查看进程的所有线程
ps -T -p <pid>             # 查看进程的线程列表
```

### 4.2 CPU 飙升排查

```bash
# 1. 找到 CPU 最高的进程
top -c

# 2. 找到该进程中最高的线程
top -H -p <pid>
# 或
ps -mp <pid> -o THREAD,tid,time | sort -rn | head -10

# 3. 线程 ID 转十六进制
printf "%x\n" <tid>

# 4. 查看线程堆栈（jstack）
jstack <pid> | grep -A 30 <hex_tid>

# 5. 统计线程状态
jstack <pid> | grep "java.lang.Thread.State" | sort | uniq -c
```

### 4.3 内存排查

```bash
free -h                    # 内存使用概览
free -m                    # 以 MB 显示

# 进程内存排序
ps aux --sort=-%mem | head -10

# Java 堆内存
jmap -heap <pid>           # 堆概要
jmap -histo <pid> | head -20  # 对象统计
jmap -dump:format=b,file=heap.hprof <pid>  # 堆转储

# 查看 GC 情况
jstat -gc <pid> 1000 10    # 每秒打印一次，共 10 次
jstat -gcutil <pid> 1000   # GC 利用率
```

### 4.4 综合排查

```bash
# 查看系统负载
uptime                     # 1/5/15 分钟平均负载
vmstat 1 5                 # 虚拟内存统计

# 查看 IO
iostat -x 1                # 磁盘 IO 统计
iotop                      # 进程 IO 排行

# 查看文件句柄
lsof -p <pid>              # 进程打开的文件
lsof -i :8080              # 查看端口占用
```

---

## 5. 网络命令

```bash
# 网络状态
ping -c 4 www.baidu.com        # 连通性测试
curl -v http://localhost:8080  # HTTP 请求详情
curl -X POST -H "Content-Type: application/json" -d '{"key":"value"}' http://localhost:8080/api

# 端口与连接
netstat -tlnp                   # 监听端口
netstat -anp | grep 8080        # 指定端口连接
ss -tlnp                        # 替代 netstat（更快）
lsof -i :8080                   # 端口被谁占用

# DNS 排查
nslookup www.baidu.com
dig www.baidu.com

# 路由追踪
traceroute www.baidu.com

# 防火墙
iptables -L -n                  # 查看规则
firewall-cmd --list-ports       # firewalld 开放端口
```

---

## 6. 磁盘与内存

```bash
# 磁盘
df -h                           # 磁盘使用概览
du -sh *                        # 当前目录各文件/目录大小
du -sh /var/log/                # 指定目录大小
du -h --max-depth=1 /           # 一级目录大小

# 找大文件
find / -type f -size +500M 2>/dev/null
du -ah / | sort -rh | head -20

# 内存
free -h
cat /proc/meminfo               # 详细内存信息
vmstat -s                       # 内存统计

# 文件系统
mount                           # 已挂载文件系统
fdisk -l                        # 磁盘分区
lsblk                           # 块设备列表
```

---

## 7. Shell 脚本基础

### 7.1 常用语法

```bash
#!/bin/bash
set -e                          # 遇到错误立即退出

# 变量
NAME="hello"
echo ${NAME}
echo "当前脚本: $0"
echo "参数个数: $#"
echo "所有参数: $@"
echo "第一个参数: $1"

# 条件判断
if [ -f "$file" ]; then
    echo "文件存在"
elif [ -d "$dir" ]; then
    echo "是目录"
else
    echo "不存在"
fi

# 数值比较
if [ $a -gt $b ]; then          # -gt/-lt/-eq/-ne/-ge/-le
    echo "大于"
fi

# 循环
for i in {1..10}; do
    echo $i
done

for file in *.log; do
    echo "处理: $file"
done

while read line; do
    echo $line
done < file.txt

# 函数
function log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}
log "开始执行"
```

### 7.2 实用脚本

```bash
#!/bin/bash
# 一键部署 Spring Boot 应用

APP_NAME="my-app"
JAR_PATH="/app/${APP_NAME}.jar"
LOG_PATH="/app/logs/${APP_NAME}.log"

# 停止旧进程
PID=$(ps -ef | grep ${JAR_PATH} | grep -v grep | awk '{print $2}')
if [ -n "$PID" ]; then
    kill -15 $PID
    sleep 5
    # 强制 kill
    if ps -p $PID > /dev/null; then
        kill -9 $PID
    fi
fi

# 启动
nohup java -Xms512m -Xmx1024m -jar ${JAR_PATH} > ${LOG_PATH} 2>&1 &

echo "启动完成，PID: $!"
tail -f ${LOG_PATH}
```

---

## 8. 高频面试题

### 8.1 基础题

**Q1: 如何查看 Linux 系统的 CPU 和内存信息？**

```bash
# CPU
lscpu                       # CPU 架构信息
cat /proc/cpuinfo | grep processor | wc -l  # CPU 核心数
top -bn1 | grep "Cpu(s)"    # CPU 使用率

# 内存
free -h
cat /proc/meminfo | grep MemTotal
```

**Q2: 硬链接和软链接的区别？**

| 对比维度   | 硬链接                         | 软链接（符号链接）               |
| ---------- | ------------------------------ | -------------------------------- |
| 本质       | 同一文件的不同名字               | 指向目标路径的快捷方式             |
| inode      | 相同 inode                     | 不同 inode                       |
| 跨文件系统 | 不支持                         | 支持                             |
| 源删除后   | 仍可访问                       | 失效（断链）                     |
| 创建命令   | `ln target link`               | `ln -s target link`              |

**Q3: 如何查看文件内容的第 100-200 行？**

```bash
sed -n '100,200p' file.txt
head -200 file.txt | tail -101
awk 'NR>=100 && NR<=200' file.txt
```

**Q4: 如何查看端口被哪个进程占用？**

```bash
lsof -i :8080
netstat -tlnp | grep 8080
ss -tlnp | grep 8080
```

### 8.2 进阶题

**Q5: 线上 CPU 100% 如何排查？**

```bash
# 1. 找到 CPU 最高的进程
top -c

# 2. 找到该进程中最高的线程
top -H -p <pid>

# 3. 线程 ID 转十六进制
printf "%x\n" <tid>

# 4. 查看线程堆栈
jstack <pid> | grep -A 30 <hex_tid>

# 5. 分析原因：死循环？死锁？GC 频繁？
```

**Q6: 如何查看日志中某个时间段的记录？**

```bash
# 方法一：sed 截取时间段
sed -n '/2024-01-15 10:00/,/2024-01-15 11:00/p' app.log

# 方法二：awk 过滤
awk '/2024-01-15 10:00/,/2024-01-15 11:00/' app.log

# 方法三：grep 日期范围
grep -E "2024-01-15 (1[0-1]:[0-5][0-9])" app.log
```

**Q7: 如何统计一个文件中某个关键字出现的次数？**

```bash
grep -c "关键字" file.txt
grep -o "关键字" file.txt | wc -l
```

**Q8: 如何批量 kill 包含某个关键字的进程？**

```bash
ps -ef | grep "关键字" | grep -v grep | awk '{print $2}' | xargs kill -9
# 或
pkill -f "关键字"
```

### 8.3 高级题

**Q9: 服务器磁盘满了怎么排查？**

```bash
# 1. 查看磁盘使用
df -h

# 2. 定位大目录
du -sh /* 2>/dev/null | sort -rh | head -10

# 3. 逐层深入
du -sh /var/* | sort -rh | head -10

# 4. 找大文件
find /var -type f -size +100M 2>/dev/null

# 5. 常见元凶
# - 日志文件未轮转（/var/log）
# - Docker 镜像/容器堆积（/var/lib/docker）
# - 大型 core dump 文件
# - 数据库数据文件
```

**Q10: 一个 Java 进程突然消失了，怎么排查？**

```bash
# 1. 查看系统日志
dmesg | tail -50
dmesg | grep -i oom          # 是否被 OOM Killer 杀掉

# 2. 查看进程日志
journalctl -u my-app --since "10 minutes ago"

# 3. 查看 /var/log/messages
grep -i "killed" /var/log/messages

# 4. 检查 crontab 定时任务
crontab -l

# 5. 检查是否有 kill 命令记录
history | grep kill
```

**Q11: nohup 和 & 的区别？**

| 对比       | `&`                          | `nohup`                         |
| ---------- | ---------------------------- | ------------------------------- |
| 作用       | 后台运行                     | 忽略 SIGHUP 信号                 |
| 终端关闭后 | 进程终止                     | 进程继续运行                     |
| 输出       | 默认输出到终端               | 默认输出到 nohup.out             |
| 最佳实践   | `nohup command > log 2>&1 &` | 两者结合使用                     |

**Q12: 如何查看系统过去一段时间的负载？**

```bash
uptime                          # 1/5/15 分钟平均负载
top -bn1 | head -5              # 快照式查看
sar -q                          # 历史负载记录（需 sysstat）
```

---

> **速记口诀**
> - **找文件**：find → 按条件；locate → 按名称
> - **看日志**：tail -f → 实时；grep → 过滤；awk → 取列；sed → 替换
> - **查性能**：top → 进程；free → 内存；df → 磁盘；iostat → IO
> - **查网络**：ping → 连通；curl → HTTP；netstat/ss → 端口；lsof → 占用