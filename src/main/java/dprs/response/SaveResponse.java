package dprs.response;

public class SaveResponse {

    private int writeQuorum;

    public SaveResponse() {}

    public SaveResponse(int writeQuorum) {
        this.writeQuorum = writeQuorum;
    }

    public int getWriteQuorum() {
        return writeQuorum;
    }

    public void setWriteQuorum(int writeQuorum) {
        this.writeQuorum = writeQuorum;
    }
}
