/*
 * Copyright 2010-2011, Sikuli.org
 * Released under the MIT License.
 *
 * modified RaiMan 2012
 */
package org.sikuli.scriptrunner;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Matcher;

import org.apache.commons.cli.CommandLine;
import org.python.util.PythonInterpreter;
import org.python.util.jython;
import org.sikuli.script.Debug;
import org.sikuli.script.IScriptRunner;
import org.sikuli.script.ScreenHighlighter;

/**
 * Executes Sikuliscripts written in Python/Jython.
 */
public class JythonScriptRunner implements IScriptRunner {

    /** The PythonInterpreter instance */
    private static PythonInterpreter interpreter = null;

    /** sys.argv for the jython script */
    private static ArrayList<String> sysargv = null;

    /**
     * Fileending for jythonscripts
     */
    private static final String FILEENDING_PYTHON = "py";

    /** The header commands, that are executed before every script */
    String[] PYTHON_SCRIPT_HEADER = new String[]{
            "# -*- coding: utf-8 -*- ",
            "import sys",
            "from __future__ import with_statement",
            "from sikuli import *",
            "resetROI()",
            "setShowActions(False)"
    };

    /**
     * CommandLine args
     */
    CommandLine cmdLine = null;

    /**
     * {@inheritDoc}
     */
    @Override
    public int runTest(File scriptfile, File imagepath, String[] argv) {
        Debug.error("Sikuli Test Feature is not implemented at the moment");
        return -1;
    }

    /**
     * Retrieves the actual file
     * @param scriptfile The File that can be the ScriptFile or the directory containing the ScriptFile.
     * @return The File containing the actual script.
     */
    private File getScriptFile(File scriptfile) {
        if (scriptfile == null) {
            return null;
        }

        if (!scriptfile.isDirectory()) {
            return scriptfile;
        }

        // scriptfile is the directory containing the script, expected name is the name of the directory without fileending (.sikuli)
        int pos = scriptfile.getName().lastIndexOf(".");
        final String expectedFileName;
        if (pos == -1) {
            expectedFileName = scriptfile.getName();
        } else {
            expectedFileName = scriptfile.getName().substring(0, pos);
        }

        File expectedFile = new File(scriptfile, expectedFileName+"."+FILEENDING_PYTHON);
        if (!expectedFile.exists() || expectedFile.isDirectory()) {
            // there is no file with python fileending in this directory, try if there is a file without ending
            expectedFile = new File(scriptfile, expectedFileName);
        } else {
            // real script file found
            return expectedFile;
        }

        if (!expectedFile.exists() || expectedFile.isDirectory()) {
            // there is no file with no fileending either, real script file cannot be found
            return null;
        }

        return expectedFile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int runScript(File scriptfile, File imagepath, String[] argv) {

        // resolve real file, maybe the passed scriptfile is only the directory containing the script
        File realScriptFile = getScriptFile(scriptfile);

        if (realScriptFile == null) {
            Debug.error("No runnable script found: " + scriptfile.getPath());
            return -2;
        }

        return runPython(realScriptFile, imagepath, argv);
    }

    /**
     * Initializes the PythonInterpreter and creates an instance.
     */
    private void createPythonInterpreter() {
        if (interpreter == null) {
            PythonInterpreter.initialize(System.getProperties(), null, sysargv.toArray(new String[sysargv.size()]));
            interpreter = new PythonInterpreter();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getInteractiveHelp() {
        return "**** this might be helpful ****\n" +
                "-- execute a line of code by pressing <enter>\n" +
                "-- separate more than one statement on a line using ;\n" +
                "-- Unlike the iDE, this command window will not vanish, when using a Sikuli feature\n" +
                "   so take care, that all you need is visible on the screen\n" +
                "-- to create an image interactively:\n" +
                "img = capture()\n" +
                "-- use a captured image later:\n" +
                "click(img)";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(String[] param) {
        // Nothing todo
    }

    /**
     * Executes the defined header for the jython script.
     * @param syspaths List of all syspath entries
     */
    private void executeScriptHeader(String[] syspaths) {
        for (String line : PYTHON_SCRIPT_HEADER) {
            Debug.log(5,"PyInit: %s",line);
            interpreter.exec(line);
        }

        for (String syspath : syspaths) {
            interpreter.exec("addModPath(\""+ syspath + "\")");
        }
    }

    /**
     * Fills the sysargv list for the Python script
     * @param pyFile The file containing the script: Has to be passed as first parameter in Python
     * @param argv The parameters passed to Sikuli with --args
     */
    private void fillSysArgv(File pyFile, String[] argv) {
        if (pyFile != null) {
            sysargv = new ArrayList<String>(argv == null ? 1 : argv.length + 1);
            sysargv.add(pyFile.getAbsolutePath());
        } else {
            sysargv = new ArrayList<String>(argv == null ? 0 : argv.length);
        }

        if (argv != null) {
            for(String arg : argv) {
                sysargv.add(arg);
            }
        }
    }

    /**
     * Executes the jythonscript
     * @param pyFile The file containing the script
     * @param imagePath The directory containing the images
     * @param argv The arguments passed by the --args parameter
     * @return The exitcode
     */
    public int runPython(File pyFile, File imagePath, String[] argv) {

        fillSysArgv(pyFile, argv);
        createPythonInterpreter();
        executeScriptHeader(new String[] {
                pyFile.getParentFile().getAbsolutePath(),
                imagePath.getAbsolutePath()
        });

        int exitCode = 0;
        try {
            interpreter.execfile(pyFile.getAbsolutePath());
        } catch (Exception e) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("SystemExit: ([0-9]+)");
            Matcher matcher = p.matcher(e.toString());
            if (matcher.find()) {
                exitCode = Integer.parseInt(matcher.group(1));
                Debug.info("Exit code: " + exitCode);
            } else {
                Debug.error("Script aborted with an error:", e);
                e.printStackTrace();
                exitCode = -1;
            }
        }
        return exitCode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        if (interpreter != null) {
            interpreter.cleanup();
        }
        ScreenHighlighter.closeAll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int runInteractive(String[] argv) {

        fillSysArgv(null, argv);

        String[] jy_args = null;
        String[] iargs = {"-i", "-c",
                "from sikuli import *; SikuliScript.runningInteractive = True; "
                        + "print \"Hello, this is your interactive Sikuli (rules for interactive Python apply)\\n"
                        + "use the UP/DOWN arrow keys to walk through the input history\\n"
                        + "help()<enter> will output some basic Python information\\n"
                        + "shelp()<enter> will output some basic Sikuli information\\n"
                        + "... use ctrl-d to end the session\""};
        if (argv != null && argv.length > 0) {
            jy_args = new String[argv.length + iargs.length];
            System.arraycopy(iargs, 0, jy_args, 0, iargs.length);
            System.arraycopy(argv, 0, jy_args, iargs.length, argv.length);
        } else {
            jy_args = iargs;
        }

        jython.main(jy_args);
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCommandLineHelp() {
        return "You are using the Jython ScriptRunner";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "jython";
    }
}
