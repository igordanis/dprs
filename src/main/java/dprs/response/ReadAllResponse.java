package dprs.response;

import java.util.HashMap;

public class ReadAllResponse extends ResponseWithException {

    HashMap values;

    public ReadAllResponse() {}

    public ReadAllResponse(Exception exception) {super(exception);}

    public ReadAllResponse(HashMap values) {
        this.values = values;
    }

    public HashMap getValues() {
        return values;
    }

    public void setValues(HashMap values) {
        this.values = values;
    }
}
