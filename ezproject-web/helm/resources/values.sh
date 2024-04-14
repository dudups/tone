helm template ./ --output-dir ./output
cp -rf ./output/resources/templates/* ../../src/main/resources