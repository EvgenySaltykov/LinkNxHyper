package com.intellij.uiDesigner.core;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Feed {
    private File file;
    private static int feedX = -1;
    private static int feedZ = -1;

    Feed(File file) {
        this.file = file;
        findFeed();
    }

    private void findFeed() {
        ReadFile reader = new ReadFile(this.file);
        String stringIn;
        StringBuilder outString = new StringBuilder();
        String PATTERN_FEED_X = "^\\d*: FX\\(";
        Pattern patternX = Pattern.compile(PATTERN_FEED_X);
        String PATTERN_FEED_Z = "^\\d*: FZ\\(";
        Pattern patternZ = Pattern.compile(PATTERN_FEED_Z);
        Matcher matcherX;
        Matcher matcherZ;
        int maxReadLine = 2000; //максимальное колличество прочтенных сторк, чтобы не читать весь файл целиком

        while (reader.ready() && maxReadLine > 0) {
            stringIn = reader.getLine();

            matcherX = patternX.matcher(stringIn);
            if (matcherX.find()) {
                outString.append(stringIn.substring(matcherX.end(), (stringIn.length() - 1)));

                if (outString.toString().equals("")) {
                    outString.append(0);
                }

                feedX = Integer.parseInt(outString.toString());
                outString = new StringBuilder();
            }

            matcherZ = patternZ.matcher(stringIn);
            if (matcherZ.find()) {
                outString.append(stringIn.substring(matcherZ.end(), (stringIn.length() - 1)));

                if (outString.toString().equals("")) {
                    outString.append(0);
                }

                feedZ = Integer.parseInt(outString.toString());
                outString = new StringBuilder();
            }

            if (feedX != -1 && feedZ != -1) break;

            maxReadLine -= 1;
        }

        reader.close();
    }

    static int getFeedX() {
        return feedX;
    }

    static int getFeedZ() {
        return feedZ;
    }
}
