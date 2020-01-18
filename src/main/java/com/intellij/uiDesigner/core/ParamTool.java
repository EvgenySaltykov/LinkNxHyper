package com.intellij.uiDesigner.core;

import nxopen.NXException;
import nxopen.cam.CAMSetup;
import nxopen.cam.NCGroupCollection;

import javax.swing.*;
import java.io.File;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ParamTool {
    private File file;

    private int idTool = 0; //корректор на длинну инструмента
    private final String ID_TOOL = "^11: vtoolIDs\\(";
    private final Pattern patternId = Pattern.compile(ID_TOOL);

    private void findId(String stringIn, Matcher matcher) {
        if (matcher.find()) {
            idTool = Integer.parseInt(stringIn.substring(matcher.end(), (stringIn.length() - 1)));
        }
    }

    private String typeTool = ""; //тип инструмента ballMill EndMill RadiusMill
    private final String TYPE_TOOL = "^11: vextToolType\\(";
    private final Pattern patternTypeTool = Pattern.compile(TYPE_TOOL);

    private void findTypeTool(String stringIn, Matcher matcher) {
        if (matcher.find()) {
            String type = stringIn.substring(matcher.end(), (stringIn.length() - 1));

            if (type.equals("ballMill")) {
                typeTool = "BALL_MILL";
            } else if (type.equals("endMill") || type.equals("radiusMill")) {
                typeTool = "MILL";
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

    private double diamTool = 0.0; //диаметр инструмента
    private final String DIAM_TOOL = "^11: vtoolDiameter\\(";
    private final Pattern patternDiamTool = Pattern.compile(DIAM_TOOL);

    private void findDiamTool(String stringIn, Matcher matcher) {
        if (matcher.find()) {
            diamTool = Double.parseDouble(stringIn.substring(matcher.end(), (stringIn.length() - 1)));
        }
    }

    private double cornerRadTool = 0.0; //угловой радиус инструмента
    private final String CORNER_RAD_TOOL = "^11: vcornerRadius\\(";
    private final Pattern patternCornerRadTool = Pattern.compile(CORNER_RAD_TOOL);

    private void findCornerTool(String stringIn, Matcher matcher) {
        if (matcher.find()) {
            cornerRadTool = Double.parseDouble(stringIn.substring(matcher.end(), (stringIn.length() - 1)));
        }
    }

    private double angleTool = 0.0; //угол при вершине у сферической фрезы
    private final String ANGLE_TOOL = "^11: vtaperAngle\\(";
    private final Pattern patternAngleTool = Pattern.compile(ANGLE_TOOL);

    private void findAngleTool(String stringIn, Matcher matcher) {
        if (matcher.find()) {
            angleTool = Double.parseDouble(stringIn.substring(matcher.end(), (stringIn.length() - 1)));
        }
    }

    private double lengthTool = 0.0; //длинна инструмента до хвостовика, или патрона
    private final String LENGTH_TOOL = "^11: vtaperHeight\\(";
    private final Pattern patternLengthTool = Pattern.compile(LENGTH_TOOL);

    private void findLengthTool(String stringIn, Matcher matcher) {
        if (matcher.find()) {
            lengthTool = Double.parseDouble(stringIn.substring(matcher.end(), (stringIn.length() - 1)));
        }
    }

    private double shankLengthTool = 0.0; // длинна хвостовика, если есть
    private final String SHANK_LENGTH_TOOL = "^11: vtoolTotalLength\\(";
    private final Pattern patternShankLengthTool = Pattern.compile(SHANK_LENGTH_TOOL);

    private void findShankLengthTool(String stringIn, Matcher matcher) {
        if (matcher.find()) {
            double totalLength = Double.parseDouble(stringIn.substring(matcher.end(), (stringIn.length() - 1)));
            shankLengthTool = totalLength - lengthTool;
        }
    }

    private double cutLengthTool = 0.0; //длинна режущей части
    private final String CUT_LENGTH_TOOL = "^11: vcuttingLength\\(";
    private final Pattern patternCutLengthTool = Pattern.compile(CUT_LENGTH_TOOL);

    private void findCutLengthTool(String stringIn, Matcher matcher) {
        if (matcher.find()) {
            cutLengthTool = Double.parseDouble(stringIn.substring(matcher.end(), (stringIn.length() - 1)));
        }
    }

    private boolean isShank = false; // флаг наличия хвостовика
    private final String SHANK_TOOL = "^11: vtoolShaftType\\(parametric\\)";
    private final Pattern patternShankTool = Pattern.compile(SHANK_TOOL);

    private void findShankTool(String stringIn, Matcher matcher) {
        if (matcher.find()) {
            isShank = true;
        }
    }

    private double diamShankTool = 0.0; //диаметр хвостовика
    private final String DIAM_SHANK_TOOL = "^11: vtoolShaftDiameter\\(";
    private final Pattern patternDiamShankTool = Pattern.compile(DIAM_SHANK_TOOL);

    private void findDiamShankTool(String stringIn, Matcher matcher) {
        if (matcher.find()) {
            diamShankTool = Double.parseDouble(stringIn.substring(matcher.end(), (stringIn.length() - 1)));
        }
    }

    private boolean isHolder = false; //флаг наличия патрона
    private final String FLAG_HOLDER_TOOL = "^11: *oL\\( x\\[/";
    private final Pattern patternFlagHolderTool = Pattern.compile(FLAG_HOLDER_TOOL);

    private void findFlagHolderTool(String stringIn, Matcher matcher) {
        if (matcher.find()) {
            isHolder = true;
        }
    }

    private ArrayList<HolderPairVar> itemHolder = new ArrayList<HolderPairVar>();
    private String itemHolderTool = "^11: *oL\\( x\\[/";
    private final Pattern patternItemHolderTool = Pattern.compile(itemHolderTool);

    private void findItemHolder(String stringIn, Matcher matcher) {
        if (matcher.find()) {
            String subStr;
            subStr = stringIn.substring(matcher.end());

            double itemX = Double.parseDouble(subStr.substring(0, subStr.indexOf("]")));
            double itemY = Double.parseDouble(subStr.substring((subStr.indexOf("/") + 1), subStr.lastIndexOf("]")));

            itemHolder.add(new HolderPairVar(itemX, itemY));
        }
    }

    private int numberTool = 0; //номер инструмента
    private final String NUMBER_TOOL = ".*cfg\\(\\*WKZNUMMER ";
    private final Pattern patternNumber = Pattern.compile(NUMBER_TOOL);

    private void findNumber(String stringIn, Matcher matcher) {
        if (matcher.find()) {
            numberTool = Integer.parseInt(stringIn.substring(matcher.end(), (stringIn.length() - 1)));
        }
    }

    private String nameTool = "Имя_инструмента_не_найдено"; //имя инструмента
    private final String PATTERN_NAME = ".*cfg\\(\\*WKZKOMMENTAR ";
    private final Pattern patternName = Pattern.compile(PATTERN_NAME);
    private boolean isNameTool = false;

    private void findName(String stringIn, Matcher matcher) {
        if (matcher.find()) {
            nameTool = stringIn.substring(matcher.end(), (stringIn.length() - 1)).replace(" ", "_").replace(":", "_");
            isNameTool = true;
        }
    }


    ParamTool(File file) {
        this.file = file;
        findParam();
        createTool();
    }

    private void findParam() {
        ReadFile reader = new ReadFile(this.file);
        String stringIn;
        int maxReadLine = 4000; //максимальное колличество прочтенных сторк, чтобы не читать весь файл целиком

        while (reader.ready() && maxReadLine > 0) {
            stringIn = reader.getLine();

            if (isNameTool) {
                break;
            } else {
                findId(stringIn, patternId.matcher(stringIn));
                findTypeTool(stringIn, patternTypeTool.matcher(stringIn));
                findDiamTool(stringIn, patternDiamTool.matcher(stringIn));
                findCornerTool(stringIn, patternCornerRadTool.matcher(stringIn));
                findAngleTool(stringIn, patternAngleTool.matcher(stringIn));
                findLengthTool(stringIn, patternLengthTool.matcher(stringIn));
                findShankLengthTool(stringIn, patternShankLengthTool.matcher(stringIn));
                findCutLengthTool(stringIn, patternCutLengthTool.matcher(stringIn));
                findShankTool(stringIn, patternShankTool.matcher(stringIn));
                findDiamShankTool(stringIn, patternDiamShankTool.matcher(stringIn));
                findFlagHolderTool(stringIn, patternFlagHolderTool.matcher(stringIn));
                findItemHolder(stringIn, patternItemHolderTool.matcher(stringIn));
                findNumber(stringIn, patternNumber.matcher(stringIn));
                findName(stringIn, patternName.matcher(stringIn));
            }

            maxReadLine -= 1;
        }
    }

    private void createTool() {
        try {
            nxopen.Session theSession = (nxopen.Session) nxopen.SessionFactory.get("Session");
            nxopen.Part workPart = theSession.parts().work();
            nxopen.cam.CAMSetup setup = workPart.camsetup();

            nxopen.cam.NCGroup machineRoot = setup.getRoot(CAMSetup.View.MACHINE_TOOL);
            nxopen.cam.CAMObject[] machineRootMembers = machineRoot.getMembers();

            nxopen.cam.NCGroupCollection groups = setup.camgroupCollection();
            nxopen.cam.NCGroupCollection.UseDefaultName camFalse = NCGroupCollection.UseDefaultName.FALSE;

            String[] listTool = getToolList(groups);
            if (!isNewTool(nameTool, listTool)) {
                return; //если имя интсрумента уже создано прервать построение инструмента
            }

            if (typeTool.equals("BALL_MILL")) {
                createBallMill(groups, machineRoot);
            }else if (typeTool.equals("MILL")) {
                createEndMill(groups, machineRoot);
            } else {
                JOptionPane.showMessageDialog(null, "Не удалось определить тип инструмента!", "", JOptionPane.WARNING_MESSAGE);
                PrintLog.closeLogFile(); //закрыть файл log.txt
                MainForm.fr.setVisible(false);
                MainForm.fr.dispose();   //закрыть программу
            }

        } catch (NXException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка NXException в методе  createTool()!!!", e);
            e.printStackTrace();
        } catch (RemoteException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка RemoteException в методе  createTool()!!!", e);
            e.printStackTrace();
        }
    }

    private String[] getToolList(nxopen.cam.NCGroupCollection groups) {
        ArrayList<String> tempListTool = new ArrayList<String>();
        String[] toolList;

        try {
            nxopen.cam.NCGroup group = null;

            for (Iterator i = groups.iterator(); i.hasNext(); ) {
                group = (nxopen.cam.NCGroup) i.next();

                if (group instanceof nxopen.cam.Tool) {
                    nxopen.cam.Tool tool = (nxopen.cam.Tool) group;
                    tempListTool.add(tool.name());
                }
            }

        } catch (NXException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка NXException в методе  getToolList!!!", e);
            e.printStackTrace();
        } catch (RemoteException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка RemoteException в методе  getToolList!!!", e);
            e.printStackTrace();
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
        if (listTool.length == 0) return false;

        for (String tool : listTool) {
            if (tool.equals(nameTool)) return false;
        }

        return true;
    }

    private void createBallMill(nxopen.cam.NCGroupCollection groups, nxopen.cam.NCGroup machineRoot) {
        try {
            nxopen.cam.NCGroupCollection.UseDefaultName camFalse = NCGroupCollection.UseDefaultName.FALSE;
            nxopen.cam.NCGroup toolGroup;
            toolGroup = groups.createTool(machineRoot, "mill_planar", typeTool, camFalse, nameTool);

            nxopen.cam.Tool myTool = (nxopen.cam.Tool) toolGroup;

            nxopen.cam.MillToolBuilder toolBuilder = groups.createMillToolBuilder(myTool);
            toolBuilder.tlHeightBuilder().setValue(lengthTool);
            toolBuilder.tlDiameterBuilder().setValue(diamTool);
            toolBuilder.tlFluteLnBuilder().setValue(cutLengthTool);
            toolBuilder.tlTaperAngBuilder().setValue(angleTool);
            if (isShank) {
                toolBuilder.setUseTaperedShank(true);
                toolBuilder.taperedShankDiameterBuilder().setValue(diamShankTool);
                toolBuilder.taperedShankLengthBuilder().setValue(shankLengthTool);
                toolBuilder.taperedShankTaperLengthBuilder().setValue(0.0);
            }

            double lowerDiam;
            double upperDiam;
            double length;
            if (isHolder) {
                for (int i = 0; i < (itemHolder.size() - 2); i++) {
                    lowerDiam = itemHolder.get(i).getX() * 2;
                    upperDiam = itemHolder.get(i + 1).getX() * 2;
                    length = itemHolder.get(i + 1).getY() - itemHolder.get(i).getY();

                    toolBuilder.holderSectionBuilder().addByUpperDiameter(i, lowerDiam, length, upperDiam, 0.0);
                }
            }
            toolBuilder.tlNumberBuilder().setValue(numberTool);
            toolBuilder.tlAdjRegBuilder().setValue(numberTool);
            toolBuilder.tlCutcomRegBuilder().setValue(numberTool);
            toolBuilder.setDescription("Tool Exported HyperMill");

            toolBuilder.commit();
            toolBuilder.destroy();
        } catch (NXException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка NXException в методе  createBallMill!!!", e);
            e.printStackTrace();
        } catch (RemoteException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка RemoteException в методе  createBallMill!!!", e);
            e.printStackTrace();
        }
    }

    private void createEndMill(nxopen.cam.NCGroupCollection groups, nxopen.cam.NCGroup machineRoot) {
        try {
            nxopen.cam.NCGroupCollection.UseDefaultName camFalse = NCGroupCollection.UseDefaultName.FALSE;
            nxopen.cam.NCGroup toolGroup;
            toolGroup = groups.createTool(machineRoot, "mill_planar", typeTool, camFalse, nameTool);

            nxopen.cam.Tool myTool = (nxopen.cam.Tool) toolGroup;

            nxopen.cam.MillToolBuilder toolBuilder = groups.createMillToolBuilder(myTool);
            toolBuilder.tlHeightBuilder().setValue(lengthTool);
            toolBuilder.tlDiameterBuilder().setValue(diamTool);
            toolBuilder.tlFluteLnBuilder().setValue(cutLengthTool);
            toolBuilder.tlCor1RadBuilder().setValue(cornerRadTool);
//            if (isShank) {
//                toolBuilder.setUseTaperedShank(true);
//                toolBuilder.taperedShankDiameterBuilder().setValue(diamShankTool);
//                toolBuilder.taperedShankLengthBuilder().setValue(shankLengthTool);
//                toolBuilder.taperedShankTaperLengthBuilder().setValue(0.0);
//            }

            double lowerDiam;
            double upperDiam;
            double length;
            if (isHolder) {
                for (int i = 0; i < (itemHolder.size() - 2); i++) {
                    lowerDiam = itemHolder.get(i).getX() * 2;
                    upperDiam = itemHolder.get(i + 1).getX() * 2;
                    length = itemHolder.get(i + 1).getY() - itemHolder.get(i).getY();

                    toolBuilder.holderSectionBuilder().addByUpperDiameter(i, lowerDiam, length, upperDiam, 0.0);
                }
            }
            toolBuilder.tlNumberBuilder().setValue(numberTool);
            toolBuilder.tlAdjRegBuilder().setValue(numberTool);
            toolBuilder.tlCutcomRegBuilder().setValue(numberTool);
            toolBuilder.setDescription("Tool Exported HyperMill");

            toolBuilder.commit();
            toolBuilder.destroy();
        } catch (NXException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка NXException в методе  createEndMill!!!", e);
            e.printStackTrace();
        } catch (RemoteException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка RemoteException в методе  createEndMill!!!", e);
            e.printStackTrace();
        }
    }
}

class HolderPairVar {
    private double x;
    private double y;

    HolderPairVar(double x, double y) {
        this.x = x;
        this.y = y;
    }

    double getX() {
        return this.x;
    }

    double getY() {
        return this.y;
    }
}
