package utility;

public class Config {
    public static final String SERVER_URL="localhost";
    public static final int SERVER_PORT=5001;
    public static final int CHUNK_SIZE_BYTES=4*1024;
    public static final String UPLOAD_FOLDER_NAME="uploaded";
    public static final String NOT_FOUND_HTML="404.html";
    public static final String IMAGE_VIEWER_HTML="imageViewer.html";
    public static final String TEXT_VIEWER_HTML="textViewer.html";
    public static final String FOLDER_VIEWER_HTML="folderViewer.html";
    public static final String MSG_DOWNLOAD_ABORTED="Download is aborted by client...";
    public static final String MSG_NOT_FOUND="Download is aborted by client...";
    public static final String MSG_INVALID_UPLOAD="Invalid Upload Request";
    public static final String CLIENT_UPLOAD_COMMAND="UPLOAD";
    public static final String CLIENT_UPLOAD_NONEXIST="NONEXIST";
    public static final String CLIENT_UPLOAD_UNSUPPORTED="UNSUPPORTED";
    public static final String CLIENT_UPLOAD_READY="READY";
    public static final String CLIENT_SUCCESS_MSG="Upload Completed";
    public static final String CLIENT_ERROR_MSG="Invalid Upload Request";




    public static final String ANSI_BLACK_BACKGROUND = "\u001B[40m";
    public static final String ANSI_RED_BACKGROUND = "\u001B[41m";
    public static final String ANSI_GREEN_BACKGROUND = "\u001B[42m";
    public static final String ANSI_YELLOW_BACKGROUND = "\u001B[43m";
    public static final String ANSI_BLUE_BACKGROUND = "\u001B[44m";
    public static final String ANSI_PURPLE_BACKGROUND = "\u001B[45m";
    public static final String ANSI_CYAN_BACKGROUND = "\u001B[46m";
    public static final String ANSI_WHITE_BACKGROUND = "\u001B[47m";
}
