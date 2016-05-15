package dprs.response;

public class SaveResponse extends ResponseWithException {

    private int writeQuorum;

    public SaveResponse() {}

    public SaveResponse(Exception exception) {super(exception);}

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
