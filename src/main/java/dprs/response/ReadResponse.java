package dprs.response;

import dprs.entity.DatabaseEntry;
import dprs.exceptions.ReadException;

import java.util.Collection;
import java.util.List;


public class ReadResponse extends ResponseWithException {
    private Collection<Object> values;
    private boolean successful;

    public ReadResponse() {}

    public ReadResponse(Collection<Object> values, boolean successful) {
        this.values = values;
        this.successful = successful;
    }

    public ReadResponse(Exception exception) {
        super(exception);
        successful = false;
    }

    public ReadResponse(ReadException readException) {
        super(readException);
        successful = false;
    }

    public Collection<Object> getValues() {
        return values;
    }
}
