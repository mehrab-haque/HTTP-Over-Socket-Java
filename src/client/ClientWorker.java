package client;

import utility.Config;
import utility.Utils;

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
                msg=Config.CLIENT_UPLOAD_COMMAND+" "+Config.CLIENT_UPLOAD_NONEXIST+" "+fileName;
            else if(!(Utils.isImageFile(file) || fileName.endsWith(".txt")))
                msg=Config.CLIENT_UPLOAD_COMMAND+" "+Config.CLIENT_UPLOAD_UNSUPPORTED+" "+fileName;
            else {
                msg=Config.CLIENT_UPLOAD_COMMAND+" "+fileName;
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
                if(input.equals(Config.CLIENT_UPLOAD_READY)){
                    DataOutputStream dataOutputStream = new DataOutputStream(
                            socket.getOutputStream());
                    int bytes = 0;
                    FileInputStream fileInputStream
                            = new FileInputStream(file);
                    dataOutputStream.writeLong(file.length());
                    byte[] buffer = new byte[Config.CHUNK_SIZE_BYTES];
                    while ((bytes = fileInputStream.read(buffer))
                            != -1) {
                        dataOutputStream.write(buffer, 0, bytes);
                        dataOutputStream.flush();
                    }
                    fileInputStream.close();
                    System.out.println(Config.ANSI_GREEN_BACKGROUND+Config.CLIENT_SUCCESS_MSG+" : "+fileName);
                }
            }else{
                System.out.println(Config.ANSI_RED_BACKGROUND+Config.CLIENT_ERROR_MSG+" : "+fileName);
            }
            socket.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
