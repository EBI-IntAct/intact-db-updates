#!/bin/bash

#SBATCH --time=06-00:00:00   # walltime
#SBATCH --ntasks=1   # number of tasks
#SBATCH --cpus-per-task=5   # number of CPUs Per Task i.e if your code is multi-threaded
#SBATCH -p production   # partition(s)
#SBATCH --mem=48G   # memory per node
#SBATCH -J "MUTATION_UPDATE"   # job name
#SBATCH -o "/nfs/production/hhe/intact/data/mutation-feature-update-report/mutation-update-%j.out"   # job output file
#SBATCH --mail-user=intact-ebi@ebi.ac.uk   # email address
#SBATCH --mail-type=ALL

# Runs the Mutation update
#
# Usage $0 database folder
#
#
##################################################################

if [ $# -ne 3 ]; then
      echo ""
      echo "ERROR: wrong number of parameters ($#)."
      echo "usage: $0 DATABASE_PROFILE[ebi-test, ebi-prod, etc] FOLDER LOG_FILE"
      echo ""
      exit 1
fi

DATABASE=$1
FOLDER=$2
LOG=$3

mvn clean -U compile -X install -Pglobal-feature-report,${DATABASE},oracle -Dfolder=${FOLDER} -Ddatabase=${DATABASE} -Dmaven.test.skip -Dmaven.repo.local=repository

# Write automatic summary
