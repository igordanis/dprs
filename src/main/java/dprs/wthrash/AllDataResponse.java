package dprs.wthrash;

import java.util.HashMap;

public class AllDataResponse {
    private HashMap<String, Object> data;

    public AllDataResponse(HashMap<String, Object> data) {
        this.data = data;
    }

    public HashMap<String, Object> getData() {
        return data;
    }

    public void setData(HashMap<String, Object> data) {
        this.data = data;
    }
}
