# usage: get_pid token_1 ... token_n
function get_pid() {
  if [ $# -lt 1 ]; then
    return
  fi
  local command="ps x";
  for token in $*; do
    command="$command | grep '$token'"
  done
  sh -c "$command | grep -v 'sh -c' | grep  -v bash | grep -v get_pid | grep -v grep | awk '{print \$1}'"
}

# usage: with_time_stamp cmd
# not reentran
function with_time_stamp() {
  if [ $# -lt 1 ]; then
    return
  fi
  # not reentran
  if [ "$WITH_TIME_STAMP_FLAG" = "ON" ]; then
      $*
      return
  fi
  export WITH_TIME_STAMP_FLAG="ON"
  # $* | awk '{print strftime("[%Y-%m-%d %H:%M:%S]"),$0}'
  # awk.strftime not work on macos
  $* 2>&1 | while read line
  do
    echo $(date "+[%Y-%m-%d %H:%M:%S]")" $line"
  done
  unset WITH_TIME_STAMP_FLAG
}

# usage: stop_process token_1 ... token_n
function stop_process() {
  if [ $# -lt 1 ]; then
    return
  fi
  local pid=$(get_pid $*)
  if [ -z "$pid" ] ; then
    echo "application [$*] is not running, skip!"
    return
  fi
  echo "start kill app: [$*] with pid: [$pid]!"
  while [ true ]; do
    pid=$(get_pid $*)
    if [ -z "$pid" ] ; then
      break
    fi
    echo "kill pid: [$pid] ..."
    # todo
    kill $pid
    sleep 5
  done
  echo "end kill app: [$*]!"
}

# usage: wait_http_ok
function wait_http_ok() {
  if [ -z "$1" ] ; then
    return
  fi
  local check_url=$1
  echo "wait until the http ok for: [$check_url]"
  for i in {1..6} ; do
    sleep 10
    local http_status=$(curl -I -m 10 -o /dev/null -s -w %{http_code} "$check_url")
    echo "http return status: [$http_status] ..."
    if [ $http_status -eq 200 ] ; then
      echo "http already ok for: [$check_url]"
      return
    fi
  done
  echo "http check timeout for: [$check_url]"
  exit 1
}

# usage: clear_history_app_dir app_parent_dir
# clear history app dir, eg: app.20200418.141939; retain top 3;
function clear_history_app_dir() {
  if [ -z "$1" ] ; then
    return
  fi
  local app_parent_dir=$1
  local count=$(ls -tr $app_parent_dir | grep ^app | wc -l)
  let delete_count=count-3
  if [ $delete_count -gt 0 ] ; then
    for app_dir in $(ls -tr $app_parent_dir | grep ^app | head -$delete_count); do
      echo "delete history app dir: [$app_parent_dir/$app_dir]"
      rm -rf "$app_parent_dir/$app_dir"
    done
  fi
}

# usage: create_app_dir app_parent_dir
# app dir, eg: app.20200418.141939
function create_app_dir() {
  if [ -z "$1" ] ; then
    return
  fi
  local app_dir=$1/$(date "+app.%Y%m%d.%H%M%S")
  mkdir -p "$app_dir"
  echo $app_dir
}

# usage: with_tmp_cd command...
function with_pwd_recovery() {
  local old_pwd=$(pwd)
  $*
  cd $old_pwd
}

# usage: derive_config app_dir values_file
function derive_config() {
  if [ -z "$1" ] ; then
    return
  fi
  if [ -z "$2" ] ; then
    return
  fi
  app_dir=$1
  values_file=$2
  mkdir -p $app_dir/config/config_derive_output
  $app_dir/bin/helm template $app_dir/config/config_derive --output-dir $app_dir/config/config_derive_output -f $app_dir/config/config_derive/$values_file
  mkdir -p $app_dir/BOOT-INF/classes
  cp -rf $app_dir/config/config_derive_output/config_derive/templates/* $app_dir/BOOT-INF/classes/
  # with_pwd_recovery cd $app_dir && jar -uf $app_dir/bin/ezproject-web-*.jar BOOT-INF/classes
  with_pwd_recovery cd $app_dir && zip -r $app_dir/bin/ezproject-web-*.jar BOOT-INF
}

alias log='with_time_stamp echo "$*"'
alias log_info='with_time_stamp echo "[INFO]$*"'
alias log_warn='with_time_stamp echo "[WARN]$*"'
alias log_error='with_time_stamp echo "[ERROR]$*"'
