package com.intellij.uiDesigner.core;

import nxopen.NXException;
import nxopen.cam.CAMSetup;
import nxopen.cam.OperationCollection;

import java.io.*;
import java.rmi.RemoteException;
import java.util.Date;
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
            BufferedWriter writerFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileOut), "UTF-8"));
            ByteArrayOutputStream writerBuffer;


            for (String file : listFile) {//перебрать все POF-файлы в директории
                File fileIn = new File(path + "\\" + file);

                createOperation(fileIn, writerFile);

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

    private void createOperation(File fileIn, BufferedWriter writer) {

        String msysName = new SystemCoordinateBlank(fileIn).getMSysName();
        String toolName = new Tool(fileIn).getNameTool();
        String groupProgramName = new Operation(fileIn).getNameGroupProgram();
        String operName = new Operation(fileIn).getNameOper();
        int spindleSpeed = new SpindleSpeed(fileIn).getSpeed();
        int feed = new Feed(fileIn).getFeed();

        createGroupProgram(groupProgramName);

        try {
            Nx nx = new Nx();
            nxopen.Part workPart = nx.getWorkPart();
            nxopen.cam.CAMSetup setup = nx.getSetup();

            nxopen.cam.NCGroup prog = setup.camgroupCollection().findObject(groupProgramName);
            nxopen.cam.Method method = (nxopen.cam.Method) setup.camgroupCollection().findObject("METHOD");
            nxopen.cam.Tool tool = (nxopen.cam.Tool) setup.camgroupCollection().findObject(toolName);
            nxopen.cam.OrientGeometry geometry = (nxopen.cam.OrientGeometry) setup.camgroupCollection().findObject(msysName);

            nxopen.cam.Operation operation = setup.camoperationCollection().create(prog, method, tool, geometry, "mill_multi-axis", "MILL_USER",
                    OperationCollection.UseDefaultName.FALSE, operName);

            nxopen.cam.MillUserDefined millUserDefined =(nxopen.cam.MillUserDefined) operation;
            nxopen.cam.MillUserDefinedBuilder builder = setup.camoperationCollection().createMillUserDefinedBuilder(millUserDefined);

            builder.setEnvVarName("HyperMill");

            //генерировать траекторию
            nxopen.NXObject nXObject1 = builder.commit();
            nxopen.cam.CAMObject [] objects1  = new nxopen.cam.CAMObject[1];
            nxopen.cam.MillUserDefined millUserDefined2 = ((nxopen.cam.MillUserDefined)nXObject1);
            objects1[0] = millUserDefined2;
            workPart.camsetup().generateToolPath(objects1);

            builder.destroy();

        } catch (NXException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка NXException в методе createOperation!!!", e);
        } catch (RemoteException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка RemoteException в методе createOperation!!!", e);
        }



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

//    static String doubleFormat(String pattern, double value) {
//        //округляет тип double, согласно шаблону
//        try {
//            Locale locale = new Locale("en", "UK");
//            DecimalFormat decimalFormat = (DecimalFormat) NumberFormat.getNumberInstance(locale);
//            decimalFormat.applyLocalizedPattern(pattern);
//            String roundString = decimalFormat.format(value);
//
//            return roundString;
//        } catch (IllegalArgumentException e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
//
//    static void writeEndOperation(BufferedWriter writer) {
//        try {
//            writer.write("PAINT/SPEED,10");
//            writer.newLine();
//            writer.write("PAINT/TOOL,NOMORE");
//            writer.newLine();
//            writer.write("END-OF-PATH");
//            writer.newLine();
//        } catch(IOException e) {
//            new PrintLog(Level.WARNING, "!!!Ошибка записи CLS-файла!!!", e);
//        }
//    }

    private void createGroupProgram(String groupProgramName) {
        boolean isEmptyProgram = true;

        try {
            Nx nx = new Nx();
            nxopen.Part workPart = nx.getWorkPart();
            nxopen.cam.CAMSetup setup = nx.getSetup();

            nxopen.cam.NCGroup programRoot = setup.getRoot(CAMSetup.View.PROGRAM_ORDER);
            nxopen.cam.CAMObject[] programRootMembers = programRoot.getMembers();

            nxopen.cam.NCGroup group = setup.camgroupCollection().findObject("NC_PROGRAM");

            String[] listProgram = Nx.getListMembers(programRootMembers);
            for (String programName : listProgram) {
                if (programName.equals(groupProgramName.toUpperCase())) {
                    isEmptyProgram = false;
                }
            }

            if (isEmptyProgram) {
                nxopen.cam.NCGroup prog = workPart.camsetup().camgroupCollection().createProgram(group,  "mill_planar", "PROGRAM", nxopen.cam.NCGroupCollection.UseDefaultName.FALSE, groupProgramName);
                nxopen.cam.ProgramOrderGroupBuilder programBuilder = workPart.camsetup().camgroupCollection().createProgramOrderGroupBuilder(prog);
                programBuilder.commit();
                programBuilder.destroy();
            }
        } catch (NXException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка NXException в методе  createGroupProgram!!!", e);
        } catch (RemoteException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка RemoteException в методе  createGroupProgram!!!", e);
        }
    }
}
