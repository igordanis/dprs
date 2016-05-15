package dprs.response;

import java.util.HashMap;

public class AllDataResponse {
    private String data;

    public AllDataResponse(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
