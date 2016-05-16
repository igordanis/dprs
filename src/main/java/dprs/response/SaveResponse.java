package dprs.response;

public class SaveResponse extends ResponseWithException {

    private boolean successful;
    private String vectorClock;

    public SaveResponse() {
    }

    public SaveResponse(Exception exception) {
        super(exception);
        successful = false;
    }

    public SaveResponse(boolean successful, String vectorClock) {
        this.successful = successful;
        this.vectorClock = vectorClock;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public String getVectorClock() {
        return vectorClock;
    }

    public void setVectorClock(String vectorClock) {
        this.vectorClock = vectorClock;
    }
}
