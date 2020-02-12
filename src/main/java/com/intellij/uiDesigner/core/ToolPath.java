package com.intellij.uiDesigner.core;

import nxopen.NXException;
import nxopen.uf.UFPath;
import nxopen.uf.UFVariant;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

class ToolPath {

    private int b;//прочитанный байт
    private ByteArrayInputStream reader;
    private ByteArrayOutputStream writer;
    private static double feed = -1.0;//значение подачи, если "-1", тогда RAPID
    private static double prevFeed = 0.0;//последнее выведенное значение подачи
    private UFPath.MotionType prevMotionType = UFPath.MotionType.MOTION_TYPE_RAPID;// предъидущий тип движения
    private Map<Items, ByteArrayOutputStream> map;
    private double[] pointVector = new double[6];
    private String operName = "";
    private nxopen.UFSession ufSession;
    private UFVariant pathPrt;
    private UFPath.LinearMotion linearMotion;
    private double[] sys;

    ToolPath() {
        map = getMap();//Заполнить коллекцию пустыми байтовыми массивами для записи координат
    }

    void writeMove(String operName, nxopen.UFSession ufSession, UFVariant pathPrt, UFPath.LinearMotion linearMotion) {
        this.ufSession = ufSession;
        this.pathPrt = pathPrt;
        this.linearMotion = linearMotion;

        sys = SystemCoordinateBlank.getOrigin();

        try {
//            reader = new ByteArrayInputStream(new Fis(pairOperFile.get(operName)).readAllBytes());
            reader = new ByteArrayInputStream(new Fis(Operation.getPairOperFile().get(operName)).readAllBytes());

            while ((b = reader.read()) >= 0) {
                getFeed(b);
                createMoveToPoint(b);
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
                                    if (b == 88 || b == 90) {//"X" или "Z"
                                        feed = b == 88 ? Feed.getFeedX() : Feed.getFeedZ();

                                        b = reader.read();
                                        if (b == 42) {//"*"
                                            writer = new ByteArrayOutputStream();

                                            while ((b = reader.read()) != 41) {//читать до ")" конца числа
                                                writer.write(b);
                                            }

                                            double k = Double.parseDouble(writer.toString());
                                            feed = Math.round(feed * k);
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
                        if (b == 76 && feed == -1.0) feed = Feed.getFeedX();

                        reader.skip(5l);//пропустить минимум 5 байт "[1]( "

                        while ((b = reader.read()) != 13) {//читать до конца строки
                            getItems(b);
                        }

                        getParseArrayDouble(map);

                        if (!isEmptyFirstPoint(pointVector)) {
                            createGoto();
                        }
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

    private void createGoto() {
        try {
            linearMotion.feedUnit = UFPath.FeedUnit.FEED_UNIT_PER_MINUTE;

            if (feed == -1.0) {
                prevMotionType = UFPath.MotionType.MOTION_TYPE_RAPID;
                linearMotion.type = prevMotionType;
                prevFeed = feed;
                linearMotion.feedValue = 0.0;
            } else {
                linearMotion.feedValue = feed;
                if (prevFeed != feed) {
                    if (prevMotionType == UFPath.MotionType.MOTION_TYPE_CUT) {
                        prevMotionType = UFPath.MotionType.MOTION_TYPE_STEPOVER;
                        linearMotion.type = prevMotionType;
                    } else {
                        prevMotionType = UFPath.MotionType.MOTION_TYPE_CUT;
                        linearMotion.type = prevMotionType;
                    }
                }

                prevFeed = feed;
            }

            //расчет точки относительно привязки на станке
            double x = ((pointVector[0] * sys[0]) + (pointVector[1] * sys[3]) + (pointVector[2] * sys[6]) + sys[9]);
            double y = ((pointVector[0] * sys[1]) + (pointVector[1] * sys[4]) + (pointVector[2] * sys[7]) + sys[10]);
            double z = ((pointVector[0] * sys[2]) + (pointVector[1] * sys[5]) + (pointVector[2] * sys[8]) + sys[11]);
            double[] pos = {x, y, z};
            linearMotion.position = pos;

            //расчет вектора инструмента относительно привязки на станке
            double vX = (pointVector[3] * sys[0]) + (pointVector[4] * sys[3]) + (pointVector[5] * sys[6]);
            double vY = (pointVector[3] * sys[1]) + (pointVector[4] * sys[4]) + (pointVector[5] * sys[7]);
            double vZ = (pointVector[3] * sys[2]) + (pointVector[4] * sys[5]) + (pointVector[5] * sys[8]);
            double[] tAxis = {vX, vY, vZ};
            linearMotion.toolAxis = tAxis;

            ufSession.path().createLinearMotion(pathPrt, linearMotion);
        } catch (NXException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка NXException в методе createGoto!!!", e);
        } catch (RemoteException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка RemoteException в методе createGoto!!!", e);
        }
    }
}
