import java.io.*;

public class Reciever {

    private Worker worker;
    private String type;

    private ObjectOutputStream out;
    private ObjectInputStream in;

    public Reciever(Worker worker, String type, ObjectOutputStream out, ObjectInputStream in) {
        this.worker = worker;
        this.type = type;
        this.out = out;
        this.in = in;
    }

    public String receive_File() throws IOException, ClassNotFoundException {
        // Get file name
        String filename = (String) in.readObject();

        // Get file size
        long fileSize = (long) in.readObject();
        worker.filesize=fileSize;
        System.out.println(worker.username + " wants to upload a " + type + " file " + filename + " of size: " + fileSize + " bytes");

        // Check buffer availability
        boolean available = Server.checkBuffer(fileSize);
        out.writeObject(available);
        if (!available) {
            return null;
        }

        // Send chunk size
        int chunkSize = Server.get_random_chunk_size();
        out.writeObject(chunkSize);

        // Get file ID
        worker.currentFile = "ServerFiles/" + worker.username + "/" + type + "/" + filename;
        int fileID = Server.addFile(worker.currentFile);
        out.writeObject(fileID);

        // Create file to save the received data
        File file = new File(worker.currentFile);
        FileOutputStream fos = new FileOutputStream(file);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        worker.current_stream = fos;

        String acknowledge = "";
        int bytesRead = 0;
        int totalBytesRead = 0;
//        long loop = (worker.filesize / chunkSize) ;
//        if((loop*chunkSize)< worker.filesize)loop++;

        while (true) {
            Object o = in.readObject();
            if (o instanceof byte[]) {
                byte[] content = (byte[]) o;
                bytesRead = content.length;
                totalBytesRead += bytesRead;
                bos.write(content, 0, bytesRead);
            } else {
                acknowledge = (String) o;
                break;
            }

            out.writeObject(totalBytesRead + " bytes received out of " + fileSize); // sending confirmation of receiving each chunk

            // timeout
//                try {
//                    Thread.sleep(4000);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
        }

        bos.flush();
        bos.close();
        fos.close();
        worker.current_stream = null;

        System.out.println(acknowledge);
        if (acknowledge.equalsIgnoreCase("COMPLETED")) {
            Server.clearBuffer(totalBytesRead);
            if (totalBytesRead == fileSize) {
                out.writeObject("SUCCESS");
            } else {
                System.out.println(worker.currentFile + ": deleting due to incomplete");
                out.writeObject("FAILURE");
                File toDelete = new File(worker.currentFile);
                delete_File(toDelete);
            }
        } else if (acknowledge.equalsIgnoreCase("TIMEOUT")) {
            File toDelete = new File(worker.currentFile);
            delete_File(toDelete);
        }

        worker.currentFile = null;
        return filename;
    }

    public void delete_File(File file){
        if (file.delete()) {
            System.out.println(worker.currentFile + ": deleting successful");
        } else {
            System.out.println(worker.currentFile + " deletion failed");
        }
    }

}
