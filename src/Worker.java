import java.io.*;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Worker extends Thread {
    public SocketAddress Client_IP;
    public Socket socket;
    public String username;
    public String currentFile = null;
    public FileOutputStream current_stream = null;
    public long filesize= 0;

    public Worker(Socket socket)
    {
        this.socket = socket;
        this.Client_IP = socket.getRemoteSocketAddress();
    }

    public void send_userlist(ObjectOutputStream out) throws IOException {
        System.out.println(username + " requested for user list");
        // send active users
        ArrayList<String> active_users = Server.get_active_users();
        out.writeObject(active_users);
        // send all users logged in at least once
        String[] users = Server.get_all_users();
        out.writeObject(users);
    }

    public void send_file_type(ObjectOutputStream out, String type) throws IOException{
        ArrayList<String> results = new ArrayList<>();
        results.add(type + " :");
        File[] files = new File("ServerFiles/" + username + "/" + type + "/" ).listFiles();
        //If this pathname does not denote a directory, then listFiles() returns null.
        for (File file : files) {
            if (file.isFile()) {
                int fileid = Server.get_file_id("ServerFiles/" + username + "/" + type + "/" + file.getName());
                results.add(fileid + " : " + file.getName());
            }
        }
        out.writeObject(results);
    }


    public void send_filelist(ObjectOutputStream out) throws IOException{
        send_file_type(out, "public");
        send_file_type(out, "private");
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

    public void sendFile(ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {
        int file_id=(int) in.readObject();
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

    public void receiveFile(ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {

        String type = (String) in.readObject();// type -- Public / Private
        int req_id = -1; //only used when type == 3(request grant)
        if( type.equalsIgnoreCase("1") ){
            type = "public";
        }
        else if( type.equalsIgnoreCase("2") ){
            type = "private";
        }
        else if( type.equalsIgnoreCase("3") ){
            send_requests(out); // send others requests
            type = "public";
            req_id = (int) in.readObject();
            boolean exists = Server.check_request_id(req_id);
            out.writeObject(exists);
            if( exists == false ){
                return;
            }
        }
        Reciever reciever= new Reciever(this, type, out, in);
        String filename = reciever.receive_File();
        Server.addUpload_to_Request(req_id, username, "ServerFiles/" + username + "/public/" + filename);
    }

    public void send_requests(ObjectOutputStream out) throws IOException{
        // view unread messages
        ArrayList<Request> requests = Server.getRequests();
        ArrayList<String> other_req = new ArrayList<>();
        for (Request request : requests) {

            if (!request.getRequester().equalsIgnoreCase(username)) {
                other_req.add(request.getRequest_id() + " : " + request.getRequester() + " requested for a file : " + request.getDescription());
            }
        }
        out.writeObject(other_req);
    }

    public void send_uploads(ObjectOutputStream out) throws IOException{
        // view unread messages
        ArrayList<Request> requests = Server.getRequests();
        ArrayList<String> own_req = new ArrayList<>();
        for (Request request : requests) {
            if (request.getRequester().equalsIgnoreCase(username)) {
                // if a request was made by client
                Map<String, String> uploads = request.getUploads();

                if (uploads.isEmpty()) {
                    continue; // no file uploaded against the request made
                }

                // add all the upload info in own_req
                for (Map.Entry<String, String> entry : uploads.entrySet()) {
                    String uploaderId = entry.getKey();
                    String filepath = entry.getValue();
                    own_req.add(request.getRequest_id() + " : " + uploaderId + " uploaded : " + filepath);
                }
                //Server.removeRequest(request.getRequest_id());
            }
        }
        out.writeObject(own_req);
    }


    public void logout(ObjectInputStream in, ObjectOutputStream out) throws IOException {
        in.close();
        out.close();
        socket.close();
        Server.logout(username);
        System.out.println(username + " has logged out");
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
                        send_filelist(out);// send list of all files

                        String download = (String) in.readObject();
                        if(download.equalsIgnoreCase("1"))// client wants to download a file
                        {
                            sendFile(in,out);
                        }
                        // user doesn't wish to download a file
                    }else if( option.equalsIgnoreCase("3") ){
                        // receive uploaded files
                        receiveFile(in,out);

                    }else if( option.equalsIgnoreCase("4") ){
                        // requesting a file
                        System.out.println(username + " wants to request for a file.");
                        String description = (String) in.readObject();
                        Server.addRequest(username, description);
                    }else if( option.equalsIgnoreCase("5") ){
                        send_requests(out);//send others requests
                    }else if( option.equalsIgnoreCase("6") ){
                        send_uploads(out);//send uploads for your requests
                    }
                    else if( option.equalsIgnoreCase("7") ){
                        send_userlist(out);
                        // See others files and download them
                        // read the username
                        String id = (String) in.readObject();
                        String[] users = Server.get_all_users();
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
                    else if( option.equalsIgnoreCase("8") ){
                        //logging out
                        logout(in,out);
                        break;
                    }
                }
            }
        }
        catch (Exception e) {
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
