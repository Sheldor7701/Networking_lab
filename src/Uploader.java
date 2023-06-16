import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Scanner;

public class Uploader {

    private ObjectOutputStream out;
    private ObjectInputStream in;

    public Uploader(ObjectOutputStream out, ObjectInputStream in) {
        this.out = out;
        this.in = in;
    }

    public void uploadFile(Socket socket){
        try{
            System.out.println("Select file to upload from the dialogue");
            FileDialog dialog = new FileDialog((Frame)null, "Select File to Open");
            dialog.setMode(FileDialog.LOAD);
            dialog.setVisible(true);
            System.out.println(dialog.getDirectory() + dialog.getFile() + " chosen. ");
            File upload_file = new File(dialog.getDirectory() + dialog.getFile() );
            // send file name
            System.out.println("Choose a filename : ");
            Scanner sc= new Scanner(System.in);
            String filename = sc.nextLine();
            out.writeObject(filename);
            // send file size
            long length = upload_file.length();
            out.writeObject(length);
            // check if buffer is available
            boolean available = (boolean) in.readObject();
            if(available == false){
                System.out.println("Buffer limit exceeded");
                return;
            }
            // get chunk size from server
            int chunk_size = (int) in.readObject();
            System.out.println("Chunk size : " + chunk_size);
            // get file ID
            int file_Id = (int) in.readObject();
            InputStream file_in = new FileInputStream(upload_file); //create an inputstream from the file
            byte[] buf = new byte[chunk_size]; //create buffer
            int len = 0;
            socket.setSoTimeout(30000);
            while ((len = file_in.read(buf)) != -1) {
                //os.write(buf, 0, len); //write buffer
                // copy buf to len array
                if(len == chunk_size){
                    out.writeObject(buf);
                }
                else{
                    byte[] buf2 = new byte[len];
                    System.arraycopy(buf, 0, buf2, 0, len);
                    out.writeObject(buf2);
                }
                out.reset();
                String confirmation = (String) in.readObject();
                System.out.println(confirmation);
                Thread.sleep(1000);
            }
            out.writeObject("COMPLETED");
            file_in.close();
            String check = (String) in.readObject();
            if(check.equalsIgnoreCase("SUCCESS")){
                System.out.println("File transfer successful");
            }
            else if(check.equalsIgnoreCase("FAILURE")){
                System.out.println("File transfer failed");
            }
        }
        catch(SocketTimeoutException se){
            System.out.println(se);
            try{
                out.writeObject("Timeout");
            }
            catch (Exception e){
                System.out.println(e);
            }

        }
        catch(Exception e){
            System.out.println(e);
        }

    }
}
