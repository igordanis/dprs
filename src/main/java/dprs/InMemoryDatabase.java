package dprs;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryDatabase extends ConcurrentHashMap {

    // eg. InMemoryDatabase map = InMemoryDatabase.INSTANCE;
    public static final InMemoryDatabase INSTANCE = new InMemoryDatabase();

    private InMemoryDatabase(){}
}
