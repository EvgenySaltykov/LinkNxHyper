package com.intellij.uiDesigner.core;

import nxopen.*;
import nxopen.uf.UFPath;
import nxopen.uf.UFUdop;
import nxopen.uf.UFVariant;

import java.rmi.RemoteException;
import java.util.logging.Level;

public class Main {
    private static nxopen.Session session;
    private static nxopen.UFSession ufSession;
    private static nxopen.UI ui;
    private static nxopen.cam.CAMSession camSession;
    private static nxopen.RemoteUtilities rus;
    private static ListingWindow listingWindow;
    private static NXMessageBox nxMessageBox;
    private static Part workPart;

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
        } catch (NXException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка NXException в static bloc класса Main!!!", e);
        } catch (RemoteException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка RemoteException в static bloc класса Main!!!", e);
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
//               ufSession.mom().setString(pathPrt, "tool_path_type", "variable_axis");
// ////////////////////////////////////////////////////////////

            if (purpose == UFUdop.Purpose.USER_PARAMS) {
                nxMessageBox.show("User Params", NXMessageBox.DialogType.INFORMATION, "ну вот зачем ты нажал на эту кнопку?");
            }

            if (purpose == UFUdop.Purpose.GENERATE) {
                ufSession.path().initToolPath(pathPrt);

//                CamPathToolAxisType.FIVE;

                UFPath.LinearMotion linearMotion = new UFPath.LinearMotion();

                new ToolPath().writeMove(operName, ufSession, pathPrt, linearMotion);

                ufSession.path().endToolPath(pathPrt);
            }
        } catch (NXException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка NXException в методе udop!!!", e);
        } catch (RemoteException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка RemoteException в методе udop!!!", e);
        }
        return 0;
    }
}
