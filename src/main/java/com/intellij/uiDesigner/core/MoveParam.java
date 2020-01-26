package com.intellij.uiDesigner.core;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class MoveParam {
//        String s = String.valueOf((char) b);
//        byte[] d = "]".getBytes();
//        System.out.println("");

    private int b;//прочитанный байт
    private File fileIn;
    private ByteArrayInputStream reader;
    private ByteArrayOutputStream writer;
    private static boolean isFirstRapid = false;//флаг что первого движение еще не было. Первое <<<oM[1]( z[/100] )>>> -не обрабатывать
    private static int feed = -1;//значение подачи, если "-1", тогда RAPID
    private static int prevFeed = 0;//последнее выведенное значение подачи
    private static boolean isOutRapid = false;
    private static boolean isPrevFeedHead31 = true;
    private static final byte[] RAPID_HEAD = "PAINT/COLOR,211".concat("\r\n").concat("RAPID").concat("\r\n").getBytes();
    private static final byte[] FEED_HEAD_31 = "PAINT/COLOR,31".concat("\r\n").getBytes();
    private static final byte[] FEED_HEAD_36 = "PAINT/COLOR,36".concat("\r\n").getBytes();
    private static final byte[] FEED = "FEDRAT/MMPM,".getBytes();
    private static final byte[] GOTO_STRING = "GOTO/".getBytes();
    private static final byte[] SPLITTER = ",".getBytes();
    private static final byte[] NEW_STRING = {13, 10};
    private Map<Items, ByteArrayOutputStream> map;

    MoveParam(File fileIn, ByteArrayOutputStream writer) {
        this.fileIn = fileIn;
        this.writer = writer;

        map = getMap();//Заполнить коллекцию пустыми байтовыми массивами для записи координат

        writeMove();
    }

    private void writeMove() {
        try {
            reader = new ByteArrayInputStream(new Fis(fileIn).readAllBytes());

            while ((b = reader.read()) >= 0) {
                getFeed(b);
                writeGotoString(b);
            }

            isFirstRapid = false;
            isOutRapid = false;
            reader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeGotoString(int b) {
        if (b == 51) {//3
            b = reader.read();
            if (b == 49) {//1
                b = reader.read();
                if (b == 58) {//:
                    reader.skip(2l);//пропустить 2 байта " o"
                    b = reader.read();
                    if (b == 77 || b == 76) {//если 'M' или 'L'
                        if (isFirstRapid) {//Пропустить первый Rapid. Первое <<<oM[1]( z[/100] )>>> -не обрабатывать

                            writeFeed(b);//если есть изменения подачи записать

                            reader.skip(5l);//пропустить минимум 5 байт "[1]( "

                            while ((b = reader.read()) != 13) {//читать до конца строки
                                getItems(b);
                            }

                            writeGotoString();
                        } else {
                            isFirstRapid = true;
                        }
                    }
                }
            }
        }
    }

    private void getItems(int b) {
        //получить массивы байт X-Y-Z-U-V-W
        try {
            if (b == 120) {//если есть x
                map.get(Items.OldX).reset();
                getItem(Items.X);
                map.get(Items.OldX).write(map.get(Items.X).toByteArray());
            } else if (b == 121) {//если есть y
                map.get(Items.OldY).reset();
                getItem(Items.Y);
                map.get(Items.OldY).write(map.get(Items.Y).toByteArray());
            } else if (b == 122) {//если есть z
                map.get(Items.OldZ).reset();
                getItem(Items.Z);
                map.get(Items.OldZ).write(map.get(Items.Z).toByteArray());
            } else if (b == 117) {//если есть z
                getItem(Items.U);
            } else if (b == 118) {//если есть z
                getItem(Items.V);
            } else if (b == 119) {//если есть z
                getItem(Items.W);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getItem(Items item) {
        //записать в коллекцию массив байт  с числом между чарами '/' и ']'
        reader.skip(2l);//пропустить 2 символа "[/"

        while ((b = reader.read()) != 93) {//]
            map.get(item).write(b);
        }
    }

    private void writeGotoString() {
        try {
            if (map.get(Items.X).size() == 0) {//если X не найден, переписать X из предъидущей точки
                map.get(Items.X).write(map.get(Items.OldX).toByteArray());
            }
            if (map.get(Items.Y).size() == 0) {//если Y не найден, переписать Y из предъидущей точки
                map.get(Items.Y).write(map.get(Items.OldY).toByteArray());
            }
            if (map.get(Items.Z).size() == 0) { //если Z не найден, переписать Z из предъидущей точки
                if (map.get(Items.U).size() != 0 ||
                        map.get(Items.V).size() != 0 ||
                        map.get(Items.W).size() != 0) {
                    map.get(Items.Z).write(map.get(Items.OldZ).toByteArray());
                }
            }
            writer.write(GOTO_STRING);

// //////////////////////////////////////////////////
//            Locale locale = new Locale("en", "UK");
//            String s;
//            double d;
//
//            s = String.valueOf(map.get(Items.X));
//            map.get(Items.X).reset();
//            d = Double.parseDouble(s);
//            s = String.format(locale,"%.4f", d);
//            map.get(Items.X).write(s.getBytes());
//
//            s = String.valueOf(map.get(Items.Y));
//            map.get(Items.Y).reset();
//            d = Double.parseDouble(s);
//            s = String.format(locale,"%.4f", d);
//            map.get(Items.Y).write(s.getBytes());
//
//            s = String.valueOf(map.get(Items.Z));
//            map.get(Items.Z).reset();
//            d = Double.parseDouble(s);
//            s = String.format(locale,"%.4f", d);
//            map.get(Items.Z).write(s.getBytes());
//
//            if (map.get(Items.U).size() != 0) {
//                s = String.valueOf(map.get(Items.U));
//                map.get(Items.U).reset();
//                d = Double.parseDouble(s);
//                s = String.format(locale, "%.7f", d);
//                map.get(Items.U).write(s.getBytes());
//            }
//            if (map.get(Items.V).size() != 0) {
//                s = String.valueOf(map.get(Items.V));
//                map.get(Items.V).reset();
//                d = Double.parseDouble(s);
//                s = String.format(locale, "%.7f", d);
//                map.get(Items.V).write(s.getBytes());
//            }
//            if (map.get(Items.W).size() != 0) {
//                s = String.valueOf(map.get(Items.W));
//                map.get(Items.W).reset();
//                d = Double.parseDouble(s);
//                s = String.format(locale, "%.7f", d);
//                map.get(Items.W).write(s.getBytes());
//            }
// ///////////////////////////////////////////////////

            writeItem(Items.X);
            writeItem(Items.Y);
            writeItem(Items.Z);
            writeItem(Items.U);
            writeItem(Items.V);
            writeItem(Items.W);
            writer.write(NEW_STRING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeItem(Items item) {
        try {

            if (item != Items.X && map.get(item).size() != 0) {
                writer.write(SPLITTER);
            }

            writer.write(map.get(item).toByteArray());
            map.get(item).reset();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Map<Items, ByteArrayOutputStream> getMap() {
        //Заполнить коллекцию байтовыми массивами для записи координат
        Map<Items, ByteArrayOutputStream> map = new HashMap<Items, ByteArrayOutputStream>();
        Items[] items = Items.values();

        for (Items i : items) {
            map.put(i, new ByteArrayOutputStream());
        }

        return map;
    }

    private enum Items {
        X,
        Y,
        Z,
        U,
        V,
        W,
        OldX,
        OldY,
        OldZ,
        OldU,
        OldV,
        OldW
    }

    private void getFeed(int b) {
        //получить значения текущей подачи
        if (b == 49) {//1
            b = reader.read();
            if (b == 48) {//0
                b = reader.read();
                if (b == 58) {//:
                    reader.skip(1l);//пропустить 1 байт " "
                    b = reader.read();
                    if (b == 116) {//"t"
                        b = reader.read();
                        if (b == 114) {//"r"
                            b = reader.read();
                            if (b == 40) {//"("
                                b = reader.read();
                                if (b == 70) {//"F"
                                    b = reader.read();
                                    if (b == 88) {//"X"
                                        b = reader.read();
                                        if (b == 42) {//"*"
                                            ByteArrayOutputStream writer = new ByteArrayOutputStream();

                                            while ((b = reader.read()) != 41) {//читать до ")" конца числа
                                                writer.write(b);
                                            }

                                            double k = Double.parseDouble(writer.toString());
                                            feed = (int) Math.round(Feed.getFeed() * k);
                                        } else {
                                            feed = Feed.getFeed();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void writeFeed(int b) {
        //если есть изменения подачи записать
        try {
            if (b == 77 && !isOutRapid) {
                writer.write(RAPID_HEAD);
                isOutRapid = true;
                feed = Feed.getFeed();
                prevFeed = -1;
            }

            if (b == 76 && isChangeFeed()) {
                writer.write(FEED);
                writer.write(String.format("%d.0000", feed).getBytes());
                writer.write(NEW_STRING);

                if (prevFeed == -1) {
                    writer.write(FEED_HEAD_31);
                    isPrevFeedHead31 = true;
                } else {
                    if (isPrevFeedHead31) {
                        writer.write(FEED_HEAD_36);
                        isPrevFeedHead31 = false;
                    } else {
                        writer.write(FEED_HEAD_31);
                        isPrevFeedHead31 = true;
                    }
                }

                prevFeed = feed;
                isOutRapid = false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isChangeFeed() {

        if (feed == prevFeed) {
            return false;
        }

        return true;
    }
}
