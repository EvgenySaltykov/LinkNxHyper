package com.intellij.uiDesigner.core;

import nxopen.NXException;
import nxopen.cam.CAMSetup;
import nxopen.cam.NCGroupCollection;

import javax.swing.*;
import java.io.File;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Tool {
    private File file;

    private Map<Enum, Pair<Double, Pattern>> itemD;//параметры инструмента типа Double
    private Map<Enum, Pair<Integer, Pattern>> itemI;//параметры инструмента типа Integer

    private String typeTool = ""; //тип инструмента ballMill EndMill RadiusMill
    private final Pattern patternTypeTool = Pattern.compile("^11: vextToolType\\(");

    private boolean isShank = false; // флаг наличия хвостовика
    private final Pattern patternShankTool = Pattern.compile("^11: vtoolShaftType\\(parametric\\)");

    private boolean isHolder = false; //флаг наличия патрона
    private final Pattern patternFlagHolderTool = Pattern.compile("^11: *oL\\( x\\[/");

    private ArrayList<Pair<Double, Double>> itemHolder = new ArrayList<Pair<Double, Double>>();
    private final Pattern patternItemHolderTool = Pattern.compile("^11: *oL\\( x\\[/");

    private String nameTool = "Имя_инструмента_не_найдено"; //имя инструмента
    private final Pattern patternName = Pattern.compile(".*cfg\\(\\*WKZKOMMENTAR ");
    private boolean isFindNameTool = false;

    Tool(File file) {
        this.file = file;
        itemD = initItemToolDouble();
        itemI = initItemToolInt();
        findParam();
        createTool();
    }

    private void findParam() {
        ReadFile reader = new ReadFile(this.file);
        String stringIn;
        int maxReadLine = 4000; //максимальное колличество прочтенных сторк, чтобы не читать весь файл целиком

        while (reader.ready() && maxReadLine > 0) {
            stringIn = reader.getLine();

            if (isFindNameTool) {
                break;
            } else {

                fillMapItemDouble(stringIn, itemD);
                fillMapItemInteger(stringIn, itemI);

                findTypeTool(stringIn, patternTypeTool.matcher(stringIn));
                findShankTool(stringIn, patternShankTool.matcher(stringIn));
                findFlagHolderTool(stringIn, patternFlagHolderTool.matcher(stringIn));
                findItemHolder(stringIn, patternItemHolderTool.matcher(stringIn));
                findName(stringIn, patternName.matcher(stringIn));
            }

            maxReadLine -= 1;
        }
    }

    private void createTool() {
        try {
            Nx nx = new Nx();
            nxopen.cam.CAMSetup setup = nx.getSetup();

            nxopen.cam.NCGroup machineRoot = setup.getRoot(CAMSetup.View.MACHINE_TOOL);
            nxopen.cam.NCGroupCollection groups = setup.camgroupCollection();

            String[] listTool = getToolList(groups);
            if (!isNewTool(nameTool, listTool)) {
                return; //если имя интсрумента уже создано прервать построение инструмента
            }

            if (typeTool.equals("BALL_MILL")) {
                createBallMill(groups, machineRoot);
            } else if (typeTool.equals("MILL")) {
                createEndMill(groups, machineRoot);
            } else if (typeTool.equals("SPHERICAL_MILL")) {
                createSphericalMill(groups, machineRoot);
            } else {
                JOptionPane.showMessageDialog(null, "Не удалось определить тип инструмента!", "", JOptionPane.WARNING_MESSAGE);
                PrintLog.closeLogFile(); //закрыть файл log.txt
                MainForm.fr.setVisible(false);
                MainForm.fr.dispose();   //закрыть программу
            }

        } catch (NXException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка NXException в методе  createTool()!!!", e);
        } catch (RemoteException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка RemoteException в методе  createTool()!!!", e);
        }
    }

    private String[] getToolList(nxopen.cam.NCGroupCollection groups) {
        ArrayList<String> tempListTool = new ArrayList<String>();
        String[] toolList;

        try {
            nxopen.cam.NCGroup group;

            for (Iterator i = groups.iterator(); i.hasNext(); ) {
                group = (nxopen.cam.NCGroup) i.next();

                if (group instanceof nxopen.cam.Tool) {
                    nxopen.cam.Tool tool = (nxopen.cam.Tool) group;
                    tempListTool.add(tool.name());
                }
            }

        } catch (NXException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка NXException в методе  getToolList!!!", e);
        } catch (RemoteException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка RemoteException в методе  getToolList!!!", e);
        }

        if (tempListTool.size() > 0) {
            toolList = new String[tempListTool.size()];

            for (int i = 0; i < tempListTool.size(); i++) toolList[i] = tempListTool.get(i);
        } else {
            toolList = new String[0];
        }

        return toolList;
    }

    private boolean isNewTool(String nameTool, String[] listTool) {

        for (String tool : listTool) {
            if (tool.equals(nameTool.toUpperCase())) return false;
        }

        return true;
    }

    private enum varDbl {
        // Имена переменных типа double
        diamTool,
        diamShankSphereTool,
        cornerRadTool,
        angleTool,
        lengthTool,
        shankLengthTool,
        cutLengthTool,
        diamShankTool,
        chamferLengthShankTool
    }

    private HashMap<Enum, Pair<Double, Pattern>> initItemToolDouble() {
        //заполнить нулями все параметры инструмента типа double

        HashMap<Enum, Pair<Double, Pattern>> map = new HashMap<Enum, Pair<Double, Pattern>>();

        Pattern dT = Pattern.compile("^11: vtoolDiameter\\(");
        map.put(varDbl.diamTool, new Pair<Double, Pattern>(0.0, dT));

        Pattern dSST = Pattern.compile("^11: vtipDiameter\\(");
        map.put(varDbl.diamShankSphereTool, new Pair<Double, Pattern>(0.0, dSST));

        Pattern cRT = Pattern.compile("^11: vcornerRadius\\(");
        map.put(varDbl.cornerRadTool, new Pair<Double, Pattern>(0.0, cRT));

        Pattern aT = Pattern.compile("^11: vtaperAngle\\(");
        map.put(varDbl.angleTool, new Pair<Double, Pattern>(0.0, aT));

        Pattern lT = Pattern.compile("^11: vtaperHeight\\(");
        map.put(varDbl.lengthTool, new Pair<Double, Pattern>(0.0, lT));

        Pattern sLT = Pattern.compile("^11: vtoolTotalLength\\(");
        map.put(varDbl.shankLengthTool, new Pair<Double, Pattern>(0.0, sLT));

        Pattern cLT = Pattern.compile("^11: vcuttingLength\\(");
        map.put(varDbl.cutLengthTool, new Pair<Double, Pattern>(0.0, cLT));

        Pattern dST = Pattern.compile("^11: vtoolShaftDiameter\\(");
        map.put(varDbl.diamShankTool, new Pair<Double, Pattern>(0.0, dST));

        Pattern cLST = Pattern.compile("^11: vtoolShaftChamferLength\\(");
        map.put(varDbl.chamferLengthShankTool, new Pair<Double, Pattern>(0.0, cLST));

        return map;
    }

    private void fillMapItemDouble(String stringIn, Map<Enum, Pair<Double, Pattern>> map) {
        //получить параметры инструмента типа Double

        for (Map.Entry<Enum, Pair<Double, Pattern>> enumPairEntry : map.entrySet()) {
            findItemDouble(stringIn, enumPairEntry.getValue());
        }
    }

    private void findItemDouble(String stringIn, Pair<Double, Pattern> pair) {
        Matcher matcher = pair.getE().matcher(stringIn);

        if (matcher.find()) {
            pair.setT(Double.parseDouble(stringIn.substring(matcher.end(), (stringIn.length() - 1))));
        }
    }

    private enum varInt {
        // Имена переменных типа int
        idTool,
        numberTool
    }

    private HashMap<Enum, Pair<Integer, Pattern>> initItemToolInt() {
        //заполнить нулями все параметры инструмента типа Integer

        HashMap<Enum, Pair<Integer, Pattern>> map = new HashMap<Enum, Pair<Integer, Pattern>>();

        Pattern id = Pattern.compile("^11: vtoolIDs\\(");
        map.put(varInt.idTool, new Pair<Integer, Pattern>(0, id));

        Pattern nT = Pattern.compile(".*cfg\\(\\*WKZNUMMER ");
        map.put(varInt.numberTool, new Pair<Integer, Pattern>(0, nT));

        return map;
    }

    private void fillMapItemInteger(String stringIn, Map<Enum, Pair<Integer, Pattern>> map) {
        //получить параметры инструмента типа Integer

        for (Map.Entry<Enum, Pair<Integer, Pattern>> enumPairEntry : map.entrySet()) {
            findItemInteger(stringIn, enumPairEntry.getValue());
        }
    }

    private void findItemInteger(String stringIn, Pair<Integer, Pattern> pair) {
        Matcher matcher = pair.getE().matcher(stringIn);

        if (matcher.find()) {
            pair.setT(Integer.parseInt(stringIn.substring(matcher.end(), (stringIn.length() - 1))));
        }
    }

    private void createBallMill(nxopen.cam.NCGroupCollection groups, nxopen.cam.NCGroup machineRoot) {
        try {
            nxopen.cam.NCGroupCollection.UseDefaultName camFalse = NCGroupCollection.UseDefaultName.FALSE;
            nxopen.cam.NCGroup toolGroup;
            toolGroup = groups.createTool(machineRoot, "mill_planar", typeTool, camFalse, nameTool);

            nxopen.cam.Tool myTool = (nxopen.cam.Tool) toolGroup;

            nxopen.cam.MillToolBuilder toolBuilder = groups.createMillToolBuilder(myTool);
            toolBuilder.tlHeightBuilder().setValue(itemD.get(varDbl.lengthTool).getT());
            toolBuilder.tlDiameterBuilder().setValue(itemD.get(varDbl.diamTool).getT());
            toolBuilder.tlFluteLnBuilder().setValue(itemD.get(varDbl.cutLengthTool).getT());
            toolBuilder.tlTaperAngBuilder().setValue(itemD.get(varDbl.angleTool).getT());

            createShank(toolBuilder);
            createHolder(toolBuilder);
            createIdTool(toolBuilder);

            toolBuilder.commit();
            toolBuilder.destroy();
        } catch (NXException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка NXException в методе  createBallMill!!!", e);
        } catch (RemoteException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка RemoteException в методе  createBallMill!!!", e);
        }
    }

    private void createEndMill(nxopen.cam.NCGroupCollection groups, nxopen.cam.NCGroup machineRoot) {
        try {
            nxopen.cam.NCGroupCollection.UseDefaultName camFalse = NCGroupCollection.UseDefaultName.FALSE;
            nxopen.cam.NCGroup toolGroup;
            toolGroup = groups.createTool(machineRoot, "mill_planar", typeTool, camFalse, nameTool);

            nxopen.cam.Tool myTool = (nxopen.cam.Tool) toolGroup;

            nxopen.cam.MillToolBuilder toolBuilder = groups.createMillToolBuilder(myTool);
            toolBuilder.tlHeightBuilder().setValue(itemD.get(varDbl.lengthTool).getT());
            toolBuilder.tlDiameterBuilder().setValue(itemD.get(varDbl.diamTool).getT());
            toolBuilder.tlFluteLnBuilder().setValue(itemD.get(varDbl.cutLengthTool).getT());
            toolBuilder.tlCor1RadBuilder().setValue(itemD.get(varDbl.cornerRadTool).getT());

            createShank(toolBuilder);
            createHolder(toolBuilder);
            createIdTool(toolBuilder);

            toolBuilder.commit();
            toolBuilder.destroy();
        } catch (NXException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка NXException в методе  createEndMill!!!", e);
        } catch (RemoteException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка RemoteException в методе  createEndMill!!!", e);
        }
    }

    private void createSphericalMill(nxopen.cam.NCGroupCollection groups, nxopen.cam.NCGroup machineRoot) {
        try {
            nxopen.cam.NCGroupCollection.UseDefaultName camFalse = NCGroupCollection.UseDefaultName.FALSE;
            nxopen.cam.NCGroup toolGroup;
            toolGroup = groups.createTool(machineRoot, "mill_planar", typeTool, camFalse, nameTool);

            nxopen.cam.Tool myTool = (nxopen.cam.Tool) toolGroup;

            nxopen.cam.MillToolBuilder toolBuilder = groups.createMillToolBuilder(myTool);
            toolBuilder.tlHeightBuilder().setValue(itemD.get(varDbl.lengthTool).getT());
            toolBuilder.tlShankDiaBuilder().setValue(itemD.get(varDbl.diamShankSphereTool).getT());
            toolBuilder.tlDiameterBuilder().setValue(itemD.get(varDbl.diamTool).getT());

            createShank(toolBuilder);
            createHolder(toolBuilder);
            createIdTool(toolBuilder);

            toolBuilder.commit();
            toolBuilder.destroy();
        } catch (NXException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка NXException в методе  createSphericalMill!!!", e);
        } catch (RemoteException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка RemoteException в методе  createSphericalMill!!!", e);
        }
    }

    private void createShank(nxopen.cam.MillToolBuilder toolBuilder) {
        // создать хвостовик инструмента
        if (isShank) {
            try {
                toolBuilder.setUseTaperedShank(true);
                toolBuilder.taperedShankDiameterBuilder().setValue(itemD.get(varDbl.diamShankTool).getT());
                toolBuilder.taperedShankLengthBuilder().setValue(itemD.get(varDbl.shankLengthTool).getT() - itemD.get(varDbl.lengthTool).getT());
                toolBuilder.taperedShankTaperLengthBuilder().setValue(itemD.get(varDbl.chamferLengthShankTool).getT());
            } catch (NXException e) {
                new PrintLog(Level.WARNING, "!!!Ошибка NXException в методе  createShank!!!", e);
            } catch (RemoteException e) {
                new PrintLog(Level.WARNING, "!!!Ошибка RemoteException в методе  createShank!!!", e);
            }
        }
    }

    private void createHolder(nxopen.cam.MillToolBuilder toolBuilder) {
        // создать патрон
        try {
            double lowerDiam;
            double upperDiam;
            double length;
            if (isHolder) {
                for (int i = 0; i < (itemHolder.size() - 2); i++) {
                    lowerDiam = itemHolder.get(i).getT() * 2;
                    upperDiam = itemHolder.get(i + 1).getT() * 2;
                    length = itemHolder.get(i + 1).getE() - itemHolder.get(i).getE();

                    if (length > 0.0001) {
                        toolBuilder.holderSectionBuilder().addByUpperDiameter(i, lowerDiam, length, upperDiam, 0.0);
                    }
                }
            }
        } catch (NXException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка NXException в методе  createHolder!!!", e);
        } catch (RemoteException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка RemoteException в методе  createHolder!!!", e);
        }
    }

    private void createIdTool(nxopen.cam.MillToolBuilder toolBuilder) {
        // установить номер инструмента, номер корректора, и т.д.
        try {
            toolBuilder.tlNumberBuilder().setValue(itemI.get(varInt.numberTool).getT());
            toolBuilder.tlAdjRegBuilder().setValue(itemI.get(varInt.idTool).getT());
            toolBuilder.tlCutcomRegBuilder().setValue(itemI.get(varInt.idTool).getT());
            toolBuilder.setDescription("Tool Exported HyperMill");
        } catch (NXException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка NXException в методе  createIdTool!!!", e);
        } catch (RemoteException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка RemoteException в методе  createIdTool!!!", e);
        }
    }

    private void findTypeTool(String stringIn, Matcher matcher) {
        if (matcher.find()) {
            String type = stringIn.substring(matcher.end(), (stringIn.length() - 1));

            if (type.equals("ballMill")) {
                typeTool = "BALL_MILL";
            } else if (type.equals("endMill") || type.equals("radiusMill")) {
                typeTool = "MILL";
            } else if (type.equals("lollipop")) {
                typeTool = "SPHERICAL_MILL";
            } else {
                JOptionPane.showMessageDialog(null, "Не удалось определить тип инструмента, обратитесь к разработчику.", "", JOptionPane.WARNING_MESSAGE);
                new PrintLog(Level.WARNING, "!!!Ошибка определения типа инструмента. method: findTypeTool!!!");
                PrintLog.closeLogFile(); //закрыть файл log.txt
                MainForm.fr.setVisible(false);
                MainForm.fr.dispose();   //закрыть программу
            }

//            typeTool = stringIn.substring(matcher.end(), (stringIn.length() - 1)).equals("1") ? "BALL_MILL" : "MILL";
        }
    }

    private void findShankTool(String stringIn, Matcher matcher) {
        if (matcher.find()) {
            isShank = true;
        }
    }

    private void findFlagHolderTool(String stringIn, Matcher matcher) {
        if (matcher.find()) {
            isHolder = true;
        }
    }

    private void findItemHolder(String stringIn, Matcher matcher) {
        if (matcher.find()) {
            String subStr;
            subStr = stringIn.substring(matcher.end());

            double itemX = Double.parseDouble(subStr.substring(0, subStr.indexOf("]")));
            double itemY = Double.parseDouble(subStr.substring((subStr.indexOf("/") + 1), subStr.lastIndexOf("]")));

            itemHolder.add(new Pair(itemX, itemY));
        }
    }

    private void findName(String stringIn, Matcher matcher) {
        if (matcher.find()) {
            nameTool = stringIn.substring(matcher.end(), (stringIn.length() - 1)).replace(" ", "_").replace(":", "_");
            isFindNameTool = true;
        }
    }

    String getFindNameTool() {
        return this.nameTool;
    }
}

