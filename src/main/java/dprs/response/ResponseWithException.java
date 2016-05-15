package dprs.response;

public abstract class ResponseWithException {
    private Exception exception;

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
