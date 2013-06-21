/*
 * Copyright 2010-2013, Sikuli.org
 * Released under the MIT License.
 *
 * modified RaiMan 2012
 */
package org.sikuli.scriptrunner;

import java.io.File;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.cli.CommandLine;
import org.python.util.PythonInterpreter;
import org.python.util.jython;
import org.sikuli.script.Debug;
import org.sikuli.script.FileManager;
import org.sikuli.script.IScriptRunner;

/**
 * Executes Sikuliscripts written in Python/Jython.
 */
public class JythonScriptRunner implements IScriptRunner {

  private static final String me = "JythonScriptRunner";
  /**
   * The PythonInterpreter instance
   */
  private static PythonInterpreter interpreter = null;
  /**
   * sys.argv for the jython script
   */
  private static ArrayList<String> sysargv = null;
  /**
   * The header commands, that are executed before every script
   */
  private static String[] SCRIPT_HEADER = new String[]{
    "# -*- coding: utf-8 -*- ",
    "import sys",
    "from __future__ import with_statement",
    "from sikuli import *",
    "resetROI()",
    "setShowActions(False)"
  };
  private static ArrayList<String> codeBefore = null;
  private static ArrayList<String> codeAfter = null;
  /**
   * CommandLine args
   */
  CommandLine cmdLine = null;
  private int errorLine;
  private int errorColumn;
  private String errorType;
  private String errorText;
  private int errorClass;
  private String errorTrace;
  private static final int PY_SYNTAX = 0;
  private static final int PY_RUNTIME = 1;
  private static final int PY_JAVA = 2;
  private static final int PY_UNKNOWN = -1;
  private static final String NL = String.format("%n");
  private Pattern pFile = Pattern.compile("File..(.*?\\.py).*?"
          + ",.*?line.*?(\\d+),.*?in(.*?)" + NL + "(.*?)" + NL);
  //TODO SikuliToHtmlConverter implement in Java
  final static InputStream SikuliToHtmlConverter =
          JythonScriptRunner.class.getResourceAsStream("/scripts/sikuli2html.py");
  static String pyConverter =
          FileManager.convertStreamToString(SikuliToHtmlConverter);
  //TODO SikuliBundleCleaner implement in Java
  final static InputStream SikuliBundleCleaner =
          JythonScriptRunner.class.getResourceAsStream("/scripts/clean-dot-sikuli.py");
  static String pyBundleCleaner =
          FileManager.convertStreamToString(SikuliBundleCleaner);

  /**
   * {@inheritDoc}
   */
  @Override
  public void init(String[] param) {
    //HACK: to let it work with python.path empty
    if (System.getProperty("python.path") == null) {
      CodeSource src = JythonScriptRunner.class.getProtectionDomain().getCodeSource();
      if (src.getLocation() != null) {
        Debug.log(2, "%s: init: python.path hack: %s", me, src.getLocation().getPath());
        String pyLib = new File(src.getLocation().getPath(), "Lib").getAbsolutePath();
        System.setProperty("python.path", pyLib);
      } else {
        Debug.error("%s: init: python.path == null: Sikuli might not work", me);
      }
    }
  }

  /**
   * Executes the jythonscript
   *
   * @param pyFile The file containing the script
   * @param imagePath The directory containing the images
   * @param argv The arguments passed by the --args parameter
   * @return The exitcode
   */
  @Override
  public int runScript(File pyFile, File imagePath, String[] argv, String[] forIDE) {
//TODO not useable this way from IDE on rerun
    fillSysArgv(pyFile, argv);
    createPythonInterpreter();
    try {
      executeScriptHeader(new String[]{
        pyFile.getParentFile().getAbsolutePath(),
        (imagePath == null ? null : imagePath.getAbsolutePath())
      });
    } catch (Exception e) {
    }
    int exitCode = 0;
    try {
      interpreter.execfile(pyFile.getAbsolutePath());
    } catch (Exception e) {
      java.util.regex.Pattern p =
              java.util.regex.Pattern.compile("SystemExit: ([0-9]+)");
      Matcher matcher = p.matcher(e.toString());
//TODO error stop I18N
      if (matcher.find()) {
        Debug.info("Exit code: " + matcher.group(1));
      } else {
        //Debug.error(_I("msgStopped"));
        exitCode = findErrorSource(e, pyFile.getAbsolutePath(), forIDE);
        if (forIDE != null) {
          exitCode *= -1;
        } else {
          exitCode = 1;
        }
      }
    }
    return exitCode;
  }

