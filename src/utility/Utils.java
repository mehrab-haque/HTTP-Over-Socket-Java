package utility;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class Utils {
    public static String getMimeType(File file) throws IOException {
        return Files.probeContentType(file.toPath());
    }

    public static byte[] getChunk(byte[] bytes,int startIndex,int length){
        byte[] chunk=new byte[length];
        for(int i=0;i<length;i++)
            chunk[i]=bytes[startIndex+i];
        return chunk;
    }

    public static boolean isImageFile(File src) throws IOException {
        String mimetype = Utils.getMimeType(src);
        return mimetype != null && mimetype.split("/")[0].equals("image");
    }
}
