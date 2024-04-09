package com.mycompany.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class Client {
    
    public Client() throws Exception{
        DatagramSocket clientSocket = new DatagramSocket();

        InetAddress serverAddress = InetAddress.getByName("localhost");
        int serverPort = 9876;

        byte[] sendData;
        byte[] receiveData = new byte[1024];

        String message = "Client";
        sendData = message.getBytes();

        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
        clientSocket.send(sendPacket);

        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        clientSocket.receive(receivePacket);

        String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
        
        List<RecIntegral> clientTable = new ArrayList<>();
        String[] obj = receivedMessage.split(" \\+ ");
        for (String smt : obj){
            String[] values = smt.split(" ");
            double lowerBound = Double.parseDouble(values[0]);
            double upperBound = Double.parseDouble(values[1]);
            double intervalLength = Double.parseDouble(values[2]);
            double result = Double.parseDouble(values[3]);
            RecIntegral recIntegral = new RecIntegral(lowerBound, upperBound, intervalLength, result);
            clientTable.add(recIntegral);
        }
        
        // Вычисление интеграла
        int subsetSize = clientTable.size() / 9; // Делим clientTable на 9 подмножеств
        List<Thread> threads = new ArrayList<>(); // Создаем список для хранения потоков

        for (int i = 0; i < 9; i++) {
            int start = i * subsetSize; // Вычисляем начало подмножества
            int end = (i == 8) ? clientTable.size() : (i + 1) * subsetSize; // Вычисляем конец подмножества
            List<RecIntegral> subset = clientTable.subList(start, end); // Получаем подмножество данных
            Thread thread = new Thread(new IntegrationCalculator(subset));
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
        //---------------------
        
        message = "";
        for (RecIntegral recIntegral : clientTable)
            message += (recIntegral.getLowerBound() + " " + recIntegral.getUpperBound() + " " + recIntegral.getIntervalLength() + " " + recIntegral.getResult()) + " + ";
        
        System.out.println("Send: " + message);
        
        sendData = message.getBytes();
        sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
        clientSocket.send(sendPacket);
        
        clientSocket.close();
    }


    public static class RecIntegral implements Serializable {
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
    
    public static void main(String[] args) throws Exception {
        new Client();
    }
}