TOKEN=123456

# usage: # usage: run_groovy endpoint groovy_file
function run_groovy() {
  if [ $# -lt 2 ]; then
    exit 1
  fi
  local timestamp=$(date +%s)000
  local auth
  if [[ "$(uname -a)" =~ "Darwin" ]]; then
    auth=$(md5 -s "$TOKEN$timestamp" | awk '{print $4}')
  else
    auth=$(echo -n "$TOKEN$timestamp" | md5sum | awk '{print $1}')
  fi
  local response=$(curl -X POST -H "X-INTERNAL-AUTH-TIMESTAMP: $timestamp" -H "X-INTERNAL-AUTH-MD5: $auth" -H "Content-Type: text/plain" "$1/v1/ezproject/ezProject/api/system/groovy?name=$2" --data-binary @"$2")
  echo "$response"
}

# usage: endpoint groovy_file
run_groovy $*

# eg: sh runGroovy.sh "https://ezone-dev.work" UniqSchemaTargetStatus.groovy
