# env: ES_PREFIX
# usage: endpoint
if [ $# -lt 1 ]; then
  exit 1
fi
export ES_AUTH="$2"
sh upgrade-index.sh "$1" "${ES_PREFIX}project-card-event" project-card-event-index.json
sh upgrade-index.sh "$1" "${ES_PREFIX}project-card" project-card-index.json
# dev: sh upgrade.sh "http://10.0.0.20:8221" 'elastic:Elasticsearch123!@#'