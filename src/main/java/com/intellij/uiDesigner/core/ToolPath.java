package com.intellij.uiDesigner.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class ToolPath {
//        String s = String.valueOf((char) b);
//        byte[] d = "]".getBytes();
//        System.out.println("");

    private int b;//прочитанный байт
    private File fileIn;
    private ByteArrayInputStream reader;
    private ByteArrayOutputStream writer;
    private static boolean isFirstRapid = false;//флаг что первого движение еще не было. Первое <<<oM[1]( z[/100] )>>> -не обрабатывать
    private static double feed = -1.0;//значение подачи, если "-1", тогда RAPID
    private static double prevFeed = 0.0;//последнее выведенное значение подачи
    private Map<Items, ByteArrayOutputStream> map;

    ToolPath(File fileIn, ByteArrayOutputStream writer) {
        this.fileIn = fileIn;
        this.writer = writer;

        map = getMap();//Заполнить коллекцию пустыми байтовыми массивами для записи координат

        createToolPath();
    }

    private void createToolPath() {
        try {
            reader = new ByteArrayInputStream(new Fis(fileIn).readAllBytes());

            while ((b = reader.read()) >= 0) {
                getFeed(b);

                getGoto(b);
//                    getItemsGoto(b, map);

//                    createMoveToPoint(feed, map);
            }

            reader.close();

        } catch (IOException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка IOException в методе  createToolPath!!!", e);
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
        //если есть кадр изменения подачи  <<< 10: tr(FX*2.594) >>>
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

                                            feed = Math.round(Feed.getFeed() * k);
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

    private void getGoto(int b) {
        //получить массивы байт X-Y-Z-U-V-W
        if (b == 51) {//3
            b = reader.read();
            if (b == 49) {//1
                b = reader.read();
                if (b == 58) {//:
                    reader.skip(2l);//пропустить 2 байта " o"
                    
                    b = reader.read();
                    if (b == 77 || b == 76) {//если 'M' или 'L'
                        //получить массивы байт X-Y-Z-U-V-W
                        if (b == 77) feed = -1.0; //если 'M' то подача RAPID

                        reader.skip(5l);//пропустить минимум 5 байт "[1]( "

                        while ((b = reader.read()) != 13) {//читать до конца строки
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
                                new PrintLog(Level.WARNING, "!!!Ошибка IOException в методе  getGoto!!!", e);
                            }
                        }
                    }
                }
            }
        }
    }

    private void getItem(Items item) {
        //записать в коллекцию массив байт  с числом между чарами '/' и ']'
        reader.skip(2l);//пропустить 2 символа "[/"

        while ((b = reader.read()) != 93) {//]
            map.get(item).write(b);
        }
    }

    private void createMoveToPoint(double feed, Map<Items, ByteArrayOutputStream> map) {

    }
}
