package com.intellij.uiDesigner.core;

import nxopen.ListingWindow;
import nxopen.NXException;
import nxopen.cam.CAMSetup;
import nxopen.cam.NCGroupCollection;

import javax.swing.*;
import java.io.File;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.regex.Pattern;

class SystemCoordinateBlank {
    private File file;
    private static double[] sysOld = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
    private static final String PERFIX_WKS = "HYPERMILL_WKS_";
    private static int countSys = 1;
    private int maxReadLine = 2000; //максимальное колличество прочтенных сторк, чтобы не читать весь файл целиком
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

            maxReadLine -= 1;
        }

        return num;
    }

    private boolean isEqualsSys(double[] sysOld, double[] sysNew) {
        double dif;

        for (int i = 0; i < sysOld.length; i++) {
            dif = sysOld[i] - sysNew[i];
            if (Math.abs(dif) > 0.000000001) {
                SystemCoordinateBlank.sysOld = sysNew;
                return false;
            }
        }

        return true;
    }

    private void createSysInNx(double[] sys) {
        if (!isEqualsSys(sysOld, sys)) { //если матрица СКС изменилась
            try {
                nxopen.Session theSession = (nxopen.Session) nxopen.SessionFactory.get("Session");
                nxopen.Part workPart = theSession.parts().work();
                nxopen.cam.CAMSetup setup = workPart.camsetup();

                nxopen.cam.NCGroup geometryRoot = setup.getRoot(CAMSetup.View.GEOMETRY);
                String parentGeometry = geometryRoot.name();
                nxopen.cam.CAMObject[] geometryRootMembers = geometryRoot.getMembers();
                for (nxopen.cam.CAMObject member : geometryRootMembers) {
                    if (member.name().startsWith(PERFIX_WKS)) {
                        countSys = getCountSys(member.name());
                    }
                }

                nxopen.cam.NCGroup nCGroup1 = ((nxopen.cam.NCGroup) workPart.camsetup().camgroupCollection().findObject(parentGeometry));
                nxopen.cam.NCGroup nCGroup2;
                String nameWKS = PERFIX_WKS.concat(Integer.toString(countSys));
                nCGroup2 = workPart.camsetup().camgroupCollection().createGeometry(nCGroup1, "mill_planar", "MCS", NCGroupCollection.UseDefaultName.FALSE, nameWKS);
                countSys++;

                nxopen.Point3d origin3 = new nxopen.Point3d(sys[10], sys[11], sys[12]);
                nxopen.Vector3d xDirection1 = new nxopen.Vector3d(sys[1], sys[2], sys[3]);
                nxopen.Vector3d yDirection1 = new nxopen.Vector3d(sys[4], sys[5], sys[6]);
                nxopen.Xform xform1;
                xform1 = workPart.xforms().createXform(origin3, xDirection1, yDirection1, nxopen.SmartObject.UpdateOption.AFTER_MODELING, 1.0);
                nxopen.CartesianCoordinateSystem cartesianCoordinateSystem1;
                cartesianCoordinateSystem1 = workPart.coordinateSystems().createCoordinateSystem(xform1, nxopen.SmartObject.UpdateOption.AFTER_MODELING);

                nxopen.cam.OrientGeometry orientGeometry1 = ((nxopen.cam.OrientGeometry) nCGroup2);
                nxopen.cam.MillOrientGeomBuilder millOrientGeomBuilder1;
                millOrientGeomBuilder1 = workPart.camsetup().camgroupCollection().createMillOrientGeomBuilder(orientGeometry1);

                millOrientGeomBuilder1.setSpecialOutputMode(nxopen.cam.OrientGeomBuilder.SpecialOutputModes.FIXTURE_OFFSET);
                millOrientGeomBuilder1.fixtureOffsetBuilder().setValue((int) sys[0]);
                millOrientGeomBuilder1.setMcs(cartesianCoordinateSystem1);
                millOrientGeomBuilder1.commit();
                millOrientGeomBuilder1.destroy();


            } catch (NXException e) {
                new PrintLog(Level.WARNING, "!!!Ошибка создания сессии nxopen.Session!!!", e);
            } catch (RemoteException e) {
                new PrintLog(Level.WARNING, "!!!Ошибка создания workPart!!!", e);
            }
        }
    }

    private int getCountSys(String nameWKS) {
        int ind = nameWKS.lastIndexOf("_");
        String num = nameWKS.substring(ind + 1);
        int count = Integer.parseInt(num);

        if (count > countSys) {
            return ++count;
        }

        return countSys;
    }
}
