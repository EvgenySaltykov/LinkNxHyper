package com.intellij.uiDesigner.core;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.logging.Level;

public class MainForm extends JFrame {
    static JFrame fr;
    private JPanel panel1;
    private JTextField textField1;
    private JButton exportButton;
    private JButton cancelButton;
    private JButton changeDirectoryButton;
    private JButton infoButton;
    private InfoDialog infoDialog;
    private static final int DEFAULT_WIDTH = 650;
    private static final int DEFAULT_HEIGHT = 200;
    private static File path;


    private MainForm() {
        $$$setupUI$$$();
        createAndAddInfoButton(); // добавление кнопки ИНФО,т.к. через конструктор не подключить было иконку

        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setLocation(locateInCenter(DEFAULT_WIDTH, DEFAULT_HEIGHT)); //разместить форму в центр экрана

        Image icon = new ImageIcon(Objects.requireNonNull(getClass().getClassLoader().getResource("hyper_icon.png"))).getImage();
//        Image icon = new ImageIcon(getCurrentPath("control_panel.png")).getImage();
//        Image icon = new ImageIcon("control_panel.png").getImage();
        setIconImage(icon);
        eventChangeTextField1(); //отслеживает изменения TextField1 и изменяет цвет в зависемости от пути
        eventClickChangeDirectoryButton(); //обработчик события нажатия на кнопку Выбрать директорию
        eventClickExportButton(); //обработчик события нажатия на кнопку Экспортировать
        eventClickCancelButton();  //обработчик события нажатия на кнопку Выход
        eventClickInfoButton(); //обработчик события нажатия на кнопку <i>
    }

    static void createForm() {
        //описание формы и добавления на форму панели
        fr = new MainForm();
        fr.add(new MainForm().$$$getRootComponent$$$());
//        fr.setDefaultCloseOperation(MainForm.EXIT_ON_CLOSE); закоментировано, потому, что иначе закрывается сам NX
        fr.setTitle("Экспорт траекторий");
        fr.setVisible(true);
    }

