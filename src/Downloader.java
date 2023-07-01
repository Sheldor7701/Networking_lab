import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;

public class Downloader {

    private String username;
    private String filename;
    private ObjectInputStream in;

    public Downloader(String username, String filename, ObjectInputStream in) {
        this.username = username;
        this.filename = filename;
        this.in = in;
    }

    public void downloadFile() {
        try {
            String path = "Downloads/";
            File directory = new File(path);

            if (!directory.exists()) {
                directory.mkdir();
            }
            path = "Downloads/" + username + "/";
            directory = new File(path);

            if (!directory.exists()) {
                directory.mkdir();
            }

            // get file size
            long filesize = (long) in.readObject();
            System.out.println("Downloading " + filename + " of size " + filesize);

            // get chunk size
            int chunkSize = (int) in.readObject();
            System.out.println("Using chunk size: " + chunkSize);

            File file = new File(path + filename);

            FileOutputStream fos = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(fos);

            byte[] buffer = new byte[chunkSize];
            int bytesRead;
            long totalBytesRead = 0;

            while (totalBytesRead < filesize) {
                int bytesToRead = (int) Math.min(chunkSize, filesize - totalBytesRead);
                bytesRead = in.read(buffer, 0, bytesToRead);
                if (bytesRead == -1) {
                    break;
                }
                bos.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
            }

            bos.flush();
            bos.close();
            fos.close();

            String acknowledge = (String) in.readObject();
            System.out.println(acknowledge);
            if (acknowledge.equalsIgnoreCase("COMPLETED")) {
                System.out.println("File download completed");
            }

        } catch (Exception e) {
            System.out.println(e);
        }
    }

}
