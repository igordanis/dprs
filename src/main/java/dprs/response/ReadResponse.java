package dprs.response;

import dprs.exceptions.ReadException;
import java.util.ArrayList;


public class ReadResponse extends ResponseWithException {
    ArrayList values;
    boolean successful;
    
    public ReadResponse(ArrayList values, boolean successful) {
        this.values = values;
        this.successful = successful;
    }
    
    public ReadResponse(Exception exception) {
        super(exception);
        successful = false;
    }

    public ReadResponse(ReadException readException) {
        //super(exception);
        successful = false;
    }
    
    public ArrayList getValues() {
        return values;
    }
    
    public boolean isSuccessful() {
        return successful;
    }

    public void setValues(ArrayList values, boolean successful) {
        this.values = values;
        this.successful = successful;
    }
}
