#!/bin/bash

NC='\033[0m'
INFO="\033[0;36mINFO: ${NC}"
WARN="\033[0;33mWARNING: ${NC}"
ERROR="\033[0;31mERROR: ${NC}"

JAR_DIR="./build/libs/"
JAR_FILE="ledger-*.jar"

dockerBuildServices() {
  echo -e "${INFO}Building docker services stack"
  docker-compose -f "./local-infra/docker/services_stack.yml" up -d --build
}

gradleBuild() {
  echo -e "${INFO}Stopping existing Gradle daemons" && ./gradlew --stop
  echo -e "${INFO}Starting gradle shadow build" && ./gradlew clean runShadow
}

dockerBuildApp() {
  echo -e "${INFO}Building docker accounting_core stack"
  docker-compose -f "./local-infra/docker/accounting_stack.yml" up -d --build
}

fullBuild() {
  gradleBuild && dockerBuildApp
}

dockerBuildServices

terraform -chdir=./local-infra/terraform init
terraform -chdir=./local-infra/terraform apply -var-file ./dev.tfvars  -auto-approve

if [ -d "${JAR_DIR}" -a "$1" == "--build" ];
then
  fullBuild
  exit 0
fi


if [ -d "${JAR_DIR}" ]; then

  FILES_COUNT=$(find "${JAR_DIR}" -name "${JAR_FILE}" | wc -w)

  if [ "${FILES_COUNT}" -eq "1" ]; then dockerBuildApp

  elif [ "${FILES_COUNT}" -eq "0" ]; then
    echo -e "${WARN} Jar file not found"
    fullBuild

  else [ "${FILES_COUNT}" -gt "1" ];
    echo -e "${ERROR}ERROR: More than one jar file found in ${JAR_DIR}"

    echo -e "${INFO}Stopping existing Gradle daemons" && ./gradlew --stop
    echo -e "${INFO}Cleaning old builds" && ./gradlew clean
    fullBuild
  fi

else
  echo -e "${WARN}Directory '${JAR_DIR}' not found!"
  fullBuild
fi
