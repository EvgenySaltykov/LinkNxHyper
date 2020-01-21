package com.intellij.uiDesigner.core;

import java.io.File;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ParamOperation {
    private File file;
    private String nameOper;
    private String nameGroupProgram;
    private final Pattern PATTERN_NAME_OPER = Pattern.compile("1: cfg\\(\\*JOBNAME ");
    private Matcher matcherNameOper;
    private boolean isFindNameOper = false;
    private final Pattern PATTERN_NAME_GROUP_PROGRAM = Pattern.compile("1: cfg\\(\\*JOBNAME ");
    private Matcher matcherNameGroupProgram;
    private boolean isFindNameGroupProgram = false;

    public ParamOperation(File file) {
        this.file = file;
        findParam();
    }

    private void findParam() {
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

    String getNameOper() {
        if (this.nameOper.equals("")) {
            new PrintLog(Level.WARNING, "!!!Не найдено имя операции!!!");
        }

        return this.nameOper;
    }

    String getNameGroupProgram() {
        if (this.nameGroupProgram.equals("")) {
            new PrintLog(Level.WARNING, "!!!Не найдено имя группы программ!!!");
        }

        return this.nameGroupProgram;
    }
}
