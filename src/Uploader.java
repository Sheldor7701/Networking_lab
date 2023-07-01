import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Scanner;

public class Uploader {

    private ObjectOutputStream out;
    private ObjectInputStream in;

    public Uploader(ObjectOutputStream out, ObjectInputStream in) {
        this.out = out;
        this.in = in;
    }

    public void uploadFile(Socket socket) throws IOException {
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
            int chunkSize = (int) in.readObject();
            System.out.println("Using chunk size: " + chunkSize);

            // get file ID
            int file_Id = (int) in.readObject();

            InputStream file_in = new FileInputStream(upload_file); //create an inputstream from the file
            byte[] buf = new byte[chunkSize]; //create buffer
            int len = 0;
            socket.setSoTimeout(30000);
            while ((len = file_in.read(buf)) != -1) {
                //os.write(buf, 0, len); //write buffer
                // copy buf to len array
                if(len == chunkSize){
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
                Thread.sleep(300);
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
            out.writeObject("Timeout");
        }
        catch(Exception e){
            System.out.println(e);
        }

    }

    public void upload_File(Socket socket) throws IOException {
        try {
            // Select file to upload
            System.out.println("Select file to upload from the dialogue");
            FileDialog dialog = new FileDialog((Frame) null, "Select File to Open");
            dialog.setMode(FileDialog.LOAD);
            dialog.setVisible(true);
            System.out.println(dialog.getDirectory() + dialog.getFile() + " chosen.");
            File uploadFile = new File(dialog.getDirectory() + dialog.getFile());

            // Send file name
            System.out.println("Choose a filename : ");
            Scanner sc = new Scanner(System.in);
            String filename = sc.nextLine();
            out.writeObject(filename);

            // Send file size
            long length = uploadFile.length();
            out.writeObject(length);

            // check if buffer is available
            boolean available = (boolean) in.readObject();
            if (available == false) {
                System.out.println("Buffer limit exceeded");
                return;
            }

            // get chunk size from server
            int chunkSize = (int) in.readObject();
            System.out.println("Using chunk size: " + chunkSize);

            // get file ID
            int file_Id = (int) in.readObject();

            // Read and send file data
            try (FileInputStream fileIn = new FileInputStream(uploadFile)) {
                byte[] buffer = new byte[chunkSize];
                int bytesRead;

                while ((bytesRead = fileIn.read(buffer)) != -1) {
                    if (bytesRead == chunkSize) {
                        out.writeObject(buffer); // Send full chunk
                    } else {
                        byte[] lastChunk = Arrays.copyOf(buffer, bytesRead);
                        out.writeObject(lastChunk); // Send last chunk with actual size
                    }
                    out.reset();
                    socket.setSoTimeout(3000);
                    String confirmation;
                    try {
                        confirmation = (String) in.readObject();
                    } catch (SocketTimeoutException e) {
                        System.out.println("Timeout occurred during data transfer.");
                        out.writeObject("Timeout");
                        String acknowledge = (String) in.readObject();
                        if(acknowledge.equalsIgnoreCase("SUCCESS")){
                            System.out.println("File transfer successful");
                        }
                        else if(acknowledge.equalsIgnoreCase("FAILURE")){
                            System.out.println("File transfer failed");
                        }else{
                            System.out.println(acknowledge);
                        }
                        return; // return on timeout
                    }
                    System.out.println(confirmation); //receiving confirmation from server
                    Thread.sleep(200);
                }
            }
            out.writeObject("COMPLETED");

            String acknowledge = (String) in.readObject();
            if(acknowledge.equalsIgnoreCase("SUCCESS")){
                System.out.println("File transfer successful");
            }
            else if(acknowledge.equalsIgnoreCase("FAILURE")){
                System.out.println("File transfer failed");
            }
        }
        catch(SocketTimeoutException se){
            System.out.println(se);
            out.writeObject("Timeout");
        }
        catch (Exception e){
            System.out.println(e);
        }
    }

}
