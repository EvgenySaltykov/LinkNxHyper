package com.intellij.uiDesigner.core;

import nxopen.NXException;
import nxopen.cam.CAMSetup;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;

class Nx {
    static nxopen.Session theSession;
    static nxopen.Part workPart;
    static nxopen.cam.CAMSetup setup;
    static nxopen.cam.NCGroup programRoot;
    static nxopen.cam.CAMObject[] programRootMembers;
    static nxopen.cam.NCGroup geometryRoot;
    static nxopen.cam.CAMObject[] geometryRootMembers;
    static nxopen.cam.NCGroup methodRoot;
    static nxopen.cam.CAMObject[] methodRootMembers;
    static nxopen.cam.NCGroup toolRoot;
    static nxopen.cam.CAMObject[] toolRootMembers;

    static {
        try {
            theSession = (nxopen.Session) nxopen.SessionFactory.get("Session");
            workPart = theSession.parts().work();
            setup = workPart.camsetup();

            programRoot = setup.getRoot(CAMSetup.View.PROGRAM_ORDER);
            programRootMembers = programRoot.getMembers();

            geometryRoot = setup.getRoot((CAMSetup.View.GEOMETRY));
            geometryRootMembers = geometryRoot.getMembers();

            methodRoot = setup.getRoot(CAMSetup.View.MACHINE_METHOD);
            methodRootMembers = methodRoot.getMembers();

            toolRoot = setup.getRoot(CAMSetup.View.MACHINE_TOOL);
            toolRootMembers = toolRoot.getMembers();

        } catch (NXException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка NXException в создания сессии в классе NX!!!", e);
            e.printStackTrace();
        } catch (RemoteException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка NXException в создания сессии в классе NX!!!", e);
            e.printStackTrace();
        }
    }

    static String[] getListMembers(nxopen.cam.CAMObject[] rootMembers) {
        ArrayList<String> tempListMembers = new ArrayList<String>();
        String name;

        try {
            for (nxopen.cam.CAMObject member : rootMembers) {
                if (member instanceof nxopen.cam.NCGroup) {
                    nxopen.cam.NCGroup group = (nxopen.cam.NCGroup) member;
                    nxopen.cam.CAMObject[] childrenMembers = group.getMembers();
                    name = member.name();
                    tempListMembers.add(name);
                    if (childrenMembers.length != 0) {
                        Collections.addAll(tempListMembers, getListMembers(childrenMembers));
                    }
                } else {
                    name = member.name();
                    tempListMembers.add(name);
                }
            }
        } catch (NXException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка NXException в методе  getListMembers!!!", e);
            e.printStackTrace();
        } catch (RemoteException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка RemoteException в методе  getListMembers!!!", e);
            e.printStackTrace();
        }

        String[] listMembers = new String[tempListMembers.size()];
        tempListMembers.toArray(listMembers);
        return listMembers;
    }
}
