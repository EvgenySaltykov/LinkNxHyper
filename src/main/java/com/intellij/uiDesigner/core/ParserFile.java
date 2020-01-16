package com.intellij.uiDesigner.core;

import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;

class ParserFile {
    private File path;
    private String[] listFile;
    private BufferedWriter writer;

    ParserFile(File path, String[] listFile) {
        this.path = path;
        this.listFile = listFile;

        try {
long start = new Date().getTime();
            File fileOut = new File(path + "\\" + "exportPof.cls");
//            BufferedWriter writerFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileOut),"cp1251"));
            BufferedWriter writerFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileOut),"UTF-8"));
            ByteArrayOutputStream writerBuffer;


            for (String file : listFile) {//перебрать все POF-файлы в директории
                File fileIn = new File(path + "\\" + file);

                writeInitParam(fileIn, writerFile);

                //записать GOTO
//                writerBuffer  = new ByteArrayOutputStream();//переменная для записи строк в оперативную память
//                new WriteMove(fileIn, writerBuffer);
//                for (byte b : writerBuffer.toByteArray()) writerFile.write(b);//записать из оперативной памяти в файл
//                writerBuffer.close();
//
//                writeEndOperation(writerFile);
            }

//            writerFile.close();

long end = new Date().getTime();
System.out.println("время на операцию = " + (end - start));
        } catch (IOException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка чтения из pof-файла!!!", e);
        }
    }

    private void  writeInitParam(File fileIn , BufferedWriter writer) {

        new SystemCoordinateBlank(fileIn);
        ParamTool paramTool = new ParamTool(fileIn);
//        String nameTool = paramTool.getName();
//        int numberTool = paramTool.getToolNumber();
//        int idTool = paramTool.getToolId();
//        String mSYS = new SystemCoordinateBlank(fileIn).getMSYS();
//        String nameOper = new NameOperation(fileIn).getName();
//        int spindleSpeed = new SpindleSpeed(fileIn).getSpeed();
//        int feed = new Feed(fileIn).getFeed();

//        try {
//            writer.write("TOOL PATH/");
//            writer.write(nameOper);
//            writer.write(",TOOL,");
//            writer.write(nameTool);
//            writer.newLine();
//
//            writer.write("MSYS/");
//            writer.write(mSYS);
//            writer.newLine();
//
//            writer.write("PAINT/PATH");
//            writer.newLine();
//            writer.write("PAINT/SPEED,10");
//            writer.newLine();
//
//            writer.write("LOAD/TOOL,");
//            writer.write(String.valueOf(numberTool));
//            writer.write(",ADJUST,");
//            writer.write(String.valueOf(numberTool));
//            writer.newLine();
//
//            writer.write("SELECT/TOOL,");
//            writer.write(String.valueOf(numberTool));
//            writer.newLine();
//
//            writer.write("SPINDL/RPM,");
//            writer.write(String.valueOf((spindleSpeed)));
//            writer.write(",CLW");
//            writer.newLine();
//
//
//        }catch (IOException e) {
//            new PrintLog(Level.WARNING, "!!!Ошибка записи CLS-файла!!!", e);
//        }
//        System.out.println("");
    }

    static String doubleFormat(String pattern, double value) {
        //округляет тип double, согласно шаблону
        try {
            Locale locale = new Locale("en", "UK");
            DecimalFormat decimalFormat = (DecimalFormat) NumberFormat.getNumberInstance(locale);
            decimalFormat.applyLocalizedPattern(pattern);
            String roundString = decimalFormat.format(value);

            return roundString;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }

    static void writeEndOperation(BufferedWriter writer) {
        try {
            writer.write("PAINT/SPEED,10");
            writer.newLine();
            writer.write("PAINT/TOOL,NOMORE");
            writer.newLine();
            writer.write("END-OF-PATH");
            writer.newLine();
        } catch(IOException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка записи CLS-файла!!!", e);
        }
    }
}
