package bg.nijel.aGrep.utils;

import com.stericson.RootShell.RootShell;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootShell.execution.Shell;

import java.io.File;
import java.util.ArrayList;

public class RootCommands {

    private static final String UNIX_ESCAPE_EXPRESSION = "(\\(|\\)|\\[|\\]|\\s|\'|\"|`|\\{|\\}|&|\\\\|\\?)";

    private static String getCommandLineString(String input) {
        if (input.startsWith("/storage/emulated/0")){
            return input.replace("/storage/emulated/0","$EXTERNAL_STORAGE");
        }else {
            return input.replaceAll(UNIX_ESCAPE_EXPRESSION, "\\\\$1");
        }
    }

    public static ArrayList<File> listFolders(String path) {
        ArrayList<File> folders = new ArrayList<>();
        ArrayList<String> items;
        items = executeForResult("ls -a " + getCommandLineString(path));
        for (String name : items) {
                if (items.get(0).equals(name)) {
                    folders.add(new File(".."));
                }
                File dir = new File(path + "/" + name);
                if (dir.isDirectory()) {
                    folders.add(dir);
                }else {
                    RootShell.log(RootShell.debugTag, "canRead: " + path + "/" + name + "->" + dir.canRead(), RootShell.LogLevel.ERROR, null);
                }
        }
        if (folders.size() < 1){
            folders.add(new File(".."));
        }
        return folders;
    }

    public static ArrayList<String> listFiles(String path) {
        ArrayList<String> files;
        files = executeForResult("find " + getCommandLineString(path) + " -type f -follow");
        for (String name : files) {
        }
        return files;
    }

    public static int countFiles(String path){
        final ArrayList<String> count = executeForResult("find " + getCommandLineString(path) + " -type f -follow | wc -l");
        if (count != null && count.size() == 1) {
            RootShell.log(RootShell.debugTag, "RCcountFiles:" + Integer.parseInt(count.get(0)));
            return Integer.parseInt(count.get(0));
        }
        RootShell.log(RootShell.debugTag, "RCcountFiles:-1", RootShell.LogLevel.ERROR,null);
        return -1;
    }

    private static ArrayList<String> executeForResult(String cmd) {
        final ArrayList<String> results = new ArrayList<>();

        Command command = new Command(911, false, cmd) {
            @Override
            public String commandOutput(int id, String line) {
                results.add(line);
                super.commandOutput(id, line);
                return line;
            }
        };

        try {
            RootShell.getShell(true).add(command);
            commandWait(RootShell.getShell(true), command);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return results;
    }

    private static void commandWait(Shell shell, Command cmd) {
        while (!cmd.isFinished()) {
            synchronized (cmd) {
                try {
                    if (!cmd.isFinished()) {
                        cmd.wait(2000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (!cmd.isExecuting() && !cmd.isFinished()) {
                if (!shell.isExecuting && !shell.isReading) {
                    Exception e = new Exception();
                    e.setStackTrace(Thread.currentThread().getStackTrace());
                    e.printStackTrace();
                } else if (shell.isExecuting && !shell.isReading) {
                    Exception e = new Exception();
                    e.setStackTrace(Thread.currentThread().getStackTrace());
                    e.printStackTrace();
                } else {
                    Exception e = new Exception();
                    e.setStackTrace(Thread.currentThread().getStackTrace());
                    e.printStackTrace();
                }
            }
        }
    }
}
