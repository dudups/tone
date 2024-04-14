# usage: alias_index endpoint index_name index_alias
function upgrade_mapping() {
  if [ $# -lt 3 ]; then
    exit 1
  fi
  if ! curl -u "$ES_AUTH" -X PUT -H "Content-Type: application/json" "$1/$2/_mapping" -d @"$3"; then
    exit 1
  fi
  echo
}

# usage: endpoint index_name index_file auth
function upgrade_index() {
  if [ $# -lt 3 ]; then
    exit 1
  fi
  local endpoint=$1
  local index_name=$2
  local index_file=$3

  echo 'upgrade_mapping'
  if ! upgrade_mapping "$endpoint" "$index_name" "$index_file"; then
    exit 1
  fi
}

# usage: endpoint index_name index_file
if upgrade_index $*; then
  echo "Succ"
else
  echo "Fail"
fi