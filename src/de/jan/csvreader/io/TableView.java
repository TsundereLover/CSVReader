package de.jan.csvreader.io;

import javafx.application.Platform;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicInteger;

//TableView stellt das gesamte GUI da + backend bis auf Exporter
//Neue Instanz dieser Klasse bedeutet neues GUI
public class TableView extends JFrame {

    private JFileChooser fileChooser;
    private JTable table;
    private DefaultTableModel defaultTableModel;
    private File selectedFile;
    private boolean opened;

    public TableView() {

        //Dieses JFrame importFrame stellt den Anfang da!
        JFrame importFrame = new JFrame();
        importFrame.setTitle("CSVReader");

        //Der JButton importButton wird auf dem Anfangsframe, dem importFrame, dargestellt + Eigenschaften werden festgelegt
        JButton importButton = new JButton("CSV-Tabelle importieren");
        importButton.setSize(50, 75);

        importFrame.add(importButton);
        importFrame.setSize(250, 250);
        importFrame.setVisible(true);
        importFrame.setLocationRelativeTo(null);
        importFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        importFrame.setResizable(false);

        //Wenn der importButton gedrückt wird:
        importButton.addActionListener(e -> {

            //Neues JFrame frame, welches den "Rahmen" für den file browser darstellt
            JFrame frame = new JFrame("File Browser");

            //Check, ob der Knopf bereits gedrückt wurde
            if (!isOpened()) {

                //Knopf gedrückt
                setOpened(true);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setResizable(false);

                //Der JFileChooser stellt den file browser im frame da
                fileChooser = new JFileChooser("\\.");
                fileChooser.setControlButtonsAreShown(false);

                //file browser wird zu frame hinzugefügt
                frame.getContentPane().add(fileChooser, BorderLayout.CENTER);

                //ActionListener für die ausgewählte Datei
                ActionListener fileChooserListener = actionEvent -> {
                    JFileChooser theFileChooser = (JFileChooser) actionEvent.getSource();
                    String command = actionEvent.getActionCommand();

                    //Check, ob die Auswahl eine richtige ist
                    if (command.equals(JFileChooser.APPROVE_SELECTION)) {
                        this.selectedFile = theFileChooser.getSelectedFile();
                        String[] filenameComponents = selectedFile.getName().split("\\.");
                        String ending = filenameComponents[filenameComponents.length - 1];

                        //Check, ob es sich um eine CSV-Datei handelt
                        if (ending.equals("csv")) {
                            frame.dispose();
                            importFrame.dispose();
                            java.util.List<String> input = new ArrayList<>();
                            String line;

                            //Datei wird geöffnet
                            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(getSelectedFile()))) {
                                int index = 0;
                                AtomicInteger pcNamePos = new AtomicInteger(0);
                                AtomicInteger macPos = new AtomicInteger(0);

                                //Jede line wird eingelesen und zur List input hinzugefügt
                                while ((line = bufferedReader.readLine()) != null) {
                                    if (index == 0) {
                                        java.util.List<String> columnNames = Arrays.asList(line.split(","));

                                        pcNamePos.set(columnNames.indexOf("AssetName"));
                                        macPos.set(columnNames.indexOf("Mac"));
                                    } else {
                                        input.add(line);
                                    }
                                    ++index;
                                }

                                //Neues Tabellenmodell defaultTableModel mit Spaltennamen wird erstellt
                                defaultTableModel = new DefaultTableModel(new Object[]{"PC-Name", "MAC", "Hinzufügen?"}, 0);

                                //Für jeden input in der List input wird eine neue row in der Tabellenmodell erstellt
                                input.forEach(s -> {
                                    String[] splitted = s.split(",");
                                    String pcName = splitted[pcNamePos.get()];
                                    String mac = splitted[macPos.get()];

                                    getDefaultTableModel().addRow(new Object[]{pcName, mac, false});
                                });
                            } catch (IOException exception) {
                                exception.printStackTrace();
                            }

                            //Neue Tabelle table mit dem Tabellenmodell defaultTableModel wird erstellt
                            table = new JTable(getDefaultTableModel()) {

                                //Hier werden die Typen der Spalten gesetzt
                                @Override
                                public Class<?> getColumnClass(int columnIndex) {
                                    switch (columnIndex) {
                                        case 0:
                                        case 1:
                                            return String.class;

                                        case 2:
                                            return Boolean.class;

                                        default:
                                            return null;
                                    }
                                }

                                //Hier wird die Veränderbarkeit der Spalten festgelegt
                                @Override
                                public boolean isCellEditable(int row, int column) {
                                    switch (column) {
                                        case 0:
                                        case 1:
                                            return false;

                                        default:
                                            return true;
                                    }
                                }
                            };
                            table.setSize(new Dimension(750, 400));

                            //Fenster mit Tabelle usw. wird geöffnet und angepasst
                            JScrollPane jScrollPane = new JScrollPane(table);
                            jScrollPane.setPreferredSize(new Dimension(750, 400));

                            //JButton applyButton sammelt alle Werte aus der Liste und übergibt sie dem Exporter
                            JButton applyButton = new JButton();
                            applyButton.setText("Apply");

                            applyButton.addActionListener(applyButtonListener -> {
                                java.util.List<Object[]> rowValues = new ArrayList<>();

                                for (int i = 0; i < table.getRowCount(); ++i) {
                                    rowValues.add(new Object[]{table.getValueAt(i, 0), table.getValueAt(i, 1), table.getValueAt(i, 2)});
                                }
                                Exporter exporter = new Exporter(rowValues);
                                exporter.exportData(System.getProperty("user.dir"), "export.txt");

                                JOptionPane.showMessageDialog(this, "Daten erfolgreich exportiert!");
                                new Timer().schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        dispatchEvent(new WindowEvent(FocusManager.getCurrentManager().getActiveWindow(), WindowEvent.WINDOW_CLOSING));
                                    }
                                }, 1000);
                            });
                            setSize(750, 450);
                            setResizable(false);
                            setTitle("CSV-Tabelle");
                            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                            setLocationRelativeTo(null);
                            setVisible(true);
                            getContentPane().setLayout(new BorderLayout());
                            getContentPane().add(jScrollPane, BorderLayout.CENTER);
                            getContentPane().add(applyButton, BorderLayout.SOUTH);

                            //Wenn es sich nicht um eine CSV-Datei handelt:
                        } else {
                            //Falsche Datei: Frage, ob neue wählen oder nicht
                            int dialogButton = JOptionPane.YES_OPTION;
                            int dialogResult = JOptionPane.showConfirmDialog(this, "Keine CSV-Datei! Neue wählen?", "Error", dialogButton);

                            //Ja: neue Instanz dieser Klasse wird erstellt und man landet beim importFrame
                            if (dialogResult == JOptionPane.YES_OPTION) {
                                JOptionPane.getRootFrame().dispose();
                                importFrame.dispose();
                                frame.dispose();
                                new TableView();

                                //Nicht: Programm wird geschlossen
                            } else if (dialogResult == JOptionPane.NO_OPTION) {
                                frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
                                Platform.exit();
                            }
                        }
                    }
                };

                fileChooser.addActionListener(fileChooserListener);
                frame.pack();
                frame.setVisible(true);
                frame.setLocationRelativeTo(null);
                frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            }
        });
    }

    public DefaultTableModel getDefaultTableModel() {
        return defaultTableModel;
    }

    public JTable getTable() {
        return table;
    }

    public File getSelectedFile() {
        return selectedFile;
    }

    public boolean isOpened() {
        return opened;
    }

    public void setOpened(boolean opened) {
        this.opened = opened;
    }

    public JFileChooser getFileChooser() {
        return fileChooser;
    }
}