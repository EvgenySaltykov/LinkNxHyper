package com.intellij.uiDesigner.core;

import nxopen.NXException;
import nxopen.cam.CAMSetup;
import nxopen.cam.NCGroupCollection;

import javax.swing.text.html.HTMLDocument;
import java.io.File;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ParamTool {
    private File file;
    private String nameTool = "Имя_инструмента_не_найдено";
    private int numberTool = 0;
    private int idTool = 0;
    private boolean isName = false;
    private boolean isNumber = false;
    private boolean isId = false;

    //шаблоны и паттерны
    private final String PATTERN_NAME = ".*cfg\\(\\*WKZKOMMENTAR ";
    private final String NUMBER_TOOL = ".*cfg\\(\\*WKZNUMMER ";
    private final String ID_TOOL = ".*cfg\\(\\*TOOL_CLASS_ID ";
    private final Pattern patternName = Pattern.compile(PATTERN_NAME);
    private final Pattern patternNumber = Pattern.compile(NUMBER_TOOL);
    private final Pattern patternId = Pattern.compile(ID_TOOL);

    ParamTool(File file) {
        this.file = file;
        findParam();
        createTool();
    }

    private void findParam() {
        ReadFile reader = new ReadFile(this.file);
        String stringIn;
        int maxReadLine = 2000; //максимальное колличество прочтенных сторк, чтобы не читать весь файл целиком

        while (reader.ready() && maxReadLine > 0) {
            stringIn = reader.getLine();

            if (isName && isNumber && isId) {
                break;
            } else {
                findName(stringIn, patternName.matcher(stringIn));
                findNumber(stringIn, patternNumber.matcher(stringIn));
                findId(stringIn, patternId.matcher(stringIn));
            }

            maxReadLine -= 1;
        }
    }

    private void findName(String stringIn, Matcher matcher) {
        if (!isName && matcher.find()) {
            nameTool = stringIn.substring(matcher.end(), (stringIn.length() - 1)).replace(" ", "_").replace(":", "_");
            isName = true;
        }
    }

    private void findNumber(String stringIn, Matcher matcher) {
        if (!isNumber && matcher.find()) {
            numberTool = Integer.parseInt(stringIn.substring(matcher.end(), (stringIn.length() - 1)));
            isNumber = true;
        }
    }

    private void findId(String stringIn, Matcher matcher) {
        if (!isId && matcher.find()) {
            idTool = Integer.parseInt(stringIn.substring(matcher.end(), (stringIn.length() - 1)));
            isId = true;
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
            if (!isNewTool(nameTool, listTool)) return; //если имя интсрумента уже создано прервать построение инструмента

            nxopen.cam.NCGroup toolGroup;
            toolGroup = groups.createTool(machineRoot, "mill_planar", "BALL_MILL", camFalse, nameTool);
            nxopen.cam.Tool myTool = (nxopen.cam.Tool) toolGroup;

            nxopen.cam.MillToolBuilder toolBuilder = groups.createMillToolBuilder(myTool);
            toolBuilder.tlDiameterBuilder().setValue(4.5);
            toolBuilder.tlHeightBuilder().setValue(61);
            toolBuilder.tlFluteLnBuilder().setValue(10.1);
            toolBuilder.tlNumFlutesBuilder().setValue(5);
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

    private String[] getToolList(nxopen.cam.NCGroupCollection groups) {
        ArrayList<String> tempListTool = new ArrayList<String>();
        String[] toolList;

        try {
            nxopen.cam.NCGroup group = null;

            for (Iterator i = groups.iterator(); i.hasNext();) {
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

}
