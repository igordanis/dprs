package dprs.response;

import dprs.entity.DatabaseEntry;
import dprs.exceptions.ReadException;

import java.util.Collection;
import java.util.List;


public class ReadResponse extends ResponseWithException {
    private Collection<DatabaseEntry> values;
    private boolean successful;

    public ReadResponse(Collection<DatabaseEntry> values, boolean successful) {
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

    public Collection<DatabaseEntry> getValues() {
        return values;
    }
}
