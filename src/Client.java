import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static java.lang.Integer.parseInt;

public class Client {

    private static Socket socket;

    private static String username;

    public static void show_users(ObjectInputStream in) throws IOException, ClassNotFoundException {
        // user wish to see the user list
        // active_users : all the users currently online
        ArrayList<String> active_users = (ArrayList<String>) in.readObject();
        // users : all the users logged in at least once
        String[] users = (String[]) in.readObject();
        // print the users
        for(int i=0; i<users.length; i++) {
            String user_name = users[i];
            // identify the ones who are currently active
            if(active_users.contains(user_name)){
                user_name += ": online";
            }
            System.out.println(user_name);
        }
    }

    public static void get_files(ObjectInputStream in) throws IOException, ClassNotFoundException {
        // public files
        ArrayList<String> public_files =  (ArrayList<String>) in.readObject();
        for(int i = 0; i < public_files.size() ; i++){
            System.out.println(public_files.get(i));
        }
        // private files
        ArrayList<String> private_files =  (ArrayList<String>) in.readObject();
        for(int i = 0; i < private_files.size() ; i++){
            System.out.println(private_files.get(i));
        }
    }

    public static void view_requests(ObjectInputStream in) throws IOException, ClassNotFoundException {
        // see others requests
        System.out.println("Requests : ");
        ArrayList<String> other_req = (ArrayList<String>) in.readObject();
        for(String s : other_req){
            System.out.println("Request ID : " + s);
        }
    }

    public static void view_uploads(ObjectInputStream in) throws IOException, ClassNotFoundException {
        // see others requests
        ArrayList<String> own_req = (ArrayList<String>) in.readObject();
        System.out.println("Uploads for your requests : ");
        for(String s : own_req){
            System.out.println("Request ID : " + s);
        }
    }

    public static void receive_file(String filename, ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {
        int File_id;
        File_id = Integer.parseInt(filename);
        out.writeObject(File_id);
        String available = (String) in.readObject();
        if(available.equalsIgnoreCase("exists")){
            filename = (String) in.readObject();
            Downloader client_downloader= new Downloader(username, filename, in);
            client_downloader.downloadFile();
        }
        else{
            System.out.println("File doesn't exist with the id : " + filename );
        }
    }

    public static void send_file(String type, ObjectOutputStream out, ObjectInputStream in) throws IOException, ClassNotFoundException {
        out.writeObject(type);
        if( type.equalsIgnoreCase("3") ){
            System.out.println("Enter request id ");
            Scanner sc= new Scanner(System.in);
            int req_id = parseInt(sc.nextLine());
            out.writeObject( req_id );
            // check if request id is valid, if not then continue
            boolean isValid = (boolean) in.readObject();
            if( isValid == false ){
                System.out.println("No such request id exists");
                return;
            }
        }
        if(type.equalsIgnoreCase("1") | type.equalsIgnoreCase("2" ) | type.equalsIgnoreCase("3") ){
            Uploader client_uploader= new Uploader(out, in);
            client_uploader.uploadFile(socket);
        }
    }

    public static void logout(ObjectInputStream in, ObjectOutputStream out) throws IOException {
        in.close();
        out.close();
        socket.close();
        System.out.println("Logging out and exiting");
        System.exit(0);
    }
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        socket = new Socket("localhost", 3000);

        System.out.println("Remote port: " + socket.getPort());
        System.out.println("Local port: " + socket.getLocalPort());

        Scanner sc= new Scanner(System.in);

        // buffers
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

        //
        while(true){
            System.out.println("Enter your username : ");
            username = sc.nextLine();
            out.writeObject(username);

            boolean connect = (boolean) in.readObject();
            if(connect == true){
                System.out.println("Successfully connected with server");
                break;
            }else{
                System.out.println("User is already logged in");
            }
        }

        while(true) {
            //print menu
            System.out.println(" 1. Lookup clients\n 2. Your files\n 3. Upload file\n 4. Request file\n 5. View\n 6. Lookup other users files\n 7. Logout");
            String option = sc.nextLine();
            out.writeObject(option);

            if(option.equalsIgnoreCase("1")){
                show_users(in);
            }
            else if( option.equalsIgnoreCase("2") ){
                // see all your files
                get_files(in);
                
                System.out.println("Do you want to download any file?\n1. Yes\t     2. No ");
                // download : 1. Yes : wants to download a file ; 2. No : doesn't want to download a file
                String download = sc.nextLine();
                out.writeObject(download);
                if(download.equalsIgnoreCase("1")){
                    // wants to download a file
                    System.out.println("Write the id of the file you want to download : ");
                    // write filename and send it
                    String filename = sc.nextLine();
                    receive_file(filename,in,out);
                }
                else if(download.equalsIgnoreCase("2")){
                    // doesn't want to download a file
                }
            }else if( option.equalsIgnoreCase("3") ){
                // user wants to upload a file
                System.out.println("1. Public\t     2.Private\t     3. Grant Request");
                // type : 1. Public file    2. Private file
                String type = sc.nextLine();

                send_file(type, out, in);

            }
            else if( option.equalsIgnoreCase("4") ){
                // user wants to request for a file
                System.out.println("Enter description for your request : ");
                String description = sc.nextLine();
                out.writeObject(description);
            }
            else if( option.equalsIgnoreCase("5") ){
                // user wants to see unread messages
                view_requests(in);
                // see updates on your request
                view_uploads(in);
            }
            else if( option.equalsIgnoreCase("6") ){
                // Lookup and download other students' files
                show_users(in);
                System.out.println("Mention the username :");
                // read the student id
                String id = sc.nextLine();
                out.writeObject(id);
                Object o = in.readObject();
                Integer check = 0;
                // check determines if a student with id exists. If doesn't exist, Server will send an Integer with value 0
                if( o.getClass().equals( check.getClass() ) ){
                    check = (Integer) o;
                    if( check == 0 ){
                        System.out.println(id + " is not in the user list");
                        // No such user with id so continue
                        continue;
                    }
                }
                // results contain all the public files list of user with id
                List<String> results = (List<String>) o;
                System.out.println(id + " - public files : ");
                for( int i = 0 ; i < results.size() ; i++ ){
                    System.out.println(results.get(i));
                }
                System.out.println("Do you want to download a file?\n1. Yes\t 2. No");
                String download = sc.nextLine();
                // If user wants to download a file  - 1. Yes   2. No
                out.writeObject(download);
                if(download.equalsIgnoreCase("1")){
                    // user wants to download a file
                    System.out.println("Mention the file id : ");
                    String filename = sc.nextLine();
                    receive_file(filename,in,out);
                }
                else if(download.equalsIgnoreCase("2")){
                    // user doesn't want to download a file
                    continue;
                }
            }
            else if( option.equalsIgnoreCase("7") ){
                // user wants to logout
                logout(in, out);
            }
        }
    }


}
