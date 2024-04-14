FROM hub.kce.ksyun.com/ezone-public/openjdk:8u302-jre-alpine3.15

ARG TARGETARCH

LABEL maintainer=ezone
LABEL description=ezproject
LABEL version="1.0"

RUN mkdir -p /app/bin /app/config/config_derive /app/logs
COPY op/docker_run.sh /app/bin/
COPY op/functions.sh /app/bin/
COPY op/helm-${TARGETARCH} /app/bin/
RUN mv /app/bin/helm-${TARGETARCH} /app/bin/helm

COPY helm/config_derive /app/config/config_derive/
COPY entrypoint.sh /app/
COPY output/ezproject-web-1.0-SNAPSHOT.jar /app/bin/

EXPOSE 8501
WORKDIR /app

ENV TZ=Asia/Shanghai
ENTRYPOINT ["/bin/sh", "/app/entrypoint.sh"]
CMD ["local"]