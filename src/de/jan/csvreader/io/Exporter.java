package de.jan.csvreader.io;

import java.io.File;
import java.io.FileWriter;

import java.io.IOException;
import java.util.List;

//Exporter exportiert die übergebenen Daten als txt-Datei
public class Exporter {

    private final List<Object[]> data;

    public Exporter(List<Object[]> data) {
        this.data = data;
    }

    //Daten in eine neue Datei übergeben und gespeichert
    public void exportData(String directory, String fileName) {
        File file = new File(directory, fileName);

        //Neuer FileWriter zum Schreiben der Daten in die Textdatei
        try (FileWriter fileWriter = new FileWriter(file)) {
            getData().forEach(rowValues -> {
                boolean add = (boolean) rowValues[2];

                if (add) {
                    String mac = (String) rowValues[1];

                    try {
                        fileWriter.write(mac + "\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Object[]> getData() {
        return data;
    }
}