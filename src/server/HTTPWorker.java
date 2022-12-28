package server;

import utility.Config;
import utility.Utils;

import java.io.*;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static utility.Config.CHUNK_SIZE_BYTES;
import static utility.Config.MSG_INVALID_UPLOAD;
import static utility.Utils.getChunk;

public class HTTPWorker implements Runnable {
    Socket socket;
    String notFoundContent;
    String imageViewerContent;
    String textViewerContent;
    String folderViewerContent;


    String folderItem="<li><a href=\"{href}\"><b><i>{name}</i></b><a></li>";
    String fileItem="<li><a href=\"{href}\" target=\"_blank\">{name}</i></li>";

    public HTTPWorker(Socket socket) throws IOException {
        this.socket = socket;
        parseNotFoundContent();
        parseImageViewerdContent();
        parseTextViewerdContent();
        parseFolderViewerdContent();
    }

    private String parseContent(String fileName) throws IOException{
        File file = new File(fileName);
        FileInputStream fis = new FileInputStream(file);
        BufferedReader br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while(( line = br.readLine()) != null ) {
            sb.append( line );
            sb.append( '\n' );
        }
        return sb.toString();
    }

    private void parseNotFoundContent() throws IOException {
        this.notFoundContent=parseContent(Config.NOT_FOUND_HTML);
    }

    private void parseImageViewerdContent() throws IOException {
        this.imageViewerContent=parseContent(Config.IMAGE_VIEWER_HTML);
    }

    private void parseTextViewerdContent() throws IOException {
        this.textViewerContent=parseContent(Config.TEXT_VIEWER_HTML);
    }

    private void parseFolderViewerdContent() throws IOException {
        this.folderViewerContent=parseContent(Config.FOLDER_VIEWER_HTML);
    }

    public static String readFileData(File file, int fileLength) throws IOException {
        FileInputStream fileIn = null;
        byte[] fileData = new byte[fileLength];

        try {
            fileIn = new FileInputStream(file);
            fileIn.read(fileData);
        } finally {
            if (fileIn != null)
                fileIn.close();
        }

        return String.valueOf(fileData);
    }

    private String generateMimeTypeResponse(String mimetype,String htmlContent,int responseCode){
        return "HTTP/1.1 "+responseCode+" OK\r\n"+
                "Server: Java HTTP Server: 1.0\r\n"+
                "Date: " + new Date() + "\r\n"+
                "Content-Type: "+mimetype+"\r\n"+
                "Content-Length: " + htmlContent.length() + "\r\n"+
                "\r\n"+
                htmlContent;
    }

    private String generateHtmlResponse(String htmlContent){
        return generateMimeTypeResponse("text/html",htmlContent,200);
    }

    private String generateHtmlResponseNotFound(String htmlContent){
        return generateMimeTypeResponse("text/html",htmlContent,404);
    }

