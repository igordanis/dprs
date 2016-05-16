package dprs.components;

import dprs.entity.DatabaseEntry;

import java.util.concurrent.ConcurrentHashMap;

public class InMemoryDatabase extends ConcurrentHashMap<String, DatabaseEntry> {

    // eg. InMemoryDatabase map = InMemoryDatabase.INSTANCE;
    public static final InMemoryDatabase INSTANCE = new InMemoryDatabase();

    private InMemoryDatabase(){}
}
