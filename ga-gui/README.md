# GitAnnexGUI

sort-of-GUI for git-annex

## IDEA

* have a map of all the files
* tag them
* generate a script based on the tagged items
* copy&paste the script and execute it at your own will (and responsibility!)


## USAGE

* compile it (you need a java jdk)
* run it using 'java GitAnnexGUI <path of annex>' (it takes time if your annex contains a large number of files, test it on small annexes at first)
* select items (on "remotes" columns)
* select a script from templates (you can add templates in the ScriptTemplates dir)
* generate the script
* copy&paste the script wherever you want

### Parameters in script templates

* {0} is remote
* {1} is filename
* {2} is progressive number