    private void createAndAddInfoButton() {
        // бобавление кнопки ИНФО,т.к. через конструктор не подключить было иконку
        infoButton = new JButton();
//        infoButton.setIcon(new ImageIcon("infoButton.png"));
//        infoButton.setIcon(new ImageIcon(getCurrentPath("infoButton.png")));
        infoButton.setIcon(new ImageIcon(Objects.requireNonNull(getClass().getClassLoader().getResource("infoButton.png"))));
        infoButton.setMargin(new Insets(0, 0, 0, 0));
        panel1.add(infoButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    private void eventClickInfoButton() {
        //обработчик события нажатия на кнопку <i>
        infoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (infoDialog == null) { //если окно диалога не создано создать
                    infoDialog = new InfoDialog(MainForm.this);
                }
                infoDialog.setVisible(true);
            }
        });
    }

    static Point locateInCenter(int widthForm, int heightForm) {
        //разместить форму в центр экрана
        Toolkit kit = Toolkit.getDefaultToolkit();
        Dimension screenSize = kit.getScreenSize();
        int widthScreen = screenSize.width;
        int heightScreen = screenSize.height;
        int verticalPoint = (heightScreen / 2) - (heightForm / 2);
        int horizontalPoint = (widthScreen / 2) - (widthForm / 2);

        return new Point(horizontalPoint, verticalPoint);
    }

    private void eventChangeTextField1() {
        //отслеживает изменения TextField1 и изменяет цвет в зависемости от пути
        textField1.getDocument().addDocumentListener(new CatchChangeTextField());
    }

    private void eventClickChangeDirectoryButton() {
        //обработчик события нажатия на кнопку Выбрать директорию
        changeDirectoryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                JFileChooser chooser = new JFileChooser();
                chooser.setCurrentDirectory(new File("."));
                chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                chooser.showDialog(panel1, "Выбрать директорию");

                path = chooser.getSelectedFile();
                try {
                    textField1.setBackground(Color.WHITE);

                    if (path.isDirectory()) {
                        textField1.setText(path.getPath());
                    } else {
                        textField1.setText(path.getPath().substring(0, path.getPath().lastIndexOf("\\")));
                    }
//                System.out.println(path.getNameOper());
//                System.out.println(path.isDirectory());
                } catch (NullPointerException e) {
                    textField1.setBackground(Color.RED);
                    textField1.setText("!!!ДИРЕКТОРИЯ НЕ ВЫБРАНА!!!");
                }
            }
        });
    }

    private void eventClickExportButton() {
        //обработчик события нажатия на кнопку Экспортировать
        exportButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
//                System.out.println("Click " + event.paramString());
                try {
                    if (path.exists()) {
//                        System.out.println(path);
                        String[] listPofFiles = getListFiles(".pof");
                        if (listPofFiles.length != 0) {
                            new ParserFile(path, listPofFiles);
                            if (!PrintLog.isException()) {
                                JOptionPane.showMessageDialog(null, "Экспорт завершен успешно!", "", JOptionPane.INFORMATION_MESSAGE);
                            } else {
                                String message = "Экспорт завершен c ошибками!\nОзнакомьтесь с файлом log.txt, или обратитесь к разработчику!";
                                JOptionPane.showMessageDialog(null, message, "", JOptionPane.WARNING_MESSAGE);
                            }
                            PrintLog.closeLogFile(); //закрыть файл log.txt
                            fr.setVisible(false);
                            fr.dispose();   //закрыть программу
                        } else {
                            //если отсутствуют pof-файлы показать диалоговое окно с ошибкой
                            JOptionPane.showMessageDialog(null, "В директории pof-файлы не найдены!", "", JOptionPane.WARNING_MESSAGE);
                        }
                    } else {
                        //добавить диалогове окно не верно указан путь к директории POV-файлов
                        JOptionPane.showMessageDialog(null, "Директория не найдена!", "", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (NullPointerException e) {
                    //Добавить диалоговое необходимо указать путь к директории POV-файлов
//                    System.out.println("ERROR");
                    JOptionPane.showMessageDialog(null, "Укажите путь к директории!", "", JOptionPane.WARNING_MESSAGE);
                }
            }
        });
    }

    private void eventClickCancelButton() {
        //обработчик события нажатия на кнопку Выход
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                PrintLog.closeLogFile(); //закрыть файл log.txt
                fr.setVisible(false);
                fr.dispose();
            }
        });
    }

    private String[] getListFiles(String mask) {
        // получить список Pof-файлов из директории
        String[] listFiles = path.list(new FilePathFilter(mask));

        return listFiles;
    }

    private class FilePathFilter implements FilenameFilter {
        private String mask;

        private FilePathFilter(String mask) {
            this.mask = mask;
        }

        @Override
        public boolean accept(File path, String name) {
            return name.endsWith(mask);
        }
    }

    private class CatchChangeTextField implements DocumentListener {
        @Override
        public void insertUpdate(DocumentEvent e) {
            isPath();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            isPath();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            isPath();
        }

        private void isPath() {
            path = new File(textField1.getText());

            if (path.exists() || textField1.getText().equals("")) {
                textField1.setBackground(Color.WHITE);
            } else {
                textField1.setBackground(Color.RED);
            }
        }
    }

    static File getPathPofDirectory() {
        return path;
    }

    static URL getCurrentPath(String fileName) {
        //возвращает путь к fileName или в скомпилированной директории, или в директории проекта ClsNxParser
        URL urlFile = MainForm.class.getResource(fileName);

        if (urlFile != null) {
            return urlFile;
        } else {
            try {
                urlFile = new File(fileName).toURI().toURL();
                return urlFile;
            } catch (MalformedURLException e) {
                new PrintLog(Level.WARNING, "!!!Файл ".concat(fileName).concat(" не найден!!!"), e);
                return null;
            }
        }
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(4, 2, new Insets(0, 25, 25, 25), -1, -1, true, true));
        panel1.setAutoscrolls(false);
        panel1.setDoubleBuffered(false);
        panel1.setEnabled(false);
        panel1.setRequestFocusEnabled(true);
        panel1.setVerifyInputWhenFocusTarget(true);
        final JLabel label1 = new JLabel();
        label1.setText("Путь к директории \".pof-файлов\"");
        panel1.add(label1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_SOUTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(246, 16), null, 0, false));
        textField1 = new JTextField();
        textField1.setEditable(true);
        textField1.setEnabled(true);
        Font textField1Font = this.$$$getFont$$$("Courier New", Font.BOLD | Font.ITALIC, -1, textField1.getFont());
        if (textField1Font != null) textField1.setFont(textField1Font);
        textField1.setHorizontalAlignment(2);
        panel1.add(textField1, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 24), null, null, 0, false));
        changeDirectoryButton = new JButton();
        changeDirectoryButton.setFocusCycleRoot(false);
        changeDirectoryButton.setFocusTraversalPolicyProvider(false);
        changeDirectoryButton.setFocusable(true);
        changeDirectoryButton.setRequestFocusEnabled(true);
        changeDirectoryButton.setSelected(false);
        changeDirectoryButton.setText("Выбрать директорию");
        panel1.add(changeDirectoryButton, new GridConstraints(0, 1, 2, 1, GridConstraints.ANCHOR_SOUTHEAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        exportButton = new JButton();
        exportButton.setEnabled(true);
        exportButton.setHideActionText(false);
        exportButton.setHorizontalAlignment(0);
        exportButton.setText("Экспортировать");
        exportButton.setVerticalAlignment(0);
        exportButton.setVerticalTextPosition(0);
        panel1.add(exportButton, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cancelButton = new JButton();
        cancelButton.setText("Выход");
        cancelButton.putClientProperty("html.disable", Boolean.FALSE);
        panel1.add(cancelButton, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        return new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
    }

    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}
