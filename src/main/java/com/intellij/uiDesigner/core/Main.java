package com.intellij.uiDesigner.core;

import nxopen.*;
import nxopen.uf.UFPath;
import nxopen.uf.UFUdop;
import nxopen.uf.UFVariant;

import java.rmi.RemoteException;

public class Main {
    public static nxopen.Session session;
    public static nxopen.UFSession ufSession;
    public static nxopen.UI ui;
    public static nxopen.cam.CAMSession camSession;
    public static nxopen.RemoteUtilities rus;
    public static ListingWindow listingWindow;
    public static NXMessageBox nxMessageBox;
    public static Part workPart;

    static {
        try {
            session = (Session) SessionFactory.get("Session");
            ufSession = (UFSession) SessionFactory.get("UFSession");
            ui = (UI) SessionFactory.get("UI");
            camSession = session.camsession();
            rus = (RemoteUtilities) SessionFactory.get("RemoteUtilities");
            listingWindow = session.listingWindow();
            nxMessageBox = ui.nxmessageBox();
            workPart = session.parts().work();
//        } catch (NXException | RemoteException e) {
        } catch (NXException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new MainForm().createForm();
    }

    public static int getUnloadOption() {
        return BaseSession.LibraryUnloadOption.EXPLICITLY;
    }

    public static int udop(String[] args) {
        UFVariant udopPtr;
        UFVariant operPrt;
        UFUdop.Purpose purpose;
        String operName = "";
        UFVariant pathPrt;

        try {
            udopPtr = ufSession.udop().askUdop(new UFVariant(0));
            operPrt = ufSession.udop().askOper(udopPtr);
            purpose = ufSession.udop().askPurpose(udopPtr);
            operName = ufSession.oper().askName(operPrt);
            pathPrt = ufSession.oper().askPath(operPrt);

// ////////////////////////////////////////////////////////////
//               UFVariant mom_tool_path_type = new UFVariant(0);
// ////////////////////////////////////////////////////////////

            if (purpose == UFUdop.Purpose.USER_PARAMS) {
                nxMessageBox.show("User Params", NXMessageBox.DialogType.INFORMATION, "User Params");
            }

            if (purpose == UFUdop.Purpose.GENERATE) {
                ufSession.path().initToolPath(pathPrt);

//                CamPathToolAxisType._FIVE;

                UFPath.LinearMotion linearMotion = new UFPath.LinearMotion();
                linearMotion.feedValue = 456;
                linearMotion.type = UFPath.MotionType.MOTION_TYPE_CUT;
                linearMotion.feedUnit = UFPath.FeedUnit.FEED_UNIT_NONE;
                double[] pos = {0, 0, 0};
                linearMotion.position = pos;
                double[] tAxis = {0, 0, 1};
                linearMotion.toolAxis = tAxis;
                ufSession.path().createLinearMotion(pathPrt, linearMotion);

                linearMotion.position[0] = 0;
                linearMotion.position[1] = 0.707;
                linearMotion.position[2] = 0.707;
                linearMotion.toolAxis[0] = 0;
                linearMotion.toolAxis[1] = 1;
                linearMotion.toolAxis[2] = 0;
                ufSession.path().createLinearMotion(pathPrt, linearMotion);

                linearMotion.position[0] = 1;
                linearMotion.position[1] = 0;
                linearMotion.position[2] = 0;
                linearMotion.toolAxis[0] = 1;
                linearMotion.toolAxis[1] = 0;
                linearMotion.toolAxis[2] = 0;
                ufSession.path().createLinearMotion(pathPrt, linearMotion);

                ufSession.path().endToolPath(pathPrt);
            }
        } catch (NXException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
