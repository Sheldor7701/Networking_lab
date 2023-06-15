import java.io.*;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Worker extends Thread {
    SocketAddress Client_IP;
    Socket socket;
    String username;

    boolean active = true;
    String currentFile = null;
    FileOutputStream current_str = null;
    long filesize = 0;

    public Worker(Socket socket)
    {
        this.socket = socket;
        this.Client_IP = socket.getRemoteSocketAddress();
    }

    public void sendFile(File file, ObjectOutputStream out){
        try{
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
        catch(Exception e){
            System.out.println(e);
        }

    }

    public String receiveFile(String type, ObjectInputStream in, ObjectOutputStream out) {
        try {
            // get file name
            String filename = (String) in.readObject();
            // get file size
            filesize = (long) in.readObject();
            System.out.println(username + " wants to upload a " + type + " file " + filename + " of size : " + filesize + " bytes");
            // check buffer availability
            boolean available = Server.checkBuffer(filesize);
            out.writeObject(available);
            if(!available) return null;
            // send chunk size
            int chunk_size = Server.get_random_chunk_size();
            out.writeObject(chunk_size);
            currentFile = "ServerFiles/" + username + "/" + type + "/" + filename;
            int fileID = Server.addFile(currentFile) ;
            out.writeObject(fileID);
            File file=new File(currentFile);

            FileOutputStream fos = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            current_str = fos;

            String acknowledge = "";
            boolean terminate = false;
            int bytesread = 0;
            int total = 0;
            long remainder = ( ( (filesize % chunk_size) - 1 ) >> 31) ^ 1;
            long loop = (filesize / chunk_size) + remainder;
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
                out.writeObject(total + " bytes received out of " + filesize);
                count += 1;
            }
            bos.flush();
            bos.close();
            fos.close();
            current_str = null;

            if(terminate == false){
                acknowledge = (String) in.readObject();
            }

            System.out.println(acknowledge);
            if(acknowledge.equalsIgnoreCase("COMPLETED")){
                Server.clearBuffer(total);
                if(total == filesize){
                    out.writeObject("SUCCESS");
                }
                else{
                    System.out.println( currentFile + ": deleting due to incomplete" );
                    out.writeObject("FAILURE");
                    File to_delete = new File(currentFile);
                    boolean success = to_delete.delete();
                    if(success == true){
                        System.out.println( currentFile + ": deleting successful");
                    }
                    else{
                        System.out.println(currentFile + " deletetion failed");
                    }
                }
            }
            else if(acknowledge.equalsIgnoreCase("TIMEOUT")){
                File to_delete = new File(currentFile);
                boolean success = to_delete.delete();
                if(success == true){
                    System.out.println( currentFile + ": deleting due to timeout");
                }
                else{
                    System.out.println(currentFile + " deletetion failed");
                }
            }
            currentFile = null;
            return filename;

        }catch(Exception e){
            System.out.println(e);
            return null;
        }

    }

    public void run()
    {
        // buffers
        try {
            ObjectOutputStream out = new ObjectOutputStream(this.socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(this.socket.getInputStream());

            while (true)
            {
                username=(String) in.readObject();
                boolean connect = Server.login(username, Client_IP);
                out.writeObject(connect);
                if(connect == false){
                    continue;
                }

                while(true){
                    String option = (String) in.readObject();

                    if( option.equalsIgnoreCase("1") ){
                        // LookUp Users
                        System.out.println(username + " requested for user list");
                        // get active users
                        ArrayList<String> active_users = Server.get_active_users();
                        out.writeObject(active_users);
                        // get all users logged in at least once
                        String[] users = Server.get_users();
                        out.writeObject(users);
                    }
                    else if( option.equalsIgnoreCase("2") ){
                        // See your files and download them
                        String[] types = {"public", "private"};
                        for(int i = 0 ; i < types.length ; i++){
                            // i = 0 : send public files
                            // i = 1 : send private files
                            List<String> results = new ArrayList<String>();
                            results.add(types[i] + " :");
                            File[] files = new File("ServerFiles/" + username + "/" + types[i] + "/" ).listFiles();
                            //If this pathname does not denote a directory, then listFiles() returns null.
                            for (File file : files) {
                                if (file.isFile()) {
                                    int fileid = Server.get_file_id("ServerFiles/" + username + "/" + types[i] + "/" + file.getName());
                                    results.add(fileid + " : " + file.getName());
                                }
                            }
                            // results contains the list of all the file names.
                            out.writeObject(results);
                            results.clear();
                        }
                        String download = (String) in.readObject();
                        // download : 1. wants to download a file   2. doesn't want to download a file
                        if(download.equalsIgnoreCase("1")){
                            // client wants to download a file
                            // read filename from client
                            int file_id;
                            try{
                                file_id = (int) in.readObject();
                            }
                            catch (Exception e){
                                continue;
                            }
                            System.out.println(file_id);
                            String file_path = Server.get_file_path(file_id);
                            System.out.println(username + " wants to download " + file_path);
                            if( file_path == null ){
                                System.out.println("No such file in your directory");
                                out.writeObject("There is no such file in this name in your folders");
                                continue;
                            }
                            if(file_path.startsWith("ServerFiles/" + username + "/public/") | file_path.startsWith( "ServerFiles/" + username + "/private/")){
                                out.writeObject("exists");
                                File file = new File(file_path);
                                out.writeObject(file.getName());
                                sendFile(file, out);
                            }
                            else{
                                System.out.println("No such file in your directory");
                                out.writeObject("There is no such file in this name in your folders");
                                continue;
                            }

                        }
                        else if(download.equalsIgnoreCase("2")){
                            // user doesn't wish to download a file
                            continue;
                        }
                    }else if( option.equalsIgnoreCase("3") ){
                        // receive uploaded files
                        // type -- Public / Private
                        String type = (String) in.readObject();
                        int req_id = -1;
                        if( type.equalsIgnoreCase("1") ){
                            type = "public";
                        }
                        else if( type.equalsIgnoreCase("2") ){
                            type = "private";
                        }
                        else if( type.equalsIgnoreCase("3") ){
                            type = "public";
                            req_id = (int) in.readObject();
                            boolean exists = Server.check_request_id(req_id);
                            out.writeObject(exists);
                            if( exists == false ){
                                continue;
                            }
                        }
                        String filename = receiveFile(type, in, out);
                        Server.addUpload(req_id, username, "ServerFiles/" + username + "/public/" + filename);
                    }else if( option.equalsIgnoreCase("4") ){
                        // requesting a file
                        System.out.println(username + " wants to request for a file.");
                        String description = (String) in.readObject();
                        Server.addRequest(username, description);
                    }else if( option.equalsIgnoreCase("5") ){
                        // view unread messages
                        ArrayList<Request> req = Server.getRequests();
                        ArrayList<String> other_req = new ArrayList<String>();
                        ArrayList<String> own_req = new ArrayList<String>();
                        for(int i = 0 ; i < req.size() ; i++ ){
                            // if a request was made by client
                            if(req.get(i).getRequester().equalsIgnoreCase(username)){
                                ArrayList<Pair> pairs = req.get(i).getuploads();
                                // no file uploaded against the request made
                                if(pairs.size() == 0){
                                    continue;
                                }
                                // add all the upload info in own_req
                                for( int j = 0 ; j < pairs.size() ; j++ ){
                                    own_req.add(req.get(i).getRequest_id() + " : " + pairs.get(j).getUploader_id() + " uploaded : " + pairs.get(j).getFilepath());
                                }
                                Server.removeRequest(req.get(i).getRequest_id());
                            }
                            else{
                                // requests by other clients
                                other_req.add(req.get(i).getRequest_id() + " : " + req.get(i).getRequester() + " requested for a file : " + req.get(i).getDescription());
                            }
                        }
                        out.writeObject(other_req);
                        out.writeObject(own_req);
                    }
                    else if( option.equalsIgnoreCase("6") ){
                        // See others files and download them
                        // read the student id
                        String id = (String) in.readObject();
                        String[] users = Server.get_users();
                        // check if the student id was at least once active
                        boolean exists = false;
                        for( int i = 0 ; i < users.length ; i++ ){
                            if( users[i].equalsIgnoreCase(id) ){
                                exists = true;
                                break;
                            }
                        }
                        if( exists == true ){
                            // user with id exists
                            System.out.println(username + " wants to see files of " + id);
                            List<String> results = new ArrayList<String>();
                            // send all public files of student id to client
                            File[] files = new File("ServerFiles/" + id + "/public/" ).listFiles();
                            //If this pathname does not denote a directory, then listFiles() returns null.
                            for (File file : files) {
                                if (file.isFile()) {
                                    results.add(file.getName());
                                }
                            }
                            // sending the public files list to client
                            out.writeObject(results);
                            // read if client wish to download a file. 1. Yes   2.  No
                            String download = (String) in.readObject();
                            if( download.equalsIgnoreCase("1") ){
                                // download a file
                                // read the filename from the client
                                String filename = (String) in.readObject();
                                File file = new File("ServerFiles/" + id + "/public/" + filename);
                                // if a file doesn't exist with a filename, send a message to client
                                if(!file.exists()){
                                    System.out.println("No such file with " + filename);;
                                    out.writeObject("No such file");
                                    continue;
                                }
                                // send file with filename to client
                                out.writeObject("exists");
                                sendFile(file, out);
                            }
                            else if( download.equalsIgnoreCase("2") ){
                                // do not want to download a file
                                continue;
                            }
                        }
                        else{
                            // user with id doesn't exist
                            Integer i = 0;
                            System.out.println(id + " doesnt exist");
                            out.writeObject(i);
                        }
                    }
                    else if( option.equalsIgnoreCase("7") ){
                        //logging out
                        in.close();
                        out.close();
                        socket.close();
                        Server.logout(username);
                        System.out.println(username + " has logged out");
                        active = false;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            if(currentFile != null){
                try {
                    if(current_str != null){
                        current_str.close();
                    }
                } catch (IOException ex) {
                    System.out.println(ex);
                }
                File to_delete = new File(currentFile);
                Server.clearBuffer(filesize);
                boolean success = to_delete.delete();
                if(success == true){
                    System.out.println(currentFile + " deleting due to " + username + " going offline.");
                }
                else{
                    System.out.println(currentFile + " deletion failed.");
                }

                currentFile = null;
            }
            System.out.println(username + " has gone offline, Logging out");
            Server.logout(username);
        }
    }


}
