package com.intellij.uiDesigner.core;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Feed {
    private File file;
    private static int feedX = -1;

    Feed(File file) {
        this.file = file;
        feedX = findFeed();
    }

    private int findFeed() {
        ReadFile reader = new ReadFile(this.file);
        String stringIn;
        StringBuilder outString = new StringBuilder();
        String PATTERN_FEED_X = "^\\d*: FX\\(";
        Pattern pattern = Pattern.compile(PATTERN_FEED_X);
        Matcher matcher;
        int maxReadLine = 2000; //максимальное колличество прочтенных сторк, чтобы не читать весь файл целиком

        while (reader.ready() && maxReadLine > 0) {
            stringIn = reader.getLine();
            matcher = pattern.matcher(stringIn);

            if (matcher.find()) {
                outString.append(stringIn.substring(matcher.end(), (stringIn.length() - 1)));
                reader.close();
                break;
            }
            maxReadLine -= 1;
        }

        if (outString.toString().equals("")) {
            outString.append(0);
        }

        return Integer.parseInt(outString.toString());
    }

    static int getFeedX() {
        return feedX;
    }
}
