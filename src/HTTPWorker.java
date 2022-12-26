import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Date;

public class HTTPWorker extends Thread {
    Socket socket;
    String notFoundContent;
    String imageViewerContent;
    String textViewerContent;
    String folderViewerContent;

    String folderItem="<li><a href=\"{href}\"><b><i>{name}</i></b><a></li>";
    String fileItem="<li><a href=\"{href}\">{name}</i></li>";

    public HTTPWorker(Socket socket) throws IOException {
        this.socket = socket;
        parseNotFoundContent();
        parseImageViewerdContent();
        parseTextViewerdContent();
        parseFolderViewerdContent();
    }

    private String getMimeType(File file) throws IOException {
        return Files.probeContentType(file.toPath());
    }

    private boolean isImageFile(File src) throws IOException {
        String mimetype = getMimeType(src);
        return mimetype != null && mimetype.split("/")[0].equals("image");
    }

    private boolean isTextFile(File src) throws IOException {
        String mimetype = getMimeType(src);
        return mimetype != null && mimetype.split("/")[0].equals("text");
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
        this.notFoundContent=parseContent("404.html");
    }

    private void parseImageViewerdContent() throws IOException {
        this.imageViewerContent=parseContent("imageViewer.html");
    }

    private void parseTextViewerdContent() throws IOException {
        this.textViewerContent=parseContent("textViewer.html");
    }

    private void parseFolderViewerdContent() throws IOException {
        this.folderViewerContent=parseContent("folderViewer.html");
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

    private String generateMimeTypeResponse(String mimetype,String htmlContent){
        return "HTTP/1.1 200 OK\r\n"+
                "Server: Java HTTP Server: 1.0\r\n"+
                "Date: " + new Date() + "\r\n"+
                "Content-Type: "+mimetype+"\r\n"+
                "Content-Length: " + htmlContent.length() + "\r\n"+
                "\r\n"+
                htmlContent;
    }

    private String generateHtmlResponse(String htmlContent){
        return generateMimeTypeResponse("text/html",htmlContent);
    }

    private void sendFileOverHTTP(OutputStream out,File file) throws IOException {
        out.write("HTTP/1.1 200 OK\r\n".getBytes());
        out.write("Accept-Ranges: bytes\r\n".getBytes());
        out.write(("Content-Length: "+file.length()+"\r\n").getBytes());
        out.write("Content-Type: application/octet-stream\r\n".getBytes());
        out.write(("Content-Disposition: attachment; filename=\""+file.getName()+"\"\r\n").getBytes());
        out.write("\r\n".getBytes());
        Files.copy(Paths.get(file.getAbsolutePath()) , out);
    }

    public void run()
    {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            String input = in.readLine();

            if(input == null) return;
            if(input.length() > 0) {
                if(input.startsWith("GET"))
                {
                    String route=input.split(" ")[1].split("[?]")[0].replaceAll("%20"," ");
                    String path=Paths.get(Paths.get("").toAbsolutePath().toAbsolutePath().toString(),route).toString();

                    if(!route.equals("/favicon.ico")){
                        System.out.println(route);
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
                                pr.flush();
                            }else{
                                if(isTextFile(requestedFile)){
                                    PrintWriter pr = new PrintWriter(this.socket.getOutputStream());
                                    String textViewerResponse=this.textViewerContent.replace("{title}",requestedFile.getName()).replace("{src}",parseContent(path));
                                    pr.write(generateHtmlResponse(textViewerResponse));
                                    pr.flush();
                                }
                                else if(isImageFile(requestedFile)){
                                    FileInputStream fin = new FileInputStream(requestedFile);
                                    byte imagebytearray[] = new byte[(int)requestedFile.length()];
                                    fin.read(imagebytearray);
                                    String base64 = Base64.getEncoder().encodeToString(imagebytearray);
                                    PrintWriter pr = new PrintWriter(this.socket.getOutputStream());
                                    String imageViewerResponse=this.imageViewerContent.replace("{title}",requestedFile.getName()).replace("{src}","data:"+getMimeType(requestedFile)+";base64, "+base64);
                                    pr.write(generateHtmlResponse(imageViewerResponse));
                                    pr.flush();
                                }else{
                                    OutputStream out=socket.getOutputStream();
                                    sendFileOverHTTP(out,requestedFile);
                                    out.flush();
                                    out.close();
                                }
                            }
                        }else {
                            PrintWriter pr = new PrintWriter(this.socket.getOutputStream());
                            pr.write(generateHtmlResponse(notFoundContent));
                            pr.flush();
                            //System.out.println(input+" 404 : Page Not Found");
                        }
                    }
                }

                else
                {

                }
            }
            this.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
