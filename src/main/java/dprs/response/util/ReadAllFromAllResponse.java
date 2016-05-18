package dprs.response.util;

import dprs.wthrash.ResponseWithException;

import java.util.HashMap;

public class ReadAllFromAllResponse extends ResponseWithException {

    HashMap values;

    public ReadAllFromAllResponse() {}

    public ReadAllFromAllResponse(Exception exception) {super(exception);}

    public ReadAllFromAllResponse(HashMap values) {
        this.values = values;
    }

    public HashMap getValues() {
        return values;
    }

    public void setValues(HashMap values) {
        this.values = values;
    }
}
