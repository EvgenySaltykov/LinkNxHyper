package com.intellij.uiDesigner.core;

import nxopen.NXException;
import nxopen.cam.CAMSetup;
import nxopen.cam.OperationCollection;

import javax.swing.*;
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

        long start = new Date().getTime();
//            File fileOut = new File(path + "\\" + "exportPof.cls");
////            BufferedWriter writerFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileOut),"cp1251"));
//            BufferedWriter writerFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileOut), "UTF-8"));
            ByteArrayOutputStream writerBuffer;


        for (String file : listFile) {//перебрать все POF-файлы в директории
            File fileIn = new File(path + "\\" + file);

            exportOperation(fileIn);

            //записать GOTO
//                writerBuffer  = new ByteArrayOutputStream();//переменная для записи строк в оперативную память
//                new ToolPath(fileIn, writerBuffer);
//                for (byte b : writerBuffer.toByteArray()) writerFile.write(b);//записать из оперативной памяти в файл
//                writerBuffer.close();
        }

//            writerFile.close();

        long end = new Date().getTime();
        JOptionPane.showMessageDialog(null, "время просчета " + (end - start), "", JOptionPane.INFORMATION_MESSAGE);
    }

    private void exportOperation(File fileIn) {

        String msysName = new SystemCoordinateBlank(fileIn).getMSysName();
        String toolName = new Tool(fileIn).getNameTool();
        String groupProgramName = new Operation(fileIn).getNameGroupProgram();
        String operName = new Operation(fileIn).getNameOper();
        int spindleSpeed = new SpindleSpeed(fileIn).getSpeed();
        new Feed(fileIn);
        int feed = Feed.getFeed();

        createGroupProgram(groupProgramName);

        createOperation(groupProgramName, operName, toolName, msysName);

        setSpeedAndFeedForOperation(operName, spindleSpeed, feed);

        createMove(fileIn, operName);
    }

    private void createGroupProgram(String groupProgramName) {
        //Создать группу программ для операций

        try {
            Nx nx = new Nx();
            nxopen.Part workPart = nx.getWorkPart();
            nxopen.cam.CAMSetup setup = nx.getSetup();

            nxopen.cam.NCGroup programRoot = setup.getRoot(CAMSetup.View.PROGRAM_ORDER);
            nxopen.cam.CAMObject[] programRootMembers = programRoot.getMembers();

            nxopen.cam.NCGroup group = setup.camgroupCollection().findObject("NC_PROGRAM");

            if (Nx.isEmptyName(groupProgramName, programRootMembers)) {
                nxopen.cam.NCGroup prog = workPart.camsetup().camgroupCollection().createProgram(group, "mill_planar", "PROGRAM", nxopen.cam.NCGroupCollection.UseDefaultName.FALSE, groupProgramName);
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

    private void createOperation(String groupProgramName, String operName, String toolName, String msysName) {
        // создать операцию в Nx
        try {
            Nx nx = new Nx();
            nxopen.cam.CAMSetup setup = nx.getSetup();

            nxopen.cam.NCGroup programRoot = setup.getRoot(CAMSetup.View.PROGRAM_ORDER);
            nxopen.cam.CAMObject[] programRootMembers = programRoot.getMembers();

            if (Nx.isEmptyName(operName, programRootMembers)) {
                nxopen.cam.NCGroup prog = setup.camgroupCollection().findObject(groupProgramName);
                nxopen.cam.Method method = (nxopen.cam.Method) setup.camgroupCollection().findObject("METHOD");
                nxopen.cam.Tool tool = (nxopen.cam.Tool) setup.camgroupCollection().findObject(toolName);
                nxopen.cam.OrientGeometry geometry = (nxopen.cam.OrientGeometry) setup.camgroupCollection().findObject(msysName);

                // создать операцию
                nxopen.cam.Operation operation = setup.camoperationCollection().create(prog, method, tool, geometry, "mill_multi-axis", "MILL_USER",
                        OperationCollection.UseDefaultName.FALSE, operName);

                // создать объект строитель nx-объектов
                nxopen.cam.MillUserDefined millUserDefined = (nxopen.cam.MillUserDefined) operation;
                nxopen.cam.MillUserDefinedBuilder builder = setup.camoperationCollection().createMillUserDefinedBuilder(millUserDefined);

                //Установть имя системной переменной и применить изменения
                builder.setEnvVarName("HyperMill");
                builder.commit();

                //генерировать траекторию
                nxopen.NXObject nXObject = builder.commit();
                nxopen.cam.CAMObject[] objects = new nxopen.cam.CAMObject[1];
                nxopen.cam.MillUserDefined millUserDefined2 = ((nxopen.cam.MillUserDefined) nXObject);
                objects[0] = millUserDefined2;
                setup.generateToolPath(objects);

                //Удалить построитель объектов
                builder.destroy();
            } else {
                String message = "!!!Операция с именем <<<".concat(operName).concat(">>> уже создана!!!");
                JOptionPane.showMessageDialog(null, message, "", JOptionPane.WARNING_MESSAGE);
                new PrintLog(Level.WARNING, "!!!Ошибка в методе createOperation, имя операции уже создано!!!");
            }
        } catch (NXException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка NXException в методе exportOperation!!!", e);
        } catch (RemoteException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка RemoteException в методе exportOperation!!!", e);
        }
    }

    private void setSpeedAndFeedForOperation(String progName, int spindleSpeed, int feed) {
        //установить подачу и скорость шпинделя на операцтю

        try {
            Nx nx = new Nx();
            nxopen.cam.CAMSetup setup = nx.getSetup();

            nxopen.cam.CAMObject[] objects = new nxopen.cam.CAMObject[1];
            nxopen.cam.MillUserDefined millUserDefined = ((nxopen.cam.MillUserDefined) setup.camoperationCollection().findObject(progName.toUpperCase()));
            objects[0] = millUserDefined;
            nxopen.cam.ObjectsFeedsBuilder builder;
            builder = setup.createFeedsBuilder(objects);

            builder.feedsBuilder().spindleRpmBuilder().setValue(spindleSpeed);
            builder.feedsBuilder().feedCutBuilder().setValue(feed);
            builder.feedsBuilder().recalculateData(nxopen.cam.FeedsBuilder.RecalcuateBasedOn.CUT_FEED_RATE);

            builder.commit();
            builder.destroy();

        } catch (NXException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка NXException в методе setSpeedAndFeed!!!", e);
        } catch (RemoteException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка RemoteException в методе setSpeedAndFeed!!!", e);
        }
    }

    private void createMove(File fileIn, String operName) {
        ByteArrayOutputStream writerBuffer;

            //записать GOTO
            writerBuffer  = new ByteArrayOutputStream();//переменная для записи строк в оперативную память
            new ToolPath(fileIn, operName);
//                for (byte b : writerBuffer.toByteArray()) writerFile.write(b);//записать из оперативной памяти в файл
//                writerBuffer.close();
//        }

//            writerFile.close();
    }
}
