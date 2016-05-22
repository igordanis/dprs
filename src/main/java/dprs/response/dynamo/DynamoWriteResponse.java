package dprs.response.dynamo;


public class DynamoWriteResponse {

    boolean successful = false;
    String vectorClock;

    public DynamoWriteResponse(){
    }

    public DynamoWriteResponse(boolean successful, String vectorClock){
        this.successful = successful;
        this.vectorClock = vectorClock;
    }

    public boolean isSuccessful() {
        return this.successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public String getVectorClock() {
        return this.vectorClock;
    }

    public void setVectorClock(String vectorClock) {
        this.vectorClock = vectorClock;
    }

}
