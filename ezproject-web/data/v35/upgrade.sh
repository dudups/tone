# env: ES_PREFIX
# usage: endpoint
if [ $# -lt 1 ]; then
  echo 'args error'
  exit 1
fi
export ES_AUTH="$2"
sh upgrade-mapping.sh "$1" "${ES_PREFIX}project-card" project-card-index.json

# dev: sh upgrade.sh "http://10.0.0.20:8221" 'elastic:Elasticsearch123!@#'
# qa sh upgrade.sh "http://10.1.3.157:30021" 'elastic:Elasticsearch123!@#'