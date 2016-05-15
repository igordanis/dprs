package dprs.response;

public class SaveResponse extends ResponseWithException {

    private boolean successful;

    public SaveResponse() {
    }

    public SaveResponse(Exception exception) {
        super(exception);
        successful = false;
    }

    public SaveResponse(boolean successful) {
        this.successful = successful;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }
}
