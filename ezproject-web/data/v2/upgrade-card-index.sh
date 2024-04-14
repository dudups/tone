# usage: endpoint
if [ $# -lt 1 ]; then
  exit 1
fi
sh upgrade-index.sh "$1" project-card card-index.json
# dev: sh upgrade.sh "http://10.0.0.20:8221"