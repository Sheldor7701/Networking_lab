import java.io.*;

public class Sender {
    private File file;
    private ObjectOutputStream out;

    public Sender(File file, ObjectOutputStream out) {
        this.file = file;
        this.out = out;
    }

    public void sendFile() throws IOException {
        long length = file.length();
        out.writeObject(length); // send file size

        int chunk_size=Server.get_max_chunk_size();
        out.writeObject(chunk_size); // send chunk size

        byte[] buffer = new byte[Server.get_max_chunk_size()]; // create buffer
        try (InputStream fileIn = new FileInputStream(file)) {
            int bytesRead;
            while ((bytesRead = fileIn.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead); // send chunk of data
            }
        }
        out.writeObject("COMPLETED");
    }

}
