package com.intellij.uiDesigner.core;

import nxopen.NXException;
import nxopen.cam.MillUserDefined;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
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
    private static double feed = -1.0;//значение подачи, если "-1", тогда RAPID
    private static double prevFeed = 0.0;//последнее выведенное значение подачи
    private Map<Items, ByteArrayOutputStream> map;
    private double[] pointVector = new double[6];
    private String operName = "";

    ToolPath(File fileIn, String operName) {
        this.fileIn = fileIn;
        this.operName = operName;

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
                createMoveToPoint(b);
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
            new PrintLog(Level.WARNING, "!!!Ошибка IOException в методе writeMove!!!", e);
        }
    }

    private Map<Items, ByteArrayOutputStream> getMap() {
        //Заполнить коллекцию байтовыми массивами для записи координат
        Map<Items, ByteArrayOutputStream> map = new HashMap<Items, ByteArrayOutputStream>();
        Items[] items = Items.values();

        for (Items i : items) {
            map.put(i, new ByteArrayOutputStream());
        }

        try {// вектор по умолчанию
            map.get(Items.X).write(String.valueOf(Double.MIN_VALUE).getBytes());
            map.get(Items.Y).write(String.valueOf(Double.MIN_VALUE).getBytes());
            map.get(Items.Z).write(String.valueOf(Double.MIN_VALUE).getBytes());
            map.get(Items.U).write("0.0".getBytes());
            map.get(Items.V).write("0.0".getBytes());
            map.get(Items.W).write("1.0".getBytes());
        } catch (IOException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка IOException в методе getMap!!!", e);
        }

        return map;
    }

    private enum Items {
        X,
        Y,
        Z,
        U,
        V,
        W
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
                                            if (feed < 1.0) feed = 1.0;
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

    private void createMoveToPoint(int b) {
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
                        movePoint(pointVector, feed, operName);
                    }
                }
            }
        }
    }

    private void getItems(int b) {
        //получить массивы байт X-Y-Z-U-V-W
        if (b == 120) {//если есть x
            map.get(Items.X).reset();
            getItem(Items.X);
        } else if (b == 121) {//если есть y
            map.get(Items.Y).reset();
            getItem(Items.Y);
        } else if (b == 122) {//если есть z
            map.get(Items.Z).reset();
            getItem(Items.Z);
        } else if (b == 117) {//если есть u
            map.get(Items.U).reset();
            getItem(Items.U);
        } else if (b == 118) {//если есть v
            map.get(Items.V).reset();
            getItem(Items.V);
        } else if (b == 119) {//если есть w
            map.get(Items.W).reset();
            getItem(Items.W);
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

        try {

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

        } catch (IllegalArgumentException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка IllegalArgumentException в методе getParseArrayDouble!!!", e);
        }
    }

    private boolean isEmptyFirstPoint(double[] pointVector) {
        //проверить что первая точка получила координаты x, y, z

        for (int i = 0; i < 3; i++) {
            if (pointVector[i] == Double.MIN_VALUE) {
                break;
            } else {
                return false;
            }
        }

        return true;
    }

    private void movePoint(double[] pointVector, double feed, String operName) {
        if (isEmptyFirstPoint(pointVector)) return; //если 3 координаты не найдены не выполнять тело метода
        // //////////////////////////////////////////////////////////////////////////////////////////////////

        try {
            Nx nx = new Nx();
            nxopen.Part workPart =nx.getWorkPart();
            nxopen.cam.CAMSetup setup = nx.getSetup();

            nxopen.cam.MillUserDefined millUserDefined = (MillUserDefined) setup.camoperationCollection().findObject(operName);

            nxopen.Point3d point3d = new nxopen.Point3d(pointVector[0], pointVector[1],pointVector[2]);
            nxopen.Point point = workPart.points().createPoint(point3d);

            nxopen.cam.MoveToPointBuilder moveBuilder = millUserDefined.cammoveCollection().createMoveToPointBuilder(null);
            moveBuilder.setPoint(point);

            moveBuilder.commit();
            moveBuilder.destroy();
//            builder.destroy();
//            toolPathBuilder.destroy();

        } catch (NXException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка NXException в методе movePoint!!!", e);
        } catch (RemoteException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка RemoteException в методе movePoint!!!", e);
        } catch (Throwable e) {
            new PrintLog(Level.WARNING, "!!!Ошибка Throwable в методе movePoint!!!", e);
        }
    }

//    private void updatingSession() {
//        try {
//            Nx nx = new Nx();
//            nxopen.Session theSession = nx.getSession();
//            theSession.applicationSwitchImmediate("UG_APP_MANUFACTURING");
//        } catch (NXException e) {
//            new PrintLog(Level.WARNING, "!!!Ошибка NXException в методе updatingSession!!!", e);
//        } catch (RemoteException e) {
//            new PrintLog(Level.WARNING, "!!!Ошибка RemoteExceptionв методе updatingSession!!!", e);
//        }
//    }

}
