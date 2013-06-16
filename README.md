SikuliX-Jython 1.0.1
====================

The Jython script runner support for the SikuliX-API. 
This project depends on SikuliX-API and provides a Python interface for java level API.

SikuliX-Jython implements the **IScriptRunner** interface that is loaded by SikuliX-API, when you run Sikuli scripts written in Python language.

Use **mvn install** with the SikuliX-API artifact in your maven repository to create sikuli-jython.jar
which contains the the latest jython (currently 2.5.4rc1) and the package Lib/sikuli (for: from sikuli import *).

This jar is not executable (use sikuli-script.jar from the SikuliX-API package).
