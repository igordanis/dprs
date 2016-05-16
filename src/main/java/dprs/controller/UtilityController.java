package dprs.controller;

import dprs.components.InMemoryDatabase;
import dprs.entity.DatabaseEntry;
import dprs.entity.NodeAddress;
import dprs.response.AllDataResponse;
import dprs.response.GetAddressRangesResponse;
import dprs.response.ReadAllResponse;
import dprs.service.BackupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@EnableAutoConfiguration
@RestController
public class UtilityController {
    private static final Logger logger = LoggerFactory.getLogger(UtilityController.class);

    public static final String CLEAR_DATA = "/clearData";
    public static final String ADDRESS_RANGES = "/addressRanges";
    public static final String ALL_DATA = "/allData";
    public static final String MY_DATA = "/myData";

    @Autowired
    BackupService backupService;

    @RequestMapping(CLEAR_DATA)
    public void clearData(@RequestParam(value = "redirected", defaultValue = "false") boolean redirected) {
        InMemoryDatabase database = InMemoryDatabase.INSTANCE;
        final ConcurrentHashMap.KeySetView<String, DatabaseEntry> keySet = database.keySet();
        keySet.forEach(database::remove);
        if (!redirected) {
            for (NodeAddress address : backupService.getAllAddresses()) {
                if (!address.equals(backupService.getSelfAddresss())) {
                    URI uri = UriComponentsBuilder.fromUriString("http://" + address.getAddress() + ":8080")
                            .path(CLEAR_DATA)
                            .queryParam("redirected", true)
                            .build().toUri();
                    new RestTemplate().delete(uri);
                }
            }
        }
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
        List<Object> data = database.keySet().stream().map(key -> key + ":" + database.get(key))
                .collect(Collectors.toList());
        return data;
    }


    public static final String READ_ALL = "/readAll";
    @RequestMapping(READ_ALL)
    public ReadAllResponse readAll() {
        return new ReadAllResponse(new HashMap(InMemoryDatabase.INSTANCE));
    }
}
