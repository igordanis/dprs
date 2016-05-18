package dprs.response.dynamo;

import dprs.entity.VectorClock;


public class DynamoWriteResponse {

    boolean updated = false;
    String vectorClock;

    public DynamoWriteResponse(){
    }

    public DynamoWriteResponse(boolean updated, String vectorClock){
        this.updated = updated;
        this.vectorClock = vectorClock;
    }

    public boolean isUpdated() {
        return this.updated;
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
    }


    public String getVectorClock() {
        return this.vectorClock;
    }

    public void setVectorClock(String vectorClock) {
        this.vectorClock = vectorClock;
    }


//    String key;
//    VectorClock vectorClock;
//    Object value;
//    public String getKey() {
//        return this.key;
//    }
//
//    public void setKey(String key) {
//        this.key = key;
//    }
//
//    public VectorClock getVectorClock() {
//        return this.vectorClock;
//    }
//
//    public void setVectorClock(VectorClock vectorClock) {
//        this.vectorClock = vectorClock;
//    }
//
//    public Object getValue() {
//        return this.value;
//    }
//
//    public void setValue(Object value) {
//        this.value = value;
//    }

}
