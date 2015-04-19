#!/bin/bash

clear

astyle -A2 *java

if
 javac *java
 #javac -Xlint:unchecked *java
then
 java GitAnnexGUI /home/atrentini/Media-annex
fi
