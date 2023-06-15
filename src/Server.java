import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Server {

    private static HashMap<String, SocketAddress> clients;
    private static HashMap<Integer, String> serverFiles;

    private static ArrayList<Request> requests = new ArrayList<Request>();
    private static int MIN_CHUNK_SIZE = 1000;
    private static int MAX_CHUNK_SIZE = 10000;
    private static long buffer = 0;
    private static long MAX_BUFFER_SIZE = 100000;

    private static int file_id=0;
    private static int request_id=0;


    public static boolean login(String username, SocketAddress clientIp) {
        if(clients.containsKey(username) == true){
            System.out.println(username + " is already logged in");
            return false;
        }
        clients.put(username, clientIp);
        System.out.println(username + " has logged in with IP Address : " + clientIp);

        String path = "ServerFiles/" + username + "/";
        File directory = new File(path);

        if (!directory.exists()) {
            directory.mkdir();
            File public_directory = new File( path + "public/");
            File private_dirrctory = new File( path + "private/");
            public_directory.mkdir();
            private_dirrctory.mkdir();
            System.out.println("Folder created with the name " + username + " in Server");
        }
        return true;
    }

    public static void logout(String student_id){
        clients.remove(student_id);
    }

    public static void addRequest(String student_id, String description){
        requests.add(new Request(request_id, student_id, description));
        request_id++;
    }
    public static void removeRequest(int request_id){
        for( int i = 0 ; i < requests.size() ; i++ ){
            if(requests.get(i).getRequest_id() == request_id){
                requests.remove(i);
                break;
            }
        }
    }
    public static ArrayList<Request> getRequests(){
        return requests;
    }

    public static ArrayList<String> get_active_users() {
        Set<String> keySet = clients.keySet();
        ArrayList<String> active_users = new ArrayList<String>(keySet);
        return active_users;
    }

    public static String[] get_users() {
        File directory = new File("ServerFiles");
        //List of all files and directories
        return directory.list();
    }

    public static Integer get_file_id(String filepath){
        for (Map.Entry<Integer, String> entry : serverFiles.entrySet()) {
            if (entry.getValue().equals(filepath)) {
                return entry.getKey();
            }
        }
        return 0;
    }

    public static String get_file_path(int file_id){
        return serverFiles.get(file_id);
    }


    public static int get_max_chunk_size(){
        return MAX_CHUNK_SIZE;
    }

    public static boolean checkBuffer(long filesize){
        if( buffer + filesize > MAX_BUFFER_SIZE ) return false;
        buffer += filesize;
        System.out.println("Buffer occupied : " + buffer + " bytes");
        return true;
    }

    public static void clearBuffer(long filesize){
        buffer -= filesize;
        System.out.println("Buffer occupied : " + buffer + " bytes");

    }

    public static int get_random_chunk_size(){
        return (int)(Math.random()*(MAX_CHUNK_SIZE - MIN_CHUNK_SIZE + 1) + MIN_CHUNK_SIZE);
    }

    public static int addFile(String filepath){
        file_id++;
        serverFiles.put(file_id, filepath);
        return file_id;
    }

    public static void addUpload( int req_id , String granter, String filepath){
        for( int i = 0 ; i < requests.size() ; i++ ){
            if(requests.get(i).getRequest_id() == req_id){
                requests.get(i).acceptRequest(granter, filepath);
                break;
            }
        }
    }

    public static boolean check_request_id(int request_id){
        for( int i = 0 ; i < requests.size() ; i++ ){
            if(requests.get(i).getRequest_id() == request_id){
                return true;
            }
        }
        return false;
    }
    public static void main(String[] args) throws IOException, ClassNotFoundException {

        clients = new HashMap<String, SocketAddress>();
        serverFiles = new HashMap<Integer, String>();

        ServerSocket welcomeSocket = new ServerSocket(3000);

        String path = "ServerFiles/";
        File directory = new File(path);
        directory.mkdir();

        while(true) {
            System.out.println("Waiting for connection...");
            Socket socket = welcomeSocket.accept();
            System.out.println("Connection established");

            // open thread
            Thread worker = new Worker(socket);
            worker.start();
        }

    }
}