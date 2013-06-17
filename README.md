SikuliX-Jython 1.0.1
====================

This is **fully Maven**, so a fork of this repo can be directly used as project in NetBeans/Eclipse/...<br />
or using mvn on commandline. Currently it needs a valid SikuliX-API in your local Maven repo.

It supplies the Jython script runner support for the SikuliX-API.<br />
This project depends on SikuliX-API and provides a Python interface for the java level API to beused 

SikuliX-Jython implements the **IScriptRunner** interface, that is loaded by SikuliX-API, when you run Sikuli scripts written in Python language.

Use **mvn [clean] [package | install]** with the SikuliX-API artifact in your local maven repository to create **sikuli-jython.jar**
in the [target folder | local repo], which contains the the latest jython (currently 2.5.4rc1) and the package Lib/sikuli (for: from sikuli import *).

This jar is not executable (use sikuli-script.jar from the SikuliX-API package to run your Sikuli scripts).
