import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Request {
    private int request_id;
    private String requester;
    private String description;
    private Map<String, String> uploads;

    public Request(int request_id, String requester, String description) {
        this.request_id = request_id;
        this.requester = requester;
        this.description = description;
        uploads = new HashMap<>();
    }

    public void acceptRequest(String granter, String filepath) {
        uploads.put(granter, filepath);
    }

    public Map<String, String> getUploads() {
        return uploads;
    }

    public int getRequest_id() {
        return request_id;
    }

    public String getRequester() {
        return requester;
    }

    public String getDescription() {
        return description;
    }
}

