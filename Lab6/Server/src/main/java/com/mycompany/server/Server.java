// Server.java
package com.mycompany.server;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;

public class Server extends JFrame {
    private DatagramSocket socket;
    private ArrayList<DatagramPacket> clients;

    private DefaultTableModel tableModel;
    private List<RecIntegral> dataTable; // Коллекция для хранения данных таблицы
  
    private void saveTextToFile(String fileName, List<RecIntegral> dataToSave) {
        try (PrintWriter writer = new PrintWriter(fileName)) {
            for (RecIntegral recIntegral : dataToSave) {
                writer.println(recIntegral.getLowerBound() + " " + recIntegral.getUpperBound() + " " + recIntegral.getIntervalLength() + " " + recIntegral.getResult());
            }
            System.out.println("Data saved to text file.");
        } catch (IOException e) {
            System.err.println("Error saving to file: " + e.getMessage());
        }
    }

    private void loadTextFromFile(String fileName) {
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(" ");
                double lowerBound = Double.parseDouble(values[0]);
                double upperBound = Double.parseDouble(values[1]);
                double intervalLength = Double.parseDouble(values[2]);
                double result = Double.parseDouble(values[3]);
                try {
                    RecIntegral recIntegral = new RecIntegral(lowerBound, upperBound, intervalLength, result);
                    dataTable.add(recIntegral);
                    if (lowerBound < 0.000001 || lowerBound > 1000000 ||
                        upperBound < 0.000001 || upperBound > 1000000 || 
                        intervalLength < 0.000001 || intervalLength > 1000000) {
                            throw new RecIntegralException("Values must be between 0.000001 and 1000000");
                    }
                    if (lowerBound >= upperBound )throw new RecIntegralException("The lowerBound cannot be equal or be greater than the upperBound");
                } catch (RecIntegralException e) {
                    System.out.println("Invalid values: " + e.getMessage());
                }
            }
            System.out.println("Data loaded from text file.");
        } catch (IOException e) {
            System.err.println("Error loading from file: " + e.getMessage());
        }
    }

    private void saveBinaryToFile(String fileName, List<RecIntegral> dataToSave) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName))) {
            oos.writeObject(dataToSave);
            oos.close();
            System.out.println("Data saved to binary file.");
        } catch (IOException e) {
            System.err.println("Error saving to file: " + e.getMessage());
        }
    }

    private void loadBinaryFromFile(String fileName) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName))) {
            List<RecIntegral> loadedData = (List<RecIntegral>) ois.readObject();
            dataTable.addAll(loadedData);
            ois.close();
            System.out.println("Data loaded from binary file.");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading from file: " + e.getMessage());
        }
    }
    
    public Server() {
        setTitle("Integration Calculator - cos(x^2) ");
        
        // Создаем коллекцию для хранения данных таблицы
        dataTable = new ArrayList<>();
        
        // Создаем таблицу
        String[] columns = {"Нижняя граница", "Верхняя граница", "Длина интервала", "Результат"};
        
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0){
            @Override
            public boolean isCellEditable(int row, int column){
                // разрешаем редактирование всех столбцов кроме 4го
                return column != 3;
            }
        };
        
        JTable table = new JTable(tableModel){
            @Override
            public TableCellRenderer getCellRenderer(int row, int column){
                if (column == 3){
                    // запрещаем редактирование 4го столбца
                    return getDefaultRenderer(Object.class);
                }
                else{
                    return super.getCellRenderer(row, column);
                }
            }
            @Override
            public TableCellEditor getCellEditor(int row, int column){
                if (column == 3){
                    return getDefaultEditor(Object.class);
                }
                else{
                    return super.getCellEditor(row, column);
                }
            }
        };
        
        TableColumn column = table.getColumnModel().getColumn(3);
        column.setCellEditor(null);
        
        // Создаем текстовые поля для ввода данных
        JTextField lowerBoundField = new JTextField(10);
        JTextField upperBoundField = new JTextField(10);
        JTextField intervalField = new JTextField(10);

        // Создаем кнопку "Добавить" и задаем действие при нажатии
        JButton addButton = new JButton("Добавить");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    double lowerBound = Double.parseDouble(lowerBoundField.getText());
                    double upperBound = Double.parseDouble(upperBoundField.getText());
                    double intervals = Double.parseDouble(intervalField.getText());
                    if (lowerBound < 0.000001 || lowerBound > 1000000 ||
                        upperBound < 0.000001 || upperBound > 1000000 || 
                        intervals < 0.000001 || intervals > 1000000) {
                            throw new RecIntegralException("Values must be between 0.000001 and 1000000");
                    }
                    if (lowerBound >= upperBound )throw new RecIntegralException("The lowerBound cannot be equal or be greater than the upperBound");
                    
                    RecIntegral recIntegral = new RecIntegral(lowerBound, upperBound, intervals, 9999.9999);
                    dataTable.add(recIntegral); // Добавление записи в список
                    Object[] rowData = { recIntegral.getLowerBound(), recIntegral.getUpperBound(), recIntegral.getIntervalLength()};
                    tableModel.addRow(rowData);
                } 
                catch (RecIntegralException ee) {
                    System.out.println("Invalid values: " + ee.getMessage());
                }
            }
        });
        
        // Создаем кнопку "Удалить" и задаем действие при нажатии
        JButton deleteButton = new JButton("Удалить");
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow != -1) {
                    tableModel.removeRow(selectedRow);
                    dataTable.remove(selectedRow);
                }
            }
        });
        
        // Создаем кнопку "Очистить" и задаем действие при нажатии
        JButton clearButton = new JButton("Очистить");
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tableModel.setRowCount(0);
            }
        });
        
        // Создаем кнопку "Заполнить" и задаем действие при нажатии
        JButton fillButton = new JButton("Заполнить");
        fillButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (RecIntegral recIntegral : dataTable)
                    tableModel.addRow(recIntegral.getDataAsStringArray());
            }
        });
        
        
        JButton calculateButton = new JButton("Вычислить");
        calculateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendArrayList();
                tableModel.setRowCount(0);
            }
        });

        
        
        
        JButton saveText = new JButton("Сохр текст");
        saveText.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                // вызываем диалоговое окно для сохранения файла.
                int result = fileChooser.showSaveDialog(Server.this);
                
                // Передаем полученный путь файла в метод saveTextToFile(), который мы определяли ранее для сохранения данных в текстовый файл
                if (result == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    saveTextToFile(file.getAbsolutePath(), dataTable);
                }
            }
        });

        JButton loadText = new JButton("Загр текст");
        loadText.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int result = fileChooser.showOpenDialog(Server.this); // вызываем диалоговое окно для выбора файла.
                if (result == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    loadTextFromFile(file.getAbsolutePath()); // загрузка данных из текстового файла
                    tableModel.setRowCount(0); // очистить таблицу
                    for (RecIntegral recIntegral : dataTable) {
                        Object[] rowData = {recIntegral.getLowerBound(), recIntegral.getUpperBound(), recIntegral.getIntervalLength(), recIntegral.getResult()};
                        tableModel.addRow(rowData); // добавить строки в таблицу
                    }
                }
            }
        });
        JButton saveBinary = new JButton("Сохр двоич");
        saveBinary.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                // вызываем диалоговое окно для сохранения файла.
                int result = fileChooser.showSaveDialog(Server.this);
                
                // Передаем полученный путь файла в метод saveTextToFile(), который мы определяли ранее для сохранения данных в текстовый файл
                if (result == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    saveBinaryToFile(file.getAbsolutePath(), dataTable);
                }
            }
        });

        JButton loadBinary = new JButton("Загр двоич");
        loadBinary.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int result = fileChooser.showOpenDialog(Server.this); // вызываем диалоговое окно для выбора файла.
                if (result == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    loadBinaryFromFile(file.getAbsolutePath()); // загрузка данных из текстового файла
                    tableModel.setRowCount(0); // очистить таблицу
                    for (RecIntegral recIntegral : dataTable) {
                        Object[] rowData = {recIntegral.getLowerBound(), recIntegral.getUpperBound(), recIntegral.getIntervalLength(), recIntegral.getResult()};
                        tableModel.addRow(rowData); // добавить строки в таблицу
                    }
                }
            }
        });
        
        
        // Создаем панель для компонентов
        JPanel panel = new JPanel();
        panel.add(lowerBoundField);
        panel.add(upperBoundField);
        panel.add(intervalField);
        panel.add(addButton);
        panel.add(deleteButton);
        panel.add(calculateButton);
        panel.add(clearButton);
        panel.add(fillButton);
        
        JPanel panel_file = new JPanel();
        panel_file.add(saveText);
        panel_file.add(loadText);
        panel_file.add(saveBinary);
        panel_file.add(loadBinary);
        
        // Добавляем панель и таблицу на окно
        add(panel, "North");
        add(panel_file, "South");
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane);

        setSize(1000, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
        
        try {
            socket = new DatagramSocket(9876);
            clients = new ArrayList<>();

            new Thread(new Runnable() {
                public void run() {
                    while (true) {
                        byte[] receivedData = new byte[1024];
                        try {
                            DatagramPacket packet = new DatagramPacket(receivedData, receivedData.length);
                            socket.receive(packet);
                            clients.add(packet);
                            String receivedMessage = new String(packet.getData(), 0, packet.getLength());
                            if (!receivedMessage.matches("[a-zA-Z]+")){
                                String[] obj = receivedMessage.split(" \\+ ");
                                for (String smt : obj){
                                    String[] values = smt.split(" ");
                                    double lowerBound = Double.parseDouble(values[0]);
                                    double upperBound = Double.parseDouble(values[1]);
                                    double intervalLength = Double.parseDouble(values[2]);
                                    double result = Double.parseDouble(values[3]);
                                    RecIntegral recIntegral = new RecIntegral(lowerBound, upperBound, intervalLength, result);
                                    dataTable.add(recIntegral);
                                    tableModel.addRow(recIntegral.getDataAsStringArray());
                                }
                            }
            
                        } catch (IOException ex) {
                          Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }).start();
        } catch (SocketException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendArrayList() {
        List<RecIntegral> SendDataTable = new ArrayList<>();
        for (RecIntegral recIntegral : dataTable)
            SendDataTable.add(recIntegral);
                
        dataTable.clear();
        
        int subsetSize = SendDataTable.size() / clients.size();
        int i = 0;
        for (DatagramPacket client : clients) {
            try {
                int start = i * subsetSize; // Вычисляем начало подмножества
                int end = (i == (clients.size()-1)) ? SendDataTable.size() : (i + 1) * subsetSize; // Вычисляем конец подмножества
                List<RecIntegral> subset = new ArrayList<>(SendDataTable.subList(start, end)); // Копируем подмножество в новый ArrayList
                
                String message = "";
                for (RecIntegral recIntegral : subset)
                    message += (recIntegral.getLowerBound() + " " + recIntegral.getUpperBound() + " " + recIntegral.getIntervalLength() + " " + recIntegral.getResult()) + " + ";
                
                System.out.println("send: " + message);
                
                byte[] sendData = message.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, client.getAddress(), client.getPort());
                socket.send(sendPacket);
            } catch (Exception e) {
                e.printStackTrace();
            }
            i++;
        }
    }

    
    public class RecIntegralException extends Exception {
        public RecIntegralException(String message) {
            JOptionPane.showMessageDialog(Server.this, message);
        }
    }
    
    // Класс для хранения записи таблицы
    public static class RecIntegral implements Serializable{
        private static final long serialVersionUID = 1L;
        private double lowerBound;
        private double upperBound;
        private double intervalLength;
        private double result;

        // Конструктор класса RecIntegral
        public RecIntegral(double lowerBound, double upperBound, double intervalLength, double result){
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
            this.intervalLength = intervalLength;
            this.result = result;
        }   
        
        // Возвращает значение нижней границы интегрирования
        public double getLowerBound() {
            return lowerBound;
        }
        // Возвращает значение верхней границы интегрирования
        public double getUpperBound() {
            return upperBound;
        }
        // Возвращает значение длины шага интегрирования
        public double getIntervalLength() {
            return intervalLength;
        }
        // Возвращает результат вычисления
        public double getResult() {
            return result;
        }
        private void setResult(double newResult) {
            this.result = newResult;
        }
        public String[] getDataAsStringArray() {
        String[] data = new String[4];
        data[0] = String.valueOf(lowerBound);
        data[1] = String.valueOf(upperBound);
        data[2] = String.valueOf(intervalLength);
        data[3] = String.valueOf(result);
        return data;
        }
    }
    
    public static void main(String[] args) {
        new Server();
    }
}