package com.intellij.uiDesigner.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ToolPath {
//        String s = String.valueOf((char) b);
//        byte[] d = "]".getBytes();
//        System.out.println("");

    private int b;//прочитанный байт
    private File fileIn;
    private ByteArrayInputStream reader;
    private ByteArrayOutputStream writer;
    private static double feed = -1.0;//значение подачи, если "-1", тогда RAPID
    private static double prevFeed = 0.0;//последнее выведенное значение подачи
    private Map<Items, ByteArrayOutputStream> map;
    private double[] pointVector = new double[6];

    ToolPath(File fileIn) {
        this.fileIn = fileIn;
        this.writer = writer;

        map = getMap();//Заполнить коллекцию пустыми байтовыми массивами для записи координат

        writeMove();
    }

    private void writeMove() {
        try {
            reader = new ByteArrayInputStream(new Fis(fileIn).readAllBytes());

//            int i = 0;
//            long start = new Date().getTime();

            while ((b = reader.read()) >= 0) {
                getFeed(b);
                getGoto(b);
//                i++;
//                if (i == 10000) {
//                    i = 0;
//                    long end = new Date().getTime();
//                    JOptionPane.showMessageDialog(null, "время траектории " + (end - start), "", JOptionPane.INFORMATION_MESSAGE);
//                    start = new Date().getTime();
//                }
            }

            reader.close();

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

//        try {
//            if (map.get(Items.OldX).size() == 0) map.get(Items.OldX).write("0.0".getBytes());
//            if (map.get(Items.OldY).size() == 0) map.get(Items.OldY).write("0.0".getBytes());
//            if (map.get(Items.OldZ).size() == 0) map.get(Items.OldZ).write("0.0".getBytes());
//            if (map.get(Items.U).size() == 0) map.get(Items.U).write("0.0".getBytes());
//            if (map.get(Items.OldU).size() == 0) map.get(Items.OldU).write("0.0".getBytes());
//            if (map.get(Items.V).size() == 0) map.get(Items.V).write("0.0".getBytes());
//            if (map.get(Items.OldV).size() == 0) map.get(Items.OldV).write("0.0".getBytes());
//            if (map.get(Items.W).size() == 0) map.get(Items.W).write("1.0".getBytes());
//            if (map.get(Items.OldW).size() == 0) map.get(Items.OldW).write("1.0".getBytes());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }


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
                                            writer = new ByteArrayOutputStream();

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
        if (b == 51) {//3
            b = reader.read();
            if (b == 49) {//1
                b = reader.read();
                if (b == 58) {//:
                    reader.skip(2l);//пропустить 2 байта " o"
                    b = reader.read();
                    if (b == 77 || b == 76) {//если 'M' или 'L'

                        if (b == 77) feed = -1.0;

                        reader.skip(5l);//пропустить минимум 5 байт "[1]( "

                        while ((b = reader.read()) != 13) {//читать до конца строки
                            getItems(b);
                        }

                        getParseArrayDouble(map);
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

    private void getParseArrayDouble(Map<Items, ByteArrayOutputStream> map) {

            if (map.get(Items.X).size() != 0) {
                pointVector[0] = Double.parseDouble(map.get(Items.X).toString());
            }
            if (map.get(Items.Y).size() != 0) {
                pointVector[1] = Double.parseDouble(map.get(Items.Y).toString());
            }
            if (map.get(Items.Z).size() != 0) {
                pointVector[2] = Double.parseDouble(map.get(Items.Z).toString());
            }
            if (map.get(Items.U).size() != 0) {
                pointVector[3] = Double.parseDouble(map.get(Items.U).toString());
            }
            if (map.get(Items.V).size() != 0) {
                pointVector[4] = Double.parseDouble(map.get(Items.V).toString());
            }
            if (map.get(Items.W).size() != 0) {
                pointVector[5] = Double.parseDouble(map.get(Items.W).toString());
            }

            map.get(Items.X).reset();
            map.get(Items.Y).reset();
            map.get(Items.Z).reset();
            map.get(Items.U).reset();
            map.get(Items.V).reset();
            map.get(Items.W).reset();
    }

    private void createMoveToPoint(double feed, Map<Items, ByteArrayOutputStream> map) {

    }
}
