SikuliX-Jython 1.0.1
====================

This is **fully Maven**, so a fork of this repo can be directly used as project in NetBeans/Eclipse/...<br />
or using mvn on commandline. Currently it needs a valid SikuliX-API in your local Maven repo.

It supplies the Jython script runner support for the SikuliX-API.<br />
This project depends on SikuliX-API and provides a Python interface for the java level API to be used 

SikuliX-Jython implements the **IScriptRunner** interface, that is loaded by SikuliX-API, when you run Sikuli scripts written in Python language.

Use **mvn [clean] [package | install]** with the SikuliX-API artifact in your local maven repository will create in the [target folder | local repo]:

- **sikulix-jython.jar**<br />
 - is not executable, must be on classpath and needs sikulix-api.jar on classpath
 - it contains the Jython script runner and the package Lib/sikuli (for: from sikuli import *)
 - it can be used inside or outside IDEs like Netbeans, Eclipse with a Jython installation
 - sikulix-jython.jar/Lib must be in the Python path at runtime

- **sikuli-script-jar-with-dependencies.jar**<br />
 - is executable and is feature compatible to the known sikuli-script.jar. 
 - it can be used standalone (needs only Java), to run Sikuli scripts written in Jython.
 - might be renamed to sikuli-script.jar for convenience and downward compatibility
