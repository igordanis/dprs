package dprs.wthrash;

public abstract class ResponseWithException {
    Exception exception;

    public ResponseWithException() {
    }

    public ResponseWithException(Exception exception) {
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }
}
