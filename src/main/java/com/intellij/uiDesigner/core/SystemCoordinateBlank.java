package com.intellij.uiDesigner.core;

import nxopen.NXException;
import nxopen.cam.CAMSetup;
import nxopen.cam.NCGroupCollection;

import java.io.File;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class SystemCoordinateBlank {
    private File file;
    private int maxReadLine = 2000; //максимальное колличество прочтенных сторк, чтобы не читать весь файл целиком
    private String mSysName = ""; //имя СКС
    private static final String MSYS_NAME_PATTERN = "^1: cfg\\(\\*NCS_NAME ";
    private static final String MSYS_PATTERN = "^0: ncCs_x|^0: ncCs_y|^0: ncCs_z|^0: ncCs_o";
    private static final String GET_ITEM_MSYS_PATTERN = "(^0: ncCs_x\\( |,| \\))|(^0: ncCs_y\\( |,| \\))|(^0: ncCs_z\\( |,| \\))|(^0: ncCs_o\\( |,| \\))";
    private static final String SYS_PATTERN_FOR_OPERATION = ".*frameCs_x\\(|.*frameCs_y\\(|.*frameCs_z\\(|.*frameCs_o\\(";
    private static final String GET_ITEM_MSYS_PATTERN_FOR_OPERATION = "(^\\d*: frameCs_x\\(|,|\\))|(^\\d*: frameCs_y\\(|,|\\))|(^\\d*: frameCs_z\\(|,|\\))|(^\\d*: frameCs_o\\(|,|\\))";

    private static double[] mSys;

    SystemCoordinateBlank(File file) {
        this.file = file;
        mSys = getItemSYS();
        createSysInNx(mSys);
    }

    private double[] getItemSYS() {
        //заполняет матрицу СКЗ
        ReadFile reader = new ReadFile(this.file);
        String in;
        double[] num = new double[12];
        Matcher matcher;

        int i = 0;
        while (reader.ready() && maxReadLine > 0) {
            in = reader.getLine();

            if ((Pattern.compile(MSYS_PATTERN).matcher(in).find())) {
                String[] items = in.split(GET_ITEM_MSYS_PATTERN);

                for (String s : items) {
                    if (!s.equals("")) {
                        try {
                            num[i] = Double.parseDouble(s);
                            i++;
                        } catch (NumberFormatException e) {
                            new PrintLog(Level.WARNING, "Ошибка в методе getItemSYS", e);
                        }
                    }
                }
            }

            matcher = Pattern.compile(MSYS_NAME_PATTERN).matcher(in);
            if (matcher.find()) {
                mSysName = in.substring(matcher.end(), (in.length() - 1)).replace(" ", "_").replace(":", "_");
            }

            maxReadLine -= 1;
        }

        return num;
    }

    private void createSysInNx(double[] sys) {
        try {
            Nx nx = new Nx();
            nxopen.Part workPart = nx.getWorkPart();
            nxopen.cam.CAMSetup setup = nx.getSetup();

            nxopen.cam.NCGroup geometryRoot = setup.getRoot(CAMSetup.View.GEOMETRY);
            String parentGeometry = geometryRoot.name();
            nxopen.cam.CAMObject[] geometryRootMembers = geometryRoot.getMembers();

            if (Nx.isEmptyName(mSysName, geometryRootMembers)) { // если имя СКС не создано
                nxopen.cam.NCGroup nCGroup1 = (setup.camgroupCollection().findObject(parentGeometry));
                nxopen.cam.NCGroup nCGroup2;
                nCGroup2 = setup.camgroupCollection().createGeometry(nCGroup1, "mill_planar", "MCS", NCGroupCollection.UseDefaultName.FALSE, mSysName);

                nxopen.Point3d origin3 = new nxopen.Point3d(sys[9], sys[10], sys[11]);
                nxopen.Vector3d xDirection1 = new nxopen.Vector3d(sys[0], sys[1], sys[2]);
                nxopen.Vector3d yDirection1 = new nxopen.Vector3d(sys[3], sys[4], sys[5]);
                nxopen.Xform xform1;
                xform1 = workPart.xforms().createXform(origin3, xDirection1, yDirection1, nxopen.SmartObject.UpdateOption.AFTER_MODELING, 1.0);
                nxopen.CartesianCoordinateSystem cartesianCoordinateSystem1;
                cartesianCoordinateSystem1 = workPart.coordinateSystems().createCoordinateSystem(xform1, nxopen.SmartObject.UpdateOption.AFTER_MODELING);

                nxopen.cam.OrientGeometry orientGeometry1 = ((nxopen.cam.OrientGeometry) nCGroup2);
                nxopen.cam.MillOrientGeomBuilder millOrientGeomBuilder1;
                millOrientGeomBuilder1 = setup.camgroupCollection().createMillOrientGeomBuilder(orientGeometry1);

                millOrientGeomBuilder1.setSpecialOutputMode(nxopen.cam.OrientGeomBuilder.SpecialOutputModes.FIXTURE_OFFSET);
                millOrientGeomBuilder1.fixtureOffsetBuilder().setValue(0); // номер СКС (G53)
                millOrientGeomBuilder1.setMcs(cartesianCoordinateSystem1);
                millOrientGeomBuilder1.commit();
                millOrientGeomBuilder1.destroy();
            }
        } catch (NXException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка NXException в методе createSysInNx!!!", e);
        } catch (RemoteException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка RemoteException в методе createSysInNx!!!", e);
        }
    }

    String getMSysName() {
        return this.mSysName;
    }

    static double[] getmSys() {
        return mSys;
    }
}
