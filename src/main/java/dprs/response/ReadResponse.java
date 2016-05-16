package dprs.response;

import dprs.exceptions.ReadException;
import java.util.ArrayList;
import java.util.List;


public class ReadResponse extends ResponseWithException {
    private List values;
    private boolean successful;
    
    public ReadResponse(List values, boolean successful) {
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
    
    public List getValues() {
        return values;
    }

}
