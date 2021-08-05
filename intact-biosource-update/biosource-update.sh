#!/bin/sh

# Runs the biosource update
#
# Usage $0 database
#
#
##################################################################

if [ $# -ne 1 ]; then
      echo ""
      echo "ERROR: wrong number of parameters ($#)."
      echo "usage: $0 DATABASE_PROFILE[intactst, intacpro, intacdev]"
      echo ""
      exit 1
fi

DATABASE=$1


# Make sure we are using institution intact by default.

mvn clean install -Pbiosource-update,oracle -Ddatabase=${DATABASE} -Dmaven.test.skip=true