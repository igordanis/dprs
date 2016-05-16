package dprs.response;

/**
 * Created by igordanis on 16/05/16.
 */
public class Tuple {

    String value;
    String vectorClock;

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getVectorClock() {
        return this.vectorClock;
    }

    public void setVectorClock(String vectorClock) {
        this.vectorClock = vectorClock;
    }
}
