SikuliX-Jython 1.0.1
====================

This is **fully Maven**, so a fork of this repo can be directly used as project in NetBeans/Eclipse/...<br />
or with mvn on commandline. 

Forking only makes sense, if you want to contribute new features or patches.
<br />Use *mvn install* in the project folder to get it in your local repo.

It implements the **IScriptRunner** interface (internally used by SikuliX), to allow to run scripts written in Python language. 

It depends on [Sikuli Basics](https://github.com/RaiMan/SikuliX-Basics) and is **currently not intended for usages outside the downloadable Sikuli packages** (sikuli-script.jar and sikuli-ide.jar).

To support Jython developement in IDEs we have the **sikuli-java.jar** from the package [Sikuli API](https://github.com/RaiMan/SikuliX-API).

