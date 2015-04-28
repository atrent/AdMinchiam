#!/bin/bash
clear
astyle -xe -f -A2 *java

sloccount *java

echo === COMPILING...
if
 #javac *java
 javac -Xlint:unchecked *java
then
 echo === RUNNING...
 java GitAnnexGUI /home/atrentini/Media-annex
fi
