package dprs.response.dynamo;

public class DynamoBulkWriteResponse {

    boolean successful;

    public DynamoBulkWriteResponse() {}

    public DynamoBulkWriteResponse(boolean successful) {
        this.successful = successful;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }
}
