package dprs.response;

import dprs.entity.VectorClock;
import dprs.exceptions.ReadException;

import java.util.ArrayList;
import java.util.Objects;


public class DynamoReadResponse{

    String key;
    String vectorClock;
    String value;


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
