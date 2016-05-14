package dprs.response;

import java.util.Map;

public class StatusResponse {

    public final String self;
    public final Map<String, String> serviceDiscovery;
    public final Map<String, String> nodes;

    public StatusResponse(String self, Map<String, String> serviceDiscovery, Map<String, String> nodes) {
        this.self = self;
        this.serviceDiscovery = serviceDiscovery;
        this.nodes = nodes;
    }
}
