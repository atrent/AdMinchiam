#!/bin/bash

clear

if
 test $# -eq 1
then
 java GitAnnexGUI $1
else
 echo "Missing parameter (git-annex repo path)!"
 exit 1
fi
