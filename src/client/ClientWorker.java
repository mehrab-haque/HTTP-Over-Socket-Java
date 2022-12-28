package client;

import Utility.Config;
import Utility.Utils;

import java.io.*;
import java.net.Socket;
import java.nio.file.Paths;

public class ClientWorker implements Runnable {

    Socket socket;
    String fileName;

    public ClientWorker(String fileName){
        this.fileName=fileName;
    }

    @Override
    public void run() {
        try {
            String path= Paths.get(Paths.get("").toAbsolutePath().toAbsolutePath().toString(),fileName).toString();
            File file=new File(path);
            this.fileName=file.getName();
            String msg="";
            boolean error=true;
            if(!file.exists())
                msg="UPLOAD NONEXIST "+fileName;
            else if(!(Utils.isImageFile(file) || fileName.endsWith(".txt")))
                msg="UPLOAD UNSUPPORTED "+fileName;
            else {
                msg="UPLOAD "+fileName;
                error=false;
            }
            socket=new Socket(Config.SERVER_URL,Config.SERVER_PORT);
            BufferedWriter bufferedWriter=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedWriter.write(msg);
            bufferedWriter.newLine();
            bufferedWriter.flush();
            if(!error){
                BufferedReader in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
                String input = in.readLine();
                if(input.equals("READY")){
                    DataOutputStream dataOutputStream = new DataOutputStream(
                            socket.getOutputStream());
                    int bytes = 0;
                    FileInputStream fileInputStream
                            = new FileInputStream(file);
                    dataOutputStream.writeLong(file.length());
                    byte[] buffer = new byte[4 * 1024];
                    while ((bytes = fileInputStream.read(buffer))
                            != -1) {
                        dataOutputStream.write(buffer, 0, bytes);
                        dataOutputStream.flush();
                    }
                    fileInputStream.close();
                    System.out.println(Config.ANSI_GREEN_BACKGROUND+"Upload Completed : "+fileName);
                }
            }else{
                System.out.println(Config.ANSI_RED_BACKGROUND+"Invalid Upload Request : "+fileName);
            }
            socket.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
