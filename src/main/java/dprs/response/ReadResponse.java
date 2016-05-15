package dprs.response;

import java.util.HashMap;

public class ReadResponse extends ResponseWithException {

    HashMap values;

    public ReadResponse() {}

    public ReadResponse(Exception exception) {super(exception);}

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
