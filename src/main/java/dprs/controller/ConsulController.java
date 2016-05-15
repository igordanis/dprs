package dprs.controller;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.agent.model.Member;
import com.ecwid.consul.v1.catalog.model.CatalogService;
import dprs.components.InMemoryDatabase;
import dprs.entity.DatabaseEntry;
import dprs.entity.NodeAddress;
import dprs.response.AllDataResponse;
import dprs.response.GetAddressRangesResponse;
import dprs.response.HealthResponse;
import dprs.response.StatusResponse;
import dprs.service.BackupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;

@EnableAutoConfiguration
@EnableDiscoveryClient
@RestController
public class ConsulController {
    private static final Logger logger = LoggerFactory.getLogger(ConsulController.class);

    public static final String STATUS = "/";
    public static final String HEALTH = "/health";
    public static final String CLEAR_DATA = "/clearData";
    public static final String ADDRESS_RANGES = "/addressRanges";
    public static final String ALL_DATA = "/allData";
    public static final String MY_DATA = "/myData";

    @Autowired
    ConsulClient consulClient;

    @Autowired
    BackupService backupService;

    @Value("${spring.application.name}")
    String applicationName;

    @RequestMapping(HEALTH)
    public HealthResponse getHealth() {
        return new HealthResponse();
    }

    @RequestMapping(STATUS)
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

    @RequestMapping(ADDRESS_RANGES)
    public GetAddressRangesResponse getAddressRanges() {
        List<NodeAddress> addressList = backupService.getAllAddresses();
        List<int[]> rangeList = backupService.getAddressRangeList();

        HashMap<String, int[]> data = new HashMap<>();
        for (int i = 0; i < addressList.size(); i++) {
            data.put(addressList.get(i).getAddress(), rangeList.get(i));
        }

        return new GetAddressRangesResponse(data);
    }

    @RequestMapping(ALL_DATA)
    public AllDataResponse getAllData() {
        HashMap<String, Object> data = new HashMap<>();
        List<NodeAddress> addressList = backupService.getAllAddresses();

        for (NodeAddress address : addressList) {
            URI uri = UriComponentsBuilder.fromUriString("http://" + address.getAddress() + ":8080").path(MY_DATA).build().toUri();
            data.put(address.getAddress(), new RestTemplate().getForObject(uri, List.class));
        }

        return new AllDataResponse(data);
    }

    @RequestMapping(MY_DATA)
    public List<Object> getMyData() {
        InMemoryDatabase database = InMemoryDatabase.INSTANCE;
        List<Object> data = new ArrayList<>();
        for (Object key : database.keySet()) {
            data.add(key + ":" + database.get(key));
        }
        return data;
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

    @RequestMapping(CLEAR_DATA)
    public void clearData(@RequestParam(value = "first") boolean first) {
        InMemoryDatabase database = InMemoryDatabase.INSTANCE;
        final KeySetView<String, DatabaseEntry> keySet = database.keySet();
        keySet.forEach(database::remove);
        if (first) {
            for (NodeAddress address : backupService.getAllAddresses()) {
                if (!address.equals(backupService.getAddressSelf())) {
                    URI uri = UriComponentsBuilder.fromUriString("http://" + address.getAddress() + ":8080")
                            .path(CLEAR_DATA)
                            .queryParam("first", false)
                            .build().toUri();
                    new RestTemplate().delete(uri);
                }
            }
        }
    }
}
