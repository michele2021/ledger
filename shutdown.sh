#!/bin/bash

# shutdown.sh has been implemented to automate process of stopping and removing docker
# containers provisioned by the following stacks => accounting_stack.yml and services_stack.yml

# By default script does not remove attached volumes, so the data can be persisted between the restarts
# It is possible tho to remove the persistence layer by adding "--volume" or "-v" flag.

NC='\033[0m'
INFO="\033[0;36mINFO: ${NC}"
ERROR="\033[0;31mERROR: ${NC}"

help() {
    echo -e "--stop     alias -s   =>   stops accounting core stack"
    echo -e "--volumes  alias -v   =>   destroy all attached docker volumes"
}


destroyInfra() {
  echo -e "${INFO}Destroying localstack infrastructure."
  terraform -chdir=./local-infra/terraform destroy -auto-approve
}

TIMEOUT=10

stopAccountingCoreStack(){
  echo -e "${INFO}Stopping accounting core stack"
  docker-compose -f "./local-infra/docker/accounting_stack.yml" stop --timeout="${TIMEOUT}"
}

destroyStacks(){
  destroyInfra
  echo -e "${INFO}Destroying all stacks"
  docker-compose -f "./local-infra/docker/accounting_stack.yml" down --timeout="${TIMEOUT}" ; \
  docker-compose -f "./local-infra/docker/services_stack.yml" down --timeout="${TIMEOUT}"
  [ -d "./local-infra/localstack_tmp" ] && rm -r ./local-infra/localstack_tmp
}

destroyStacksAndVolumes() {
  destroyInfra
  echo -e "${INFO}Destroying all stacks and related volumes"
  docker-compose -f "./local-infra/docker/accounting_stack.yml" down --volumes ; \
  docker-compose -f "./local-infra/docker/services_stack.yml" down --volumes
  [ -d "./local-infra/localstack_tmp" ] && rm -r ./local-infra/localstack_tmp
}


if [ "$#" -eq "0" ]; then destroyStacks; exit 0
elif [ "$1" == "--help" ] || [ "$1" == "-h" ]; then help; exit 0
elif [ "$1" == "--volumes" ] || [ "$1" == "-v" ] || [ "$1" == "-V" ]; then destroyStacksAndVolumes
elif [ "$1" == "--stop" ] || [ "$1" == "-s" ]; then stopAccountingCoreStack
else echo -e "${ERROR}Invalid param"
fi

