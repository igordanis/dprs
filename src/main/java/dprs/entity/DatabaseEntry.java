package dprs.entity;

public class DatabaseEntry {
    String value;
    VectorClock vectorClock;


    public DatabaseEntry(String value, VectorClock vectorClock) {
        this.value = value;
        this.vectorClock = vectorClock;
    }


    public DatabaseEntry(DatabaseEntry source) {
        this.value = source.value;
        this.vectorClock = source.vectorClock;
//        this.maxBackups = source.maxBackups;
//        this.currentBackup = source.currentBackup;
    }


    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }


    public VectorClock getVectorClock() {
        return vectorClock;
    }

    public void setVectorClock(VectorClock vectorClock) {
        this.vectorClock = vectorClock;
    }

    public Integer getMaxBackups() {
//        return maxBackups;
        return null;
    }

    public void setMaxBackups(Integer maxBackups) {
//        this.maxBackups = maxBackups;
    }

    public Integer getCurrentBackup() {
//        return currentBackup;
        return null;
    }

    public void setCurrentBackup(Integer currentBackup) {
//        this.currentBackup = currentBackup;
    }

    @Override
    public String toString() {
        return "DatabaseEntry{" +
                "value=" + value +
                ", vectorClock=" + vectorClock.toJSON() +
//                ", maxBackups=" + maxBackups +
//                ", currentBackup=" + currentBackup +
                '}';
    }

}
