package com.intellij.uiDesigner.core;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
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
    private static Map<String, File> pairOperFile = new HashMap<String, File>();
    private String toolPathAxis = "none";
    private final Pattern PATTERN_TOOLPATH_MULTI_AXIS = Pattern.compile("^10: proc\\(firstPosition");
    private Matcher matcherToolPathMultiAxis;
    private boolean isFindToolAxis = false;

    Operation(File file) {
        this.file = file;
        findNames();
        pairOperFile.put(nameOper.toUpperCase(), file);
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

            if (!isFindToolAxis) {
                this.toolPathAxis = findToolPathAxis(stringIn);
            }

            if (isFindToolAxis) {
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

    private String findToolPathAxis(String stringIn) {

        matcherToolPathMultiAxis = PATTERN_TOOLPATH_MULTI_AXIS.matcher(stringIn);

        if (matcherToolPathMultiAxis.find()) {
            int l = stringIn.length();
            String subStr = stringIn.substring((l - 3), (l - 1));
            if (subStr.equals("3D")) {
                isFindToolAxis = true;
                return "3D";
            }
            if (subStr.equals("5X")) {
                isFindToolAxis = true;
                return "5X";
            }
        }

        return "none";
    }

    String getNameGroupProgram() {
        return this.nameGroupProgram;
    }

    String getNameOper() {
        return this.nameOper;
    }

    String getToolPathAxis() {
        return this.toolPathAxis;
    }

    static Map<String, File> getPairOperFile() {
        return pairOperFile;
    }

    static void resetPairOperFile() {
        pairOperFile = new HashMap<String, File>();
    }
}
