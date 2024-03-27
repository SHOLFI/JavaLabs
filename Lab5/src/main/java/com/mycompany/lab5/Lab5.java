package com.mycompany.lab5;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import java.io.*;

public class Lab5 extends JFrame{
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

    
    public Lab5() {
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
                int subsetSize = dataTable.size() / 9; // Делим dataTable на 9 подмножеств
                List<Thread> threads = new ArrayList<>(); // Создаем список для хранения потоков

                for (int i = 0; i < 9; i++) {
                    int start = i * subsetSize; // Вычисляем начало подмножества
                    int end = (i == 8) ? dataTable.size() : (i + 1) * subsetSize; // Вычисляем конец подмножества
                    List<RecIntegral> subset = dataTable.subList(start, end); // Получаем подмножество данных
                    Thread thread = new Thread(new IntegrationCalculator(subset)); // Создаем поток для вычисления интегралов
                    threads.add(thread); // Добавляем поток в список
                    thread.start(); // Запускаем поток
                }
                
                // Ждем завершения всех потоков
                for (Thread thread : threads) {
                    try {
                        thread.join(); // Ждем завершения каждого потока
                        
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }

                tableModel.setRowCount(0);
                for (RecIntegral recIntegral : dataTable)
                    tableModel.addRow(recIntegral.getDataAsStringArray());
            }
        });

        
        
        
        JButton saveText = new JButton("Сохр текст");
        saveText.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                // вызываем диалоговое окно для сохранения файла.
                int result = fileChooser.showSaveDialog(Lab5.this);
                
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
                int result = fileChooser.showOpenDialog(Lab5.this); // вызываем диалоговое окно для выбора файла.
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
                int result = fileChooser.showSaveDialog(Lab5.this);
                
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
                int result = fileChooser.showOpenDialog(Lab5.this); // вызываем диалоговое окно для выбора файла.
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
    }
    
    public class RecIntegralException extends Exception {
        public RecIntegralException(String message) {
            JOptionPane.showMessageDialog(Lab5.this, message);
        }
    }
    
    // Создаем класс, реализующий интерфейс Runnable
    class IntegrationCalculator implements Runnable {
        private List<RecIntegral> dataSubset; // Список данных для вычисления интегралов

        public IntegrationCalculator(List<RecIntegral> dataSubset) {
            this.dataSubset = dataSubset;
        }

        @Override
        public void run() {
            // Выполняем вычисление интеграла для подмножества
            for (RecIntegral recIntegral : dataSubset) {
                 // Выполняем вычисление интеграла для каждого recIntegral
                double result = calculateIntegration(recIntegral.getLowerBound(), recIntegral.getUpperBound(), recIntegral.getIntervalLength());
                recIntegral.setResult(result);
            }
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
        public RecIntegral(double lowerBound, double upperBound, double intervalLength, double result) throws RecIntegralException {
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
    // Метод для вычисления интеграла функции cos(x^2) методом трапеций
    private double calculateIntegration(double lowerBound, double upperBound, double step) {
        double sum = 0.0;
        double x = lowerBound;
        while (x < upperBound) {
            double fx1 = Math.cos(x * x); // значение функции в левой точке отрезка
            double fx2 = Math.cos(Math.min(x + step, upperBound) * Math.min(x + step, upperBound)); // значение функции в правой точке отрезка
            sum += (fx1 + fx2) * Math.min(step, upperBound - x) / 2;
            x += step;
        }
        //округлим ответ
        int decimalPlaces = 4; // количество знаков после запятой, до которого нужно округлить
        sum = Math.round(sum * Math.pow(10, decimalPlaces)) / Math.pow(10, decimalPlaces);
        
        return sum; // возвращаем значение интеграла
    }

    public static void main(String[] args) {
        new Lab5(); // создаем экземпляр приложения
    }
}