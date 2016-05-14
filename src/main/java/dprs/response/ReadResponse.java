package dprs.response;

import java.util.HashMap;

public class ReadResponse {

    HashMap values;

    public ReadResponse(HashMap values) {
        this.values = values;
    }

    public HashMap getValues() {
        return values;
    }

    public void setValues(HashMap values) {
        this.values = values;
    }
}
