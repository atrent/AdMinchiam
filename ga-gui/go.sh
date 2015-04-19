#!/bin/bash

astyle *java

if
 javac *java
then
 java GitAnnexGUI /home/atrentini/Media-annex
fi
