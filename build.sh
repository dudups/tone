set -xe

mvn -U clean package
mkdir -p target/bin target/lib target/config

cp -r ezproject-web/output/ezproject-web-1.0-SNAPSHOT.jar target/lib/
cp -r ezproject-web/op/* target/bin/
cp -r  ezproject-web/helm/config_derive target/config/
tar -zcvf target.tar.gz target/*
