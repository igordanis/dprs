package dprs.response;

import dprs.entity.VectorClock;
import dprs.exceptions.ReadException;

import java.util.ArrayList;
import java.util.Objects;


public class DynamoReadResponse{

    String key;
    VectorClock vectorClock;
    Object value;


    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public VectorClock getVectorClock() {
        return this.vectorClock;
    }

    public void setVectorClock(VectorClock vectorClock) {
        this.vectorClock = vectorClock;
    }

    public Object getValue() {
        return this.value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

}
