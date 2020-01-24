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
    private static final String MSYS_PATTERN = ".*frameCs_x\\(|.*frameCs_y\\(|.*frameCs_z\\(|.*frameCs_o\\(|.*frameCs_id\\(";
    private static final String GET_ITEM_MSYS_PATTERN = "(^\\d*: frameCs_x\\(|,|\\))|(^\\d*: frameCs_y\\(|,|\\))|(^\\d*: frameCs_z\\(|,|\\))|(^\\d*: frameCs_o\\(|,|\\))|(^\\d*: frameCs_id\\(|,|\\))";

    SystemCoordinateBlank(File file) {
        this.file = file;
        double[] sys = getItemSYS();
        createSysInNx(sys);
    }

    private double[] getItemSYS() {
        //заполняет матрицу СКЗ
        ReadFile reader = new ReadFile(this.file);
        String in;
        double[] num = new double[13];
        Matcher matcher;

        int i = 0;
        while (reader.ready() && maxReadLine > 0) {
            in = reader.getLine();

            if ((Pattern.compile(MSYS_PATTERN).matcher(in).find())) {
                String[] items = in.split(GET_ITEM_MSYS_PATTERN);

                for (String s : items) {
                    if (!s.equals("")) {
                        num[i] = Double.parseDouble(s);
                        i++;
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
        boolean isEmptyMSYS = true;

        try {
            Nx nx = new Nx();
            nxopen.Part workPart = nx.getWorkPart();
            nxopen.cam.CAMSetup setup = nx.getSetup();

            nxopen.cam.NCGroup geometryRoot = setup.getRoot(CAMSetup.View.GEOMETRY);
            String parentGeometry = geometryRoot.name();
            nxopen.cam.CAMObject[] geometryRootMembers = geometryRoot.getMembers();

            String[] listGeometryMembers = Nx.getListMembers(geometryRootMembers);
            for (String name : listGeometryMembers) {
                if (name.equals(mSysName.toUpperCase())) {
                    isEmptyMSYS = false;
                }
            }

            if (isEmptyMSYS) { // если имя СКС не создано
                nxopen.cam.NCGroup nCGroup1 = (setup.camgroupCollection().findObject(parentGeometry));
                nxopen.cam.NCGroup nCGroup2;
                nCGroup2 = setup.camgroupCollection().createGeometry(nCGroup1, "mill_planar", "MCS", NCGroupCollection.UseDefaultName.FALSE, mSysName);

                nxopen.Point3d origin3 = new nxopen.Point3d(sys[10], sys[11], sys[12]);
                nxopen.Vector3d xDirection1 = new nxopen.Vector3d(sys[1], sys[2], sys[3]);
                nxopen.Vector3d yDirection1 = new nxopen.Vector3d(sys[4], sys[5], sys[6]);
                nxopen.Xform xform1;
                xform1 = workPart.xforms().createXform(origin3, xDirection1, yDirection1, nxopen.SmartObject.UpdateOption.AFTER_MODELING, 1.0);
                nxopen.CartesianCoordinateSystem cartesianCoordinateSystem1;
                cartesianCoordinateSystem1 = workPart.coordinateSystems().createCoordinateSystem(xform1, nxopen.SmartObject.UpdateOption.AFTER_MODELING);

                nxopen.cam.OrientGeometry orientGeometry1 = ((nxopen.cam.OrientGeometry) nCGroup2);
                nxopen.cam.MillOrientGeomBuilder millOrientGeomBuilder1;
                millOrientGeomBuilder1 = setup.camgroupCollection().createMillOrientGeomBuilder(orientGeometry1);

                millOrientGeomBuilder1.setSpecialOutputMode(nxopen.cam.OrientGeomBuilder.SpecialOutputModes.FIXTURE_OFFSET);
                millOrientGeomBuilder1.fixtureOffsetBuilder().setValue((int) sys[0]);
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
}
