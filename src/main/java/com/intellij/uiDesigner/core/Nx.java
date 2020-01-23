package com.intellij.uiDesigner.core;

import nxopen.NXException;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;

class Nx {

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
