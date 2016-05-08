package dprs;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.agent.model.Member;
import com.ecwid.consul.v1.catalog.model.CatalogService;
import dprs.struct.HealthResponse;
import dprs.struct.StatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.InetAddress;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

@EnableAutoConfiguration
@EnableDiscoveryClient
@RestController
public class ConsulController {
    private static final Logger logger = LoggerFactory.getLogger(ConsulController.class);

    @Autowired
    ConsulClient consulClient;

    @Value("${spring.application.name}")
    String applicationName;

    @RequestMapping("/health")
    public HealthResponse getHealth() {
        return new HealthResponse();
    }

    @RequestMapping("/")
    public StatusResponse getStatus() {
        logger.info("Requesting status from " + consulClient.getAgentSelf().getValue().getMember().getAddress());
        Map<String, String> serviceMap = new HashMap<>();
        for (CatalogService serviceInstance : consulClient.getCatalogService(applicationName, QueryParams.DEFAULT).getValue()) {
            serviceMap.put(serviceInstance.getAddress(), ping(serviceInstance.getAddress()));
        }

        Map<String, String> discoveryMap = new HashMap<>();
        for (Member discoveryInstance : consulClient.getAgentMembers().getValue()) {
            if ("consul".equals(discoveryInstance.getTags().get("role"))) {
                discoveryMap.put(discoveryInstance.getAddress(), ping(discoveryInstance.getAddress()));
            }
        }

        String identifier = consulClient.getAgentSelf().getValue().getMember().getName() + " "
                + consulClient.getAgentSelf().getValue().getMember().getAddress();

        return new StatusResponse(
                identifier,
                discoveryMap,
                serviceMap
        );
    }

    public String ping(String address) {
        try {
            InetAddress inet = InetAddress.getByName(address);
            long startTime = new GregorianCalendar().getTimeInMillis();
            if (inet.isReachable(2000)) {
                return (new GregorianCalendar().getTimeInMillis() - startTime) + "ms";
            }
        } catch (IOException e) {
            logger.error("Failed to ping " + address);
        }

        return "Not reachable.";
    }
}
