package com.intellij.uiDesigner.core;

import nxopen.NXException;
import nxopen.cam.CAMSetup;
import nxopen.cam.OperationCollection;

import javax.swing.*;
import java.io.BufferedWriter;
import java.io.File;
import java.rmi.RemoteException;
import java.util.logging.Level;

class ParserFile {
    private File path;
    private String[] listFile;
    private BufferedWriter writer;
    private String operName;

    ParserFile(File path, String[] listFile) {
        this.path = path;
        this.listFile = listFile;

        for (String file : listFile) {//перебрать все POF-файлы в директории
            File fileIn = new File(path + "\\" + file);

            createOperation(fileIn);
        }
    }

    private void createOperation(File fileIn) {

        String msysName = new SystemCoordinateBlank(fileIn).getMSysName();
        String toolName = new Tool(fileIn).getNameTool();
        String groupProgramName = new Operation(fileIn).getNameGroupProgram();
        this.operName = new Operation(fileIn).getNameOper();
        int spindleSpeed = new SpindleSpeed(fileIn).getSpeed();
        new Feed(fileIn);
        int feed = Feed.getFeed();

        createGroupProgram(groupProgramName);

        createOperation(groupProgramName, operName, toolName, msysName);

        setSpeedAndFeedForOperation(operName, spindleSpeed, feed);
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
                nxopen.cam.MillUserDefined millUserDefined = ((nxopen.cam.MillUserDefined) operation);
                nxopen.cam.MillUserDefinedBuilder builder = setup.camoperationCollection().createMillUserDefinedBuilder(millUserDefined);
                builder.setSelectToolFlag(true);
                builder.setEnvVarName("EXP_HYPER");

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
            new PrintLog(Level.WARNING, "!!!Ошибка NXException в методе createOperation!!!", e);
        } catch (RemoteException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка RemoteException в методе createOperation!!!", e);
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
            nxopen.cam.ObjectsFeedsBuilder builder = setup.createFeedsBuilder(objects);

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
}
