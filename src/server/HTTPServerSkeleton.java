package server;

import Utility.Config;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class HTTPServerSkeleton {
    
    public static void main(String[] args) throws IOException {
        
        ServerSocket serverConnect = new ServerSocket(Config.SERVER_PORT);
        System.out.println("Server started.\nListening for connections on port : " + Config.SERVER_PORT + " ...\n");
        while(true)
        {
            Socket s = serverConnect.accept();
            HTTPWorker worker=new HTTPWorker(s);
            new Thread(worker).start();
        }
    }
    
}
