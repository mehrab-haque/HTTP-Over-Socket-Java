package client;

import java.io.IOException;
import java.util.Scanner;

public class FTPClient {
    public static void main(String[] args) {
        while(true){
            System.out.println("Enter file names to upload (separated by space) (or exit):");
            Scanner scanner=new Scanner(System.in);
            String userInput=scanner.nextLine();
            if(userInput.equalsIgnoreCase("exit"))
                break;
            else{
                String[] inputFiles=userInput.split(" ");
                for(String fileName:inputFiles){
                    try {
                        ClientWorker clientWorker=new ClientWorker(fileName);
                        new Thread(clientWorker).start();
                    } catch (RuntimeException e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
            System.out.println();
        }
    }
}
