# env: ES_PREFIX
# usage: endpoint
if [ $# -lt 1 ]; then
  exit 1
fi
export ES_AUTH="$2"
sh upgrade-mapping.sh "$1" "${ES_PREFIX}project-extend" project-extend-index.json
# dev: sh upgrade.sh "http://10.1.2.235:30011" 'elastic:Elasticsearch123!@#'