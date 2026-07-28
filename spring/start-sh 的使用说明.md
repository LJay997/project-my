# 进入项目目录
cd /Users/jay/Documents/ideaProject/demo/project-my/spring

# 启动应用（选择模式）
./start.sh           # 基础模式
./start.sh cpu       # CPU测试模式（推荐）
./start.sh jmx       # JMX监控模式
./start.sh gc        # GC测试模式

# 停止应用
./start.sh stop

# CPU压力测试示例
curl 'http://localhost:8082/cpu/loop?threads=10&duration=30'
curl 'http://localhost:8082/cpu/busy?threads=10&duration=60'

# 查看GC日志
tail -f logs/gc-cpu.log

# 查看应用日志
tail -f logs/app-cpu.log