package dprs.response;

import java.util.HashMap;

public class GetAddressRangesResponse {
    HashMap<String, int[]> data;

    public GetAddressRangesResponse(HashMap<String, int[]> data) {
        this.data = data;
    }

    public HashMap<String, int[]> getData() {
        return data;
    }

    public void setData(HashMap<String, int[]> data) {
        this.data = data;
    }
}
