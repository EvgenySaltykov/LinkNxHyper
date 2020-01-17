package com.intellij.uiDesigner.core;

import nxopen.NXException;
import nxopen.cam.CAMSetup;
import nxopen.cam.InheritableDoubleBuilder;
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

    private double lengthTool = 0.0;
    private final String LENGTH_TOOL = "^11: vtaperHeight\\(";
    private final Pattern patternLengthTool = Pattern.compile(LENGTH_TOOL);

    private void findLengthTool(String stringIn, Matcher matcher) {
        if (matcher.find()) {
            lengthTool = Double.parseDouble(stringIn.substring(matcher.end(), (stringIn.length() - 1)));
        }
    }

    private double cutLengthTool = 0.0;
    private final String CUT_LENGTH_TOOL = "^11: vcuttingLength\\(";
    private final Pattern patternCutLengthTool = Pattern.compile(CUT_LENGTH_TOOL);

    private void findCutLengthTool(String stringIn, Matcher matcher) {
        if (matcher.find()) {
            cutLengthTool = Double.parseDouble(stringIn.substring(matcher.end(), (stringIn.length() - 1)));
        }
    }

    private int numberTool = 0;
    private final String NUMBER_TOOL = ".*cfg\\(\\*WKZNUMMER ";
    private final Pattern patternNumber = Pattern.compile(NUMBER_TOOL);

    private void findNumber(String stringIn, Matcher matcher) {
        if (matcher.find()) {
            numberTool = Integer.parseInt(stringIn.substring(matcher.end(), (stringIn.length() - 1)));
        }
    }


    private String nameTool = "Имя_инструмента_не_найдено";
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
                findLengthTool(stringIn, patternLengthTool.matcher(stringIn));
                findCutLengthTool(stringIn, patternCutLengthTool.matcher(stringIn));
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

            toolBuilder.tlTaperAngBuilder().setValue(3.0);
            
//            toolBuilder.tlNumFlutesBuilder().setValue(5);
            toolBuilder.setDescription("Example ball mill");
            toolBuilder.commit();
            toolBuilder.destroy();
        } catch (NXException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка NXException в методе  createTool()!!!", e);
            e.printStackTrace();
        } catch (RemoteException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка RemoteException в методе  createTool()!!!", e);
            e.printStackTrace();
        }
    }

}
