package dprs.entity;

public class DatabaseEntry {
    Integer value;
    Object vectorClock;
    Integer maxBackups;
    Integer currentBackup;

    public DatabaseEntry(Integer value, Object vectorClock, Integer maxBackups, Integer currentBackup) {
        this.value = value;
        this.vectorClock = vectorClock;
        this.maxBackups = maxBackups;
        this.currentBackup = currentBackup;
    }

    public DatabaseEntry(DatabaseEntry source) {
        this.value = source.value;
        this.vectorClock = source.vectorClock;
        this.maxBackups = source.maxBackups;
        this.currentBackup = source.currentBackup;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public Object getVectorClock() {
        return vectorClock;
    }

    public void setVectorClock(Object vectorClock) {
        this.vectorClock = vectorClock;
    }

    public Integer getMaxBackups() {
        return maxBackups;
    }

    public void setMaxBackups(Integer maxBackups) {
        this.maxBackups = maxBackups;
    }

    public Integer getCurrentBackup() {
        return currentBackup;
    }

    public void setCurrentBackup(Integer currentBackup) {
        this.currentBackup = currentBackup;
    }

    @Override
    public String toString() {
        return "DatabaseEntry{" +
                "value=" + value +
                ", vectorClock=" + vectorClock +
                ", maxBackups=" + maxBackups +
                ", currentBackup=" + currentBackup +
                '}';
    }
}
