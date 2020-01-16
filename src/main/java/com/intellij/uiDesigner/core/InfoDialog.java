package com.intellij.uiDesigner.core;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.Objects;
import java.util.logging.Level;

/**
 * Образец модального диалогового окна, в котором
 * выводится сообщение и ожидается до тех пор, пока
 * пользователь не щелкнет по кнопке ЗАКРЫТЬ
 */
class InfoDialog extends JDialog {
    private TextArea textArea = new TextArea();
    private static final int DEFAULT_WIDTH = 700;
    private static final int DEFAULT_HEIGHT = 450;

    InfoDialog(JFrame owner) {
        super(owner, "Информация о приложении", true);

        //Добавить текстовое поле на форму
        add(textArea);

        //При выборе кнопки OK диалоговое окно закрывается
        Button ok = new Button("Закрыть");
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                InfoDialog.this.setVisible(false);
            }
        }); //если нажать ok скрыть форму, но объект остается

        //ввести кнопку ОК в нижний части окна у южной границы
        JPanel panel = new JPanel();
        panel.add(ok);
        add(panel, BorderLayout.SOUTH);

        pack();

        textArea.setEditable(false);
        this.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        contentDialog("info.txt");
        int width = this.getWidth();
        int height = this.getHeight();
        Point pointLocate = MainForm.locateInCenter(width, height);
        this.setLocation(pointLocate);
    }

    private void contentDialog(String fileName) {
//        File file = new File(MainForm.getCurrentPath(fileName).getFile());
        InputStream resourceFileStream = Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(fileName));

        new PrintLog(Level.WARNING, "!!!Файл " + resourceFileStream);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(resourceFileStream, "UTF-8"));
            while(reader.ready()) {
                textArea.append(reader.readLine() + '\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
