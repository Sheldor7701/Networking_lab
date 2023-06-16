import java.io.*;

public class Sender {
    private File file;
    private ObjectOutputStream out;

    public Sender(File file, ObjectOutputStream out) {
        this.file = file;
        this.out = out;
    }

    public void sendFile() throws IOException {
    // send file size
        long length = file.length();
        out.writeObject(length);
        // get chunk size from server
        int chunk_size = Server.get_max_chunk_size();
        InputStream file_in = new FileInputStream(file); //create an inputstream from the file
        byte[] buf = new byte[chunk_size]; //create buffer
        int len = 0;
        int count = 0;
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
            count += 1;
//                if(count == 5){
//                    break;
//                }
        }
        out.writeObject("COMPLETED");
        file_in.close();
    }
}