    private void sendFileOverHTTP(OutputStream out,File file,FileWriter logWriter) throws IOException {
        out.write("HTTP/1.1 200 OK\r\n".getBytes());
        out.write("Accept-Ranges: bytes\r\n".getBytes());
        out.write(("Content-Length: "+file.length()+"\r\n").getBytes());
        out.write("Content-Type: application/octet-stream\r\n".getBytes());
        out.write(("Content-Disposition: attachment; filename=\""+file.getName()+"\"\r\n").getBytes());
        out.write("\r\n".getBytes());
        FileInputStream fis=new FileInputStream(file);
        byte[] bytes=fis.readAllBytes();
        try{
            for(int i=0;i*CHUNK_SIZE_BYTES+CHUNK_SIZE_BYTES<=bytes.length;i++)
                out.write(getChunk(bytes,i*CHUNK_SIZE_BYTES,CHUNK_SIZE_BYTES));
            out.write(getChunk(bytes,((int)(bytes.length/CHUNK_SIZE_BYTES))*CHUNK_SIZE_BYTES,bytes.length%CHUNK_SIZE_BYTES));
        }catch (IOException e){
            System.out.println(Config.MSG_DOWNLOAD_ABORTED);
        }
        out.flush();
        out.close();
        fis.close();
        logWriter.write("HTTP/1.1 200 OK\r\n"+"Accept-Ranges: bytes\r\n"+"Content-Length: "+file.length()+"\r\n"+"Content-Type: application/octet-stream\r\n"+"Content-Disposition: attachment; filename=\""+file.getName()+"\"\r\n");
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            String input = in.readLine();

            if(input == null) return;
            if(input.length() > 0) {
                if(input.startsWith("GET"))
                {
                    String route=input.split(" ")[1].split("[?]")[0].replaceAll("%20"," ");
                    String path=Paths.get(Paths.get("").toAbsolutePath().toString(),route).toString();

                    if(!route.equals("/favicon.ico")){
                        UUID uuid=UUID.randomUUID();
                        FileWriter logWriter=new FileWriter(Paths.get(Paths.get("").toString(),Config.LOG_FOLDER_NAME,uuid.toString()+".txt").toString());
                        logWriter.write(input+"\n\n");


                        System.out.println(input);
                        File requestedFile=new File(path);
                        if(requestedFile.exists()){
                            if(requestedFile.isDirectory()){
                                PrintWriter pr = new PrintWriter(this.socket.getOutputStream());
                                String itemsString="";
                                for (File file:requestedFile.listFiles()){
                                    String childRoute=(route+"/"+file.getName()).replaceAll("//","/");
                                    if(file.isDirectory())
                                        itemsString+=folderItem.replace("{name}",file.getName()).replace("{href}",childRoute)+"\n";
                                    else
                                        itemsString+=fileItem.replace("{name}",file.getName()).replace("{href}",childRoute)+"\n";

                                }
                                String folderViewerResponse=this.folderViewerContent.replace("{title}",requestedFile.getName()).replace("{items}",itemsString);
                                pr.write(generateHtmlResponse(folderViewerResponse));
                                logWriter.write(generateHtmlResponse(folderViewerResponse));
                                pr.flush();
                            }else{
                                if(requestedFile.getName().endsWith(".txt")){
                                    PrintWriter pr = new PrintWriter(this.socket.getOutputStream());
                                    String textViewerResponse=this.textViewerContent.replace("{title}",requestedFile.getName()).replace("{src}",parseContent(path));
                                    pr.write(generateHtmlResponse(textViewerResponse));
                                    logWriter.write(generateHtmlResponse(textViewerResponse));
                                    pr.flush();
                                }
                                else if(Utils.isImageFile(requestedFile)){
                                    FileInputStream fin = new FileInputStream(requestedFile);
                                    byte imagebytearray[] = new byte[(int)requestedFile.length()];
                                    fin.read(imagebytearray);
                                    String base64 = Base64.getEncoder().encodeToString(imagebytearray);
                                    PrintWriter pr = new PrintWriter(this.socket.getOutputStream());
                                    String imageViewerResponse=this.imageViewerContent.replace("{title}",requestedFile.getName()).replace("{src}","data:"+Utils.getMimeType(requestedFile)+";base64, "+base64);
                                    pr.write(generateHtmlResponse(imageViewerResponse));
                                    logWriter.write(generateHtmlResponse(imageViewerResponse));
                                    pr.flush();
                                }else{
                                    OutputStream out=socket.getOutputStream();
                                    sendFileOverHTTP(out,requestedFile,logWriter);
                                    out.flush();
                                    out.close();
                                }
                            }
                        }else {
                            PrintWriter pr = new PrintWriter(this.socket.getOutputStream());
                            pr.write(generateHtmlResponseNotFound(notFoundContent));
                            logWriter.write(generateHtmlResponse(notFoundContent));
                            pr.flush();
                            System.out.println(input+" "+Config.MSG_NOT_FOUND);
                        }
                        logWriter.close();
                    }
                }

                else if(input.startsWith(Config.CLIENT_UPLOAD_COMMAND))
                {
                    if(input.startsWith(Config.CLIENT_UPLOAD_COMMAND+" "+Config.CLIENT_UPLOAD_NONEXIST) || input.startsWith(Config.CLIENT_UPLOAD_COMMAND+" "+Config.CLIENT_UPLOAD_UNSUPPORTED))
                        System.out.println(MSG_INVALID_UPLOAD+" : "+input);
                    else{
                        DataInputStream dataInputStream = new DataInputStream(
                                socket.getInputStream());
                        while (dataInputStream.available()>0)
                            dataInputStream.read();
                        BufferedWriter bufferedWriter=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                        bufferedWriter.write(Config.CLIENT_UPLOAD_READY);
                        bufferedWriter.newLine();
                        bufferedWriter.flush();

                        int bytes = 0;
                        String fileName=input.substring(Config.CLIENT_UPLOAD_COMMAND.length()+1);
                        String path= Paths.get(Paths.get("").toAbsolutePath().toAbsolutePath().toString(),Config.UPLOAD_FOLDER_NAME,fileName).toString();
                        FileOutputStream fileOutputStream
                                = new FileOutputStream(path);

                        long size
                                = dataInputStream.readLong();
                        byte[] buffer = new byte[CHUNK_SIZE_BYTES];
                        while (size > 0
                                && (bytes = dataInputStream.read(
                                buffer, 0,
                                (int)Math.min(buffer.length, size)))
                                != -1) {
                            fileOutputStream.write(buffer, 0, bytes);
                            size -= bytes;
                        }
                        fileOutputStream.close();
                    }

                }
            }
            this.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
