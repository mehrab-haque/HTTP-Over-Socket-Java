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

    public HTTPWorker(Socket socket) throws IOException {
        this.socket = socket;
        parseNotFoundContent();
        parseImageViewerdContent();
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

    private String generateHtmlResponse(String htmlContent){
        return "HTTP/1.1 200 OK\r\n"+
                "Server: Java HTTP Server: 1.0\r\n"+
                "Date: " + new Date() + "\r\n"+
                "Content-Type: text/html\r\n"+
                "Content-Length: " + htmlContent.length() + "\r\n"+
                "\r\n"+
                htmlContent;
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
                    String route=input.split(" ")[1];
                    String path=Paths.get(Paths.get("").toAbsolutePath().toAbsolutePath().toString(),route).toString();

                    if(!route.equals("/favicon.ico")){
                        File requestedFile=new File(path);
                        if(requestedFile.exists()){
                            if(requestedFile.isDirectory()){

                            }else{
                                if(isTextFile(requestedFile)){
                                    System.out.println("hi");
                                }
                                else if(isImageFile(requestedFile)){
                                    System.out.println(getMimeType(requestedFile));
                                    FileInputStream fin = new FileInputStream(requestedFile);
                                    byte imagebytearray[] = new byte[(int)requestedFile.length()];
                                    fin.read(imagebytearray);
                                    String base64 = Base64.getEncoder().encodeToString(imagebytearray);
                                    PrintWriter pr = new PrintWriter(this.socket.getOutputStream());
                                    String imageViewerResponse=this.imageViewerContent.replace("{title}",requestedFile.getName()).replace("{src}","data:"+getMimeType(requestedFile)+";base64, "+base64);
                                    pr.write(generateHtmlResponse(imageViewerResponse));
                                    pr.flush();
                                }

//                                FileInputStream fs=new FileInputStream(path);
//                                OutputStream out = socket.getOutputStream();
//                                int reads=0;
//                                while((reads=fs.read())!=-1)
//                                {
//                                    out.write(reads);
//                                }
//                                out.close();
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
