package hello.struct;

import java.util.Set;

/**
 * Created by igordanis on 16/03/16.
 */
public class TestResponse {

    public final String self;
    public final String serviceDiscovery;
    public final String logs;
    public final Set<String> nodes;

    public TestResponse(
            String self,
            String serviceDiscovery,
            String logs,
            Set<String> nodes
    ){
        this.self = self;
        this.serviceDiscovery = serviceDiscovery;
        this.logs = logs;
        this.nodes = nodes;
    }

}
