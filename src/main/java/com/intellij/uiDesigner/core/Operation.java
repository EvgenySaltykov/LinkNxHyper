package com.intellij.uiDesigner.core;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Operation {
    private File file;
    private String nameGroupProgram;
    private final Pattern PATTERN_NAME_GROUP_PROGRAM = Pattern.compile("^1: cfg\\(\\*JOBLISTNAME ");
    private Matcher matcherNameGroupProgram;
    private boolean isFindNameGroupProgram = false;
    private String nameOper;
    private final Pattern PATTERN_NAME_OPER = Pattern.compile("^1: cfg\\(\\*JOBNAME ");
    private Matcher matcherNameOper;
    private boolean isFindNameOper = false;

    Operation(File file) {
        this.file = file;
        findNames();
    }

    private void findNames() {
        //возвращает имя операции
        ReadFile reader = new ReadFile(this.file);
        String stringIn;
        int maxReadLine = 2000; //максимальное колличество прочтенных сторк, чтобы не читать весь файл целиком

        while (reader.ready() && maxReadLine > 0) {
            stringIn = reader.getLine();

            if (!isFindNameGroupProgram) {
                this.nameGroupProgram = findNameGroupProgram(stringIn);
            }

            if (!isFindNameOper) {
                this.nameOper = findNameOper(stringIn);
            }

            if (isFindNameGroupProgram && isFindNameOper) {
                break;
            }

            maxReadLine -= 1;
        }

        reader.close();
    }

    private String findNameOper(String stringIn) {

        matcherNameOper = PATTERN_NAME_OPER.matcher(stringIn);

        if (matcherNameOper.find()) {
            StringBuilder outString = new StringBuilder();
            outString.append(stringIn.substring(matcherNameOper.end(), (stringIn.length() - 1)).replace(" ", "_").replace(":", "_"));
            isFindNameOper = true;
            return outString.toString();
        } else {
            return "";
        }
    }

    private String findNameGroupProgram(String stringIn) {

        matcherNameGroupProgram = PATTERN_NAME_GROUP_PROGRAM.matcher(stringIn);

        if (matcherNameGroupProgram.find()) {
            StringBuilder outString = new StringBuilder();
            outString.append(stringIn.substring(matcherNameGroupProgram.end(), (stringIn.length() - 1)).replace(" ", "_").replace(":", "_"));
            isFindNameGroupProgram = true;
            return outString.toString();
        } else {
            return "";
        }
    }

    String getNameGroupProgram() {
        return this.nameGroupProgram;
    }

    String getNameOper() {
        return this.nameOper;
    }
}
