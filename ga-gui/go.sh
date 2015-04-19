#!/bin/bash

astyle -A2 *java

if
 javac *java
then
 java GitAnnexGUI /home/atrentini/Media-annex
fi
