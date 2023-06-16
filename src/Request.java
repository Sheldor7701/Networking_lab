import java.util.ArrayList;


public class Request {
    private int request_id;
    private String requester;
    private String description;
    private ArrayList<Pair> uploads;

    public Request(int request_id, String requester, String description){
        this.request_id = request_id;
        this.requester = requester;
        this.description = description;
        uploads = new ArrayList<Pair>();
    }

    public void acceptRequest(String granter, String filepath){
        Pair p = new Pair(granter, filepath);
        uploads.add(p);
    }

    public ArrayList<Pair> getuploads(){
        return uploads;
    }

    public int getRequest_id(){
        return request_id;
    }

    public String getRequester(){
        return requester;
    }

    public String getDescription(){
        return description;
    }

}


class Pair {
    private String uploader_id ;
    private String filepath;

    public Pair(String uploader_id, String filepath)
    {
        this.uploader_id = uploader_id;
        this.filepath = filepath;
    }

    public String getUploader_id() {
        return uploader_id;
    }

    public String getFilepath() {
        return filepath;
    }
}
