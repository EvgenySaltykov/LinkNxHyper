package com.intellij.uiDesigner.core;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ParamTool {
    private File file;
    private String nameTool = "Имя_инструмента_не_найдено";
    private int numberTool = 0;
    private int idTool = 0;
    private boolean isName = false;
    private boolean isNumber = false;
    private boolean isId = false;

    //шаблоны и паттерны
    private final String PATTERN_NAME = ".*cfg\\(\\*WKZKOMMENTAR ";
    private final String NUMBER_TOOL = ".*cfg\\(\\*WKZNUMMER ";
    private final String ID_TOOL = ".*cfg\\(\\*TOOL_CLASS_ID ";
    private final Pattern patternName = Pattern.compile(PATTERN_NAME);
    private final Pattern patternNumber = Pattern.compile(NUMBER_TOOL);
    private final Pattern patternId = Pattern.compile(ID_TOOL);

    ParamTool(File file) {
        this.file = file;
        findParam();
    }

    private void findParam() {
        ReadFile reader = new ReadFile(this.file);
        String stringIn;
        int maxReadLine = 2000; //максимальное колличество прочтенных сторк, чтобы не читать весь файл целиком

        while (reader.ready() && maxReadLine > 0) {
            stringIn = reader.getLine();

            if (isName && isNumber && isId) {
                break;
            } else {
                findName(stringIn, patternName.matcher(stringIn));
                findNumber(stringIn, patternNumber.matcher(stringIn));
                findId(stringIn, patternId.matcher(stringIn));
            }

            maxReadLine -= 1;
        }
    }

    private void findName(String stringIn, Matcher matcher) {
        if (!isName && matcher.find()) {
            nameTool = stringIn.substring(matcher.end(), (stringIn.length() - 1)).replace(" ", "_").replace(":", "_");
            isName = true;
        }
    }

    private void findNumber(String stringIn, Matcher matcher) {
        if (!isNumber && matcher.find()) {
            numberTool = Integer.parseInt(stringIn.substring(matcher.end(), (stringIn.length() - 1)));
            isNumber = true;
        }
    }

    private void findId(String stringIn, Matcher matcher) {
        if (!isId && matcher.find()) {
            idTool = Integer.parseInt(stringIn.substring(matcher.end(), (stringIn.length() - 1)));
            isId = true;
        }
    }

    String getName() {
        return nameTool;
    }

    int getToolNumber() {
        return numberTool;
    }

    int getToolId() {
        return idTool;
    }
}
