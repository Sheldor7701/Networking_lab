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

    public String receiveFile() throws IOException, ClassNotFoundException {
        // get file name
        String filename = (String) in.readObject();
        // get file size
        worker.filesize = (long) in.readObject();
        System.out.println(worker.username + " wants to upload a " + type + " file " + filename + " of size : " + worker.filesize + " bytes");
        // check buffer availability
        boolean available = Server.checkBuffer(worker.filesize);
        out.writeObject(available);
        if(!available) return null;
        // send chunk size
        int chunk_size = Server.get_random_chunk_size();
        out.writeObject(chunk_size);
        worker.currentFile = "ServerFiles/" + worker.username + "/" + type + "/" + filename;
        int fileID = Server.addFile(worker.currentFile) ;
        out.writeObject(fileID);
        File file=new File(worker.currentFile);

        FileOutputStream fos = new FileOutputStream(file);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        worker.current_stream = fos;

        String acknowledge = "";
        boolean terminate = false;
        int bytesread = 0;
        int total = 0;
        long remainder = ( ( (worker.filesize % chunk_size) - 1 ) >> 31) ^ 1;
        long loop = (worker.filesize / chunk_size) + remainder;
        long count = 0;
        while (count != loop) {
            //bytesread = dis.read(contents);
            Object o = in.readObject();
            if( o.getClass().equals(acknowledge.getClass() ) ){
                acknowledge = (String) o;
                terminate = true;
                break;
            }
            byte[] con = (byte[]) o;
            bytesread = con.length;
            total += bytesread;
            bos.write(con, 0, bytesread);
            // simulate timeout
//                if(count == 6){
//                    Thread.sleep(31000);
//                    continue;
//                }
            out.writeObject(total + " bytes received out of " + worker.filesize);
            count += 1;
        }
        bos.flush();
        bos.close();
        fos.close();
        worker.current_stream = null;

        if(terminate == false){
            acknowledge = (String) in.readObject();
        }

        System.out.println(acknowledge);
        if(acknowledge.equalsIgnoreCase("COMPLETED")){
            Server.clearBuffer(total);
            if(total == worker.filesize){
                out.writeObject("SUCCESS");
            }
            else{
                System.out.println( worker.currentFile + ": deleting due to incomplete" );
                out.writeObject("FAILURE");
                File to_delete = new File(worker.currentFile);
                boolean success = to_delete.delete();
                if(success == true){
                    System.out.println( worker.currentFile + ": deleting successful");
                }
                else{
                    System.out.println(worker.currentFile + " deletetion failed");
                }
            }
        }
        else if(acknowledge.equalsIgnoreCase("TIMEOUT")){
            File to_delete = new File(worker.currentFile);
            boolean success = to_delete.delete();
            if(success == true){
                System.out.println( worker.currentFile + ": deleting due to timeout");
            }
            else{
                System.out.println(worker.currentFile + " deletetion failed");
            }
        }
        worker.currentFile = null;
        return filename;
    }
}
