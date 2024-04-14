# usage: index_name endpoint index_name/alias_name
function index_name() {
  if [ $# -lt 2 ]; then
    exit 1
  fi
  local index_name=$(curl -u 'elastic:Elasticsearch123!@#' "$1/$2/_alias")
  index_name=${index_name#*\"}
  index_name=${index_name%%\"*}
  echo "$index_name"
}

# usage: gen_new_index_name index_name
function gen_new_index_name() {
  echo "$1"_$(date "+%Y-%m-%d_%H-%M-%S")
}

# usage: # usage: create_new_index endpoint index_name index_file
function create_new_index() {
  if [ $# -lt 3 ]; then
    exit 1
  fi
  if ! curl -u 'elastic:Elasticsearch123!@#' -X PUT -H "Content-Type: application/json" "$1/$2" -d @"$3"; then
    exit 1
  fi
  echo
}

# usage: # usage: update_index_settings endpoint index_name settings_file
function update_index_settings() {
  if [ $# -lt 3 ]; then
    exit 1
  fi
  if ! curl -u 'elastic:Elasticsearch123!@#' -X PUT -H "Content-Type: application/json" "$1/$2/_settings" -d @"$3"; then
    exit 1
  fi
  echo
}

# usage: # usage: re_index endpoint from_index to_index
function re_index() {
  if [ $# -lt 3 ]; then
    exit 1
  fi
  if ! curl -u 'elastic:Elasticsearch123!@#' -X POST -H "Content-Type: application/json" "$1/_reindex" -d \
    '{
      "source": {
        "index": "'"$2"'"
      },
      "dest": {
        "index": "'"$3"'"
      }
    }';
  then
    exit 1
  fi
  echo
}

# usage: del_index endpoint index_name
function del_index() {
  if [ $# -lt 2 ]; then
    exit 1
  fi
  if ! curl -u 'elastic:Elasticsearch123!@#' -X DELETE "$1/$2"; then
    exit 1
  fi
  echo
}

# usage: alias_index endpoint index_name index_alias
function alias_index() {
  if [ $# -lt 3 ]; then
    exit 1
  fi
  if ! curl -u 'elastic:Elasticsearch123!@#' -X PUT "$1/$2/_alias/$3"; then
    exit 1
  fi
  echo
}

# usage: endpoint index_name index_file
function upgrade_index() {
  if [ $# -lt 3 ]; then
    exit 1
  fi
  local endpoint=$1
  local index_name=$2
  local index_file=$3

  #
  local new_index_name=$(gen_new_index_name "$index_name")
  if ! create_new_index "$endpoint" "$new_index_name" "$index_file"; then
    exit 1
  fi
  if ! update_index_settings "$endpoint" "$new_index_name" reindex_settings.json; then
    exit 1
  fi
  
  #
  re_index "$endpoint" "$index_name" "$new_index_name" 
  
  #
  local real_index_name=$(index_name "$endpoint" "$2")
  if ! del_index "$endpoint" "$real_index_name"; then
    exit 1
  fi
  if ! alias_index "$endpoint" "$new_index_name" "$index_name"; then
    exit 1
  fi
  if ! update_index_settings "$endpoint" "$new_index_name" recovery_settings.json; then
    exit 1
  fi
}

# usage: endpoint index_name index_file
if upgrade_index $*; then
  echo "Succ"
else
  echo "Fail"
fi


