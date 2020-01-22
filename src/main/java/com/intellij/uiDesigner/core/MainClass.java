package com.intellij.uiDesigner.core;

import nxopen.BaseSession;
import nxopen.ListingWindow;
import nxopen.NXException;
import nxopen.Session;

import javax.swing.*;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;

public class MainClass {
    public static Session theSession = null;
    public static ListingWindow lw = null;

    public static void main(String[] args) throws NXException, java.rmi.RemoteException {
//        nxopen.Session theSession = (nxopen.Session) nxopen.SessionFactory.get("Session");
//        lw = theSession.listingWindow();
//        lw.open();
//        lw.writeLine("");
//        lw.writeLine("!!! Hello World !!!");
//        lw.writeLine("");

        //Создать форму для диалога с пользователем
        MainForm.createForm();
    }

    public static int getUnloadOption() {
        return BaseSession.LibraryUnloadOption.EXPLICITLY;
    }

    static String[] getListMembersNx(nxopen.cam.CAMObject[] rootMembers) {
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
                        Collections.addAll(tempListMembers, getListMembersNx(childrenMembers));
                    }
                } else {
                    name = member.name();
                    tempListMembers.add(name);
                }
            }
        } catch (NXException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка NXException в методе  getListMembersNx!!!", e);
            e.printStackTrace();
        } catch (RemoteException e) {
            new PrintLog(Level.WARNING, "!!!Ошибка RemoteException в методе  getListMembersNx!!!", e);
            e.printStackTrace();
        }

        String[] listMembers = new String[tempListMembers.size()];
        tempListMembers.toArray(listMembers);
        return listMembers;
    }
}
