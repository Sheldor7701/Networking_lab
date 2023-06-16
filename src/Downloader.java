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
            path = "Downloads/"+username+"/";
            directory = new File(path);

            if (!directory.exists()) {
                directory.mkdir();
            }

            // get file size
            long filesize = (long) in.readObject();
            System.out.println("Downloading " + filename + " of size " + filesize);
            File file=new File(path + filename);

            FileOutputStream fos = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(fos);

            String acknowledge = "";
            int bytesread = 0;
            int total = 0;

            while (true) {
                Object o = in.readObject();
                if( o.getClass().equals(acknowledge.getClass() ) ){
                    acknowledge = (String) o;
                    break;
                }
                byte[] con = (byte[]) o;
                bytesread = con.length;
                total += bytesread;
                bos.write(con, 0, bytesread);
            }
            bos.flush();
            bos.close();
            fos.close();

            System.out.println(acknowledge);
            if(acknowledge.equalsIgnoreCase("COMPLETED")){
                System.out.println(" File download completed");
            }

        }catch(Exception e){
            System.out.println(e);
        }

    }
}
