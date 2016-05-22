package dprs.response.dynamo;

public class DynamoReadResponse {

    String key;
    String vectorClock;
    String value;
    boolean successful;

    public DynamoReadResponse() {
        successful = false;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getVectorClock() {
        return this.vectorClock;
    }

    public void setVectorClock(String vectorClock) {
        this.vectorClock = vectorClock;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
