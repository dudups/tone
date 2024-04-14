source functions.sh
docker stop $(docker ps -q | grep ezproject)
docker rm $(docker ps -q | grep ezproject)
docker run -d \
  -v /Users/zhm/Documents/code/ezone-i.work/ezproject/web/logs:/app/logs \
  -p 8501:8501 \
  -p 8509:8509 \
  -e JAVA_OPTS='-server -Xmx512m -Xms512m -Xmn128m -Xss512k -XX:MetaspaceSize=64m -XX:MaxMetaspaceSize=128m
    -Dskywalking.agent.service_name=ezproject-dev -Dskywalking.collector.backend_service=10.5.0.47:11800
    -Xdebug -Xrunjdwp:transport=dt_socket,address=8509,server=y,suspend=n
    ' \
  docker-snapshot-ezone.ezone-i.work/ezone/ezproject-web:1.0 \
  dev
with_time_stamp wait_http_ok http://127.0.0.1:8501/ok