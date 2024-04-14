pwd
ls bin
source bin/functions.sh

# usage: start ${env}
function start() {
  if [ $# -lt 1 ]; then
    return
  fi
  local env=$1
  derive_config /app values-${env}.yaml
  mkdir -p /app/logs
  java -jar -Drocketmq.client.logLevel=ERROR \
    -Dfile.encoding=UTF-8 \
    $JAVA_MEM_OPTS \
    $JAVA_DEBUG_OPTS \
    $JAVA_APM_OPTS \
    -server \
    -XX:MetaspaceSize=200m \
    -XX:MaxMetaspaceSize=256m \
    -XX:+DisableExplicitGC \
    -XX:+UseConcMarkSweepGC \
    -XX:+CMSParallelRemarkEnabled \
    -XX:+UseCMSCompactAtFullCollection \
    -XX:LargePageSizeInBytes=128m \
    -XX:+UseFastAccessorMethods \
    -XX:+UseCMSInitiatingOccupancyOnly \
    -XX:CMSInitiatingOccupancyFraction=70 \
    bin/ezproject-web-1.0-SNAPSHOT.jar \
    </dev/null &>/app/logs/$(date +%F_%H-%M-%S).$(hostname).log
}

start $*