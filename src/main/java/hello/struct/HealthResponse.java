package hello.struct;

public class HealthResponse {
    boolean status = true;

    public HealthResponse() {
    }

    public HealthResponse(boolean status) {
        this.status = status;
    }

    public boolean getStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }
}
