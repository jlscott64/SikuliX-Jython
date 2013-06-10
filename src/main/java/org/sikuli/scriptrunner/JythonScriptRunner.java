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
import org.sikuli.script.FileManager;
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

    /** The header commands, that are executed before every script */
    private static String[] SCRIPT_HEADER = new String[]{
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
    public void init(String[] param) {
        // Nothing todo
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int runScript(File script, File imagepath, String[] argv) {
        File scriptFile = FileManager.getScriptFile(script, this);
        if (scriptFile == null) {
            Debug.error("No runnable script found: " + script.getAbsolutePath());
            return -2;
        }
        return runPython(scriptFile, imagepath, argv);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int runTest(File scriptfile, File imagepath, String[] argv) {
        Debug.error("Sikuli Test Feature is not implemented at the moment");
        return -1;
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
    public String getName() {
        return "jython";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getFileEndings() {
      return new String[]{"py"};
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
     * Executes the jythonscript
     * @param pyFile The file containing the script
     * @param imagePath The directory containing the images
     * @param argv The arguments passed by the --args parameter
     * @return The exitcode
     */
    private int runPython(File pyFile, File imagePath, String[] argv) {

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
     * Initializes the PythonInterpreter and creates an instance.
     */
    private void createPythonInterpreter() {
        if (interpreter == null) {
            PythonInterpreter.initialize(System.getProperties(), null, sysargv.toArray(new String[sysargv.size()]));
            interpreter = new PythonInterpreter();
        }
    }

    /**
     * Executes the defined header for the jython script.
     * @param syspaths List of all syspath entries
     */
    private void executeScriptHeader(String[] syspaths) {
        for (String line : SCRIPT_HEADER) {
            Debug.log(5,"PyInit: %s",line);
            interpreter.exec(line);
        }

        for (String syspath : syspaths) {
            interpreter.exec("addModPath(\""+ syspath + "\")");
        }
    }
 }
