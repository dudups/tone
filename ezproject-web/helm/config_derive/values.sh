helm template ./ --output-dir ./output -f values-local.yaml
cp -rf ./output/config_derive/templates/* ../../src/main/resources
