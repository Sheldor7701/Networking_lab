import java.io.*;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

public class Worker extends Thread {
    SocketAddress Client_IP;
    Socket socket;
    String username;
    boolean active = true;
    String currentFile = null;
    FileOutputStream current_stream = null;
    long filesize = 0;

    public Worker(Socket socket)
    {
        this.socket = socket;
        this.Client_IP = socket.getRemoteSocketAddress();
    }

    public void send_userlist(ObjectOutputStream out) throws IOException {
        // LookUp Users
        System.out.println(username + " requested for user list");
        // get active users
        ArrayList<String> active_users = Server.get_active_users();
        out.writeObject(active_users);
        // get all users logged in at least once
        String[] users = Server.get_users();
        out.writeObject(users);
    }

    public void send_filelist(ObjectOutputStream out) throws IOException{
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
    }

    public void send_filelist(ObjectOutputStream out, String id) throws IOException{
        System.out.println(username + " wants to see files of " + id);
        List<String> results = new ArrayList<String>();
        // send all public files of student id to client
        File[] files = new File("ServerFiles/" + id + "/public/" ).listFiles();
        //If this pathname does not denote a directory, then listFiles() returns null.
        for (File file : files) {
            if (file.isFile()) {
                int fileid = Server.get_file_id("ServerFiles/" + id + "/public/"+file.getName());
                results.add(fileid + " : " + file.getName());
            }
        }
        // results contains the list of all the file names.
        out.writeObject(results);
    }

    public void sendFile(ObjectInputStream in, ObjectOutputStream out) throws IOException {
        int file_id;
        try{
            file_id = (int) in.readObject();
        }
        catch (Exception e){
            return;
        }
        System.out.println(file_id);
        String file_path = Server.get_file_path(file_id);
        System.out.println(username + " wants to download " + file_path);
        if( file_path == null ){
            System.out.println("No such file in your directory");
            out.writeObject("There is no such file in this name in your folders");
            return;
        }
        if(file_path.startsWith("ServerFiles/" + username + "/public/") | file_path.startsWith( "ServerFiles/" + username + "/private/")){
            out.writeObject("exists");
            File file = new File(file_path);
            out.writeObject(file.getName());
            Sender sender=new Sender(file, out);
            sender.sendFile();
        }
        else{
            System.out.println("No such file in your directory");
            out.writeObject("There is no such file in this name in your folders");
            return;
        }

    }

    public void sendFile(ObjectInputStream in, ObjectOutputStream out, String id) throws IOException {
        int file_id;
        try{
            file_id = (int) in.readObject();
        }
        catch (Exception e){
            return;
        }
        System.out.println(file_id);
        String file_path = Server.get_file_path(file_id);
        System.out.println(username + " wants to download " + file_path);
        if( file_path == null ){
            System.out.println("No such file in your directory");
            out.writeObject("There is no such file in this name in your folders");
            return;
        }
        if(file_path.startsWith("ServerFiles/" + id + "/public/")){
            out.writeObject("exists");
            File file = new File(file_path);
            out.writeObject(file.getName());
            Sender sender=new Sender(file, out);
            sender.sendFile();
        }
        else{
            System.out.println("No such file in your directory");
            out.writeObject("There is no such file in this name in your folders");
            return;
        }

    }

    public void logout(ObjectInputStream in, ObjectOutputStream out) throws IOException {
        in.close();
        out.close();
        socket.close();
        Server.logout(username);
        System.out.println(username + " has logged out");
        active = false;
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
                        send_userlist(out);
                    }
                    else if( option.equalsIgnoreCase("2") ){
                        // See your files and download them
                        send_filelist(out);

                        String download = (String) in.readObject();
                        // download : 1. wants to download a file   2. doesn't want to download a file
                        if(download.equalsIgnoreCase("1")){
                            // client wants to download a file
                            // read filename from client
                            sendFile(in,out);

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
                        Reciever reciever= new Reciever(this, type, out, in);
                        String filename = reciever.receiveFile();
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
                        send_userlist(out);
                        // See others files and download them
                        // read the username
                        String id = (String) in.readObject();
                        String[] users = Server.get_users();
                        // check if the username was at least once active
                        boolean exists = false;
                        for( int i = 0 ; i < users.length ; i++ ){
                            if( users[i].equalsIgnoreCase(id) ){
                                exists = true;
                                break;
                            }
                        }
                        if( exists == true ){
                            // user with id exists
                            send_filelist(out, id);
                            String download = (String) in.readObject();
                            // download : 1. wants to download a file   2. doesn't want to download a file
                            if(download.equalsIgnoreCase("1")){
                                System.out.println(username+" wants to download a file from "+ id);
                                // client wants to download a file
                                // read filename from client
                                sendFile(in,out,id);

                            }
                            // read if client wish to download a file. 1. Yes   2.  No

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
                        logout(in,out);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            if(currentFile != null){
                try {
                    if(current_stream != null){
                        current_stream.close();
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
