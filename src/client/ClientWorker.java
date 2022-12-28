package client;

import utility.Config;
import utility.Utils;

import java.io.*;
import java.net.Socket;
import java.nio.file.Paths;

import static utility.Config.CHUNK_SIZE_BYTES;
import static utility.Utils.getChunk;

public class ClientWorker implements Runnable {

    Socket socket;
    String fileName;

    public ClientWorker(String fileName){
        this.fileName=fileName.replaceAll("%20"," ");
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
                    FileInputStream fileInputStream
                            = new FileInputStream(file);
                    dataOutputStream.writeLong(file.length());
                    byte[] bytes=fileInputStream.readAllBytes();
                    for(int i=0;i*CHUNK_SIZE_BYTES+CHUNK_SIZE_BYTES<=bytes.length;i++) {
                        dataOutputStream.write(getChunk(bytes, i * CHUNK_SIZE_BYTES, CHUNK_SIZE_BYTES));
                        System.out.print(fileName+" : " + Config.UPLOADING_PROGRESS_MSG+" "+Integer.parseInt((i*100/(bytes.length/CHUNK_SIZE_BYTES))+"") + "%\r");
                        System.out.flush();
                    }
                    dataOutputStream.write(getChunk(bytes,((int)(bytes.length/CHUNK_SIZE_BYTES))*CHUNK_SIZE_BYTES,bytes.length%CHUNK_SIZE_BYTES));
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
