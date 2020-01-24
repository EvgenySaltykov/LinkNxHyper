package com.intellij.uiDesigner.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

class PrintLog {
    private static File file = null;
    private static FileOutputStream fileOutputStream;
    private static PrintStream console = System.err;
    private static PrintStream stream;

    static Logger logger = Logger.getLogger(MainForm.class.getName());

    PrintLog(Level level, String message) {
        //вывод в файл информационного лога
        try {
            if (file == null) {
                file = new File(MainForm.getPathPofDirectory() + "\\" + "log.txt");

                //создаем адапер к классу PrintStream
                fileOutputStream = new FileOutputStream(file);
                stream = new PrintStream(fileOutputStream);

                //Устанавливаем вывод System.err в файл
                System.setErr(stream);
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Файл не найден");
            e.printStackTrace();
        }

        System.err.println("\n" + "# log");
        logger.log(level, message.concat("\n"));
        System.err.println("#######################################");
        System.err.println("");
    }

    PrintLog(Level level, String message, Throwable event) {
        //вывод в файл лога с исключением
        try {
            if (file == null) {
                file = new File(MainForm.getPathPofDirectory() + "\\" + "log.txt");

                //создаем адапер к классу PrintStream
                fileOutputStream = new FileOutputStream(file);
                stream = new PrintStream(fileOutputStream);

                //Устанавливаем вывод System.err в файл
                System.setErr(stream);
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Файл не найден");
            e.printStackTrace();
        }

        System.err.println("\n" + "# log");
        logger.log(level, message.concat("\n"));
        System.err.println("# fillInStackTrace().printStackTrace()");
        event.fillInStackTrace().printStackTrace();
        System.err.println("#######################################");
        System.err.println("");
    }

    static void closeLogFile() {
        //Возвпащаем вывод ошибок обратно в консоль
        System.setErr(console);

        try {
            if (file != null && file.exists()) {
                fileOutputStream.close();
                stream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static boolean isException() {
        if (file == null) {
            return false;
        } else {
            return true;
        }
    }
}
