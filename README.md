SikuliX-Jython-API
==================

The Jython API for the SikuliX-API. 
This project depends on SikuliX-API and provides a Python interface for the Sikuli API.

SikuliX-Jython-API implements the **IScriptRunner** Interface that is loaded by SikuliX-API with the Java ServiceLoader. 

Use **mvn install** with the SikuliX-API artifact in your maven repository to create two jars:<br>

- sikuli-pyapi.jar contains only this project's compiled java files and the python files. You have to manage all dependencies yourself to run this jar.
- sikuli-script-jython.jar contains the dependencies jython.2.5.4 standalone and SikuliX-API and can run without any external dependencies.