  private int findErrorSource(Throwable thr, String filename, String[] forIDE) {
    String err = thr.toString();
//      Debug.error("------------- Traceback -------------\n" + err +
//              "------------- Traceback -------------\n");
    errorLine = -1;
    errorColumn = -1;
    errorClass = PY_UNKNOWN;
    errorType = "--UnKnown--";
    errorText = "--UnKnown--";

    String msg;
    Matcher mFile = null;

    if (err.startsWith("Traceback")) {
      Pattern pError = Pattern.compile(NL + "(.*?):.(.*)$");
      mFile = pFile.matcher(err);
      if (mFile.find()) {
        Debug.log(2, "Runtime error line: " + mFile.group(2)
                + "\n in function: " + mFile.group(3)
                + "\n statement: " + mFile.group(4));
        errorLine = Integer.parseInt(mFile.group(2));
        errorClass = PY_RUNTIME;
        Matcher mError = pError.matcher(err);
        if (mError.find()) {
          Debug.log(2, "Error:" + mError.group(1));
          Debug.log(2, "Error:" + mError.group(2));
          errorType = mError.group(1);
          errorText = mError.group(2);
        } else {
//org.sikuli.core.FindFailed: FindFailed: can not find 1352647716171.png on the screen
          Pattern pFF = Pattern.compile(": FindFailed: (.*?)" + NL);
          Matcher mFF = pFF.matcher(err);
          if (mFF.find()) {
            errorType = "FindFailed";
            errorText = mFF.group(1);
          } else {
            errorClass = PY_UNKNOWN;
          }
        }
      }
    } else if (err.startsWith("SyntaxError")) {
      Pattern pLineS = Pattern.compile(", (\\d+), (\\d+),");
      java.util.regex.Matcher mLine = pLineS.matcher(err);
      if (mLine.find()) {
        Debug.log(4, "SyntaxError error line: " + mLine.group(1));
        Pattern pText = Pattern.compile("\\((.*?)\\(");
        java.util.regex.Matcher mText = pText.matcher(err);
        mText.find();
        errorText = mText.group(1) == null ? errorText : mText.group(1);
        Debug.log(4, "SyntaxError: " + errorText);
        errorLine = Integer.parseInt(mLine.group(1));
        errorColumn = Integer.parseInt(mLine.group(2));
        errorClass = PY_SYNTAX;
        errorType = "SyntaxError";
      }
    }

    msg = "script";
    if (forIDE != null) {
      msg += " [ " + forIDE[0] + " ]";
    }
    if (errorLine != -1) {
      //Debug.error(_I("msgErrorLine", srcLine));
      msg += " stopped with error in line " + errorLine;
      if (errorColumn != -1) {
        msg += " at column " + errorColumn;
      }
    } else {
      msg += "] stopped with error at line --unknown--";
    }

    if (errorClass == PY_RUNTIME || errorClass == PY_SYNTAX) {
      Debug.error(msg);
      Debug.error(errorType + " ( " + errorText + " )");
      if (errorClass == PY_RUNTIME) {
        errorClass = findErrorSourceWalkTrace(mFile, filename);
        if (errorTrace.length() > 0) {
          Debug.error("--- Traceback --- error source first\n"
                  + "line: module ( function ) statement \n" + errorTrace);
        }
      }
    } else if (errorClass == PY_JAVA) {
    } else {
      Debug.error(msg);
      Debug.error("Could not evaluate error source nor reason. Analyze StackTrace!");
      Debug.error(err);
    }
    return errorLine;
  }

  private int findErrorSourceWalkTrace(Matcher m, String filename) {
//[error] Traceback (most recent call last):
//File "/var/folders/wk/pcty7jkx1r5bzc5dvs6n5x_40000gn/T/sikuli-tmp3464751893408897244.py", line 2, in
//sub.hello()
//File "/Users/rhocke/NetBeansProjects/RaiManSikuli2012-Script/sub.sikuli/sub.py", line 4, in hello
//print "hello from sub", 1/0
//ZeroDivisionError: integer division or modulo by zero
    Pattern pModule = Pattern.compile(".*/(.*?).py");
    //Matcher mFile = pFile.matcher(etext);
    String mod;
    String modIgnore = "SikuliImporter,";
    StringBuilder trace = new StringBuilder();
    String telem;
    while (m.find()) {
      if (m.group(1).equals(filename)) {
        mod = "main";
      } else {
        Matcher mModule = pModule.matcher(m.group(1));
        mModule.find();
        mod = mModule.group(1);
        if (modIgnore.contains(mod + ",")) {
          continue;
        }
      }
      telem = m.group(2) + ": " + mod + " ( "
              + m.group(3) + " ) " + m.group(4) + NL;
      //Debug.log(2,telem);
      trace.insert(0, telem);
//        Debug.log(2,"Rest of Trace ----\n" + etext.substring(mFile.end()));
    }
    Debug.log(2, "------------- Traceback -------------\n" + trace);
    errorTrace = trace.toString();
    return errorClass;
  }

