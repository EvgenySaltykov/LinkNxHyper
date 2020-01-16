package com.intellij.uiDesigner.core;

import nxopen.BaseSession;
import nxopen.ListingWindow;
import nxopen.NXException;
import nxopen.Session;

import javax.swing.*;

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
}
