package com.intellij.uiDesigner.core;

import java.io.*;
import java.util.logging.Level;

class ReadFile {
    private File file;
    private BufferedReader reader;

    public ReadFile(File file) {
        this.file = file;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "cp1251"));
        } catch (IOException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка поиска pof-фала!!!", e);
        }
    }

    String getLine() {
        try {
            if (reader.ready()) {
                return reader.readLine();
            } else {
                return null;
            }
        } catch (IOException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка чтения из pof-файла!!!", e);
            return null;
        }
    }

    boolean ready() {
        try {
            return reader.ready();
        } catch (IOException e) {
            new PrintLog(Level.WARNING, "!!!ошибка чтения файла!!!", e);
            return false;
        }
    }

    void close() {
        try {
            reader.close();
        } catch (IOException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка закрытия pof-файла!!!", e);
        }
    }
}