  private void findErrorSourceFromJavaStackTrace(Throwable thr, String filename) {
    Debug.error("seems to be an error in the Java API supporting code");
    StackTraceElement[] s;
    Throwable t = thr;
    while (t != null) {
      s = t.getStackTrace();
      Debug.log(2, "stack trace:");
      for (int i = s.length - 1; i >= 0; i--) {
        StackTraceElement si = s[i];
        Debug.log(2, si.getLineNumber() + " " + si.getFileName());
        if (si.getLineNumber() >= 0 && filename.equals(si.getFileName())) {
          errorLine = si.getLineNumber();
        }
      }
      t = t.getCause();
      Debug.log(2, "cause: " + t);
    }
  }

  @Override
  public int runTest(File scriptfile, File imagepath, String[] argv, String[] forIDE) {
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
    return "**** this might be helpful ****\n"
            + "-- execute a line of code by pressing <enter>\n"
            + "-- separate more than one statement on a line using ;\n"
            + "-- Unlike the iDE, this command window will not vanish, when using a Sikuli feature\n"
            + "   so take care, that all you need is visible on the screen\n"
            + "-- to create an image interactively:\n"
            + "img = capture()\n"
            + "-- use a captured image later:\n"
            + "click(img)";
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
  public String hasFileEnding(String ending) {
    for (String suf : getFileEndings()) {
      if (suf.equals(ending.toLowerCase())) {
        return suf;
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() {
    if (interpreter != null) {
      interpreter.cleanup();
    }
  }

  /**
   * Fills the sysargv list for the Python script
   *
   * @param pyFile The file containing the script: Has to be passed as first parameter in Python
   * @param argv The parameters passed to Sikuli with --args
   */
  private void fillSysArgv(File pyFile, String[] argv) {
    sysargv = new ArrayList<String>();
    if (pyFile != null) {
      sysargv.add(pyFile.getAbsolutePath());
    }
    if (argv != null) {
      sysargv.addAll(Arrays.asList(argv));
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

  public PythonInterpreter getPythonInterpreter() {
    if (interpreter == null) {
      sysargv = new ArrayList<String>();
      createPythonInterpreter();
    }
    return interpreter;
  }

  @Override
  public boolean doSomethingSpecial(String action, Object[] args) {
    if ("redirect".equals(action)) {
      doRedirect((PipedInputStream[]) args);
      return true;
    } else if ("convertSrcToHtml".equals(action)) {
      convertSrcToHtml((String) args[0]);
      return true;
    } else if ("cleanBundle".equals(action)) {
      cleanBundle((String) args[0]);
      return true;
    } else {
      return false;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execBefore(String[] stmts) {
    if (stmts == null) {
      codeBefore = null;
      return;
    }
    if (codeBefore == null) {
      codeBefore = new ArrayList<String>();
    }
    codeBefore.addAll(Arrays.asList(stmts));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execAfter(String[] stmts) {
    if (stmts == null) {
      codeAfter = null;
      return;
    }
    if (codeAfter == null) {
      codeAfter = new ArrayList<String>();
    }
    codeAfter.addAll(Arrays.asList(stmts));
  }

  /**
   * Executes the defined header for the jython script.
   *
   * @param syspaths List of all syspath entries
   */
  private void executeScriptHeader(String[] syspaths) {
    for (String line : SCRIPT_HEADER) {
      Debug.log(5, "PyInit: %s", line);
      interpreter.exec(line);
    }
    for (String syspath : syspaths) {
      interpreter.exec("addModPath(\"" + syspath + "\")");
    }
    if (codeBefore != null) {
      for (String line : codeBefore) {
        interpreter.exec(line);
      }
    }
  }

  private boolean doRedirect(PipedInputStream[] pin) {
    PythonInterpreter py = getPythonInterpreter();
    try {
      PipedOutputStream pout = new PipedOutputStream(pin[0]);
      PrintStream ps = new PrintStream(pout, true);
      System.setOut(ps);
      py.setOut(ps);
    } catch (Exception e) {
      Debug.log(2, "%s: doRedirect: Couldn't redirect STDOUT\n%s", me, e.getMessage());
      return false;
    }
    try {
      PipedOutputStream pout = new PipedOutputStream(pin[1]);
      PrintStream ps = new PrintStream(pout, true);
      System.setErr(ps);
      py.setErr(ps);
    } catch (Exception e) {
      Debug.log(2, "%s: doRedirect: Couldn't redirect STDERR\n%s", me, e.getMessage());
      return false;
    }
    return true;
  }

  private void convertSrcToHtml(String bundle) {
    PythonInterpreter py = new PythonInterpreter();
    Debug.log(2, "Convert Sikuli source code " + bundle + " to HTML");
    py.set("local_convert", true);
    py.set("sikuli_src", bundle);
    py.exec(pyConverter);
  }

  private void cleanBundle(String bundle) {
    PythonInterpreter py = new PythonInterpreter();
    Debug.log(2, "Clear source bundle " + bundle);
    py.set("bundle_path", bundle);
    py.exec(pyBundleCleaner);
  }
}
