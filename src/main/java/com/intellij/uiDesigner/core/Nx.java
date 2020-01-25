package com.intellij.uiDesigner.core;

import nxopen.NXException;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;

class Nx {

    private nxopen.Session theSession;
    private nxopen.Part workPart; // = workPart = theSession.parts().work();
    private nxopen.cam.CAMSetup setup; // = workPart.camsetup();

    Nx() {
        try {
            theSession = (nxopen.Session) nxopen.SessionFactory.get("Session");
            workPart = theSession.parts().work();
            setup = workPart.camsetup();
        } catch (NXException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка NXException в конструкторе класса Nx!!!", e);
        } catch (RemoteException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка RemoteException в конструкторе класса Nx!!!", e);
        }

    }

    nxopen.Session getSession() {
        return this.theSession;
    }

    nxopen.Part getWorkPart() {
        return this.workPart;
    }

    nxopen.cam.CAMSetup getSetup() {
        return this.setup;
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
        } catch (RemoteException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка RemoteException в методе  getListMembers!!!", e);
        }

        String[] listMembers = new String[tempListMembers.size()];
        tempListMembers.toArray(listMembers);
        return listMembers;
    }

    static boolean isEmptyName(String nameMember, nxopen.cam.CAMObject[] rootMembers) {
        String[] listProgram = getListMembers(rootMembers);
        for (String name : listProgram) {
            if (name.equals(nameMember.toUpperCase())) {
                return false;
            }
        }

        return true;
    }
}
