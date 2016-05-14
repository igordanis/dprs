package dprs.response;

public class TransportDataResponse {

    boolean successful;

    public TransportDataResponse(boolean successful) {
        this.successful = successful;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }
}
