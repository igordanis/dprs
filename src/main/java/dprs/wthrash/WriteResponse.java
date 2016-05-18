package dprs.wthrash;

import dprs.entity.VectorClock;


public class WriteResponse {

    String vectorClock;
    String originalKey;

    public WriteResponse(VectorClock vectorClock, String key){
        originalKey = key;
        this.vectorClock = vectorClock.toJSON();
    }

    public String getVectorClock() {
        return this.vectorClock;
    }

    public void setVectorClock(String vectorClock) {
        this.vectorClock = vectorClock;
    }

    public String getOriginalKey() {
        return this.originalKey;
    }

    public void setOriginalKey(String originalKey) {
        this.originalKey = originalKey;
    }



}
