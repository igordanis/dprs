package dprs.controller.util;

import dprs.components.InMemoryDatabase;
import dprs.entity.DatabaseEntry;
import dprs.entity.NodeAddress;
import dprs.response.util.ReadAllFromAllResponse;
import dprs.service.ChordService;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@EnableAutoConfiguration
@RestController
public class UtilityController {
    private static final Logger logger = LoggerFactory.getLogger(UtilityController.class);

    public static final String CLEAR_DATA = "/clearData";
    public static final String READ_ALL_DATA = "/readAllData";
    public static final String READ_MY_DATA = "/readMyData";
    public static final String ALL_ADDRESSES = "/allAddresses";
    public static final String PRINT_CHORD_STATE = "/printChordState";

    @Autowired
    ChordService chordService;

    @RequestMapping(CLEAR_DATA)
    public void clearData(@RequestParam(value = "redirected", defaultValue = "false") boolean redirected) {
        InMemoryDatabase database = InMemoryDatabase.INSTANCE;
        final ConcurrentHashMap.KeySetView<String, DatabaseEntry> keySet = database.keySet();
        keySet.forEach(database::remove);
        if (!redirected) {
            chordService.getChordAddresses().values().stream()
                    .filter(address -> !address.equals(chordService.getSelfAddressInChord())).forEach(address -> {
                URI uri = UriComponentsBuilder.fromUriString("http://" + address.getIP() + ":8080")
                        .path(CLEAR_DATA)
                        .queryParam("redirected", true)
                        .build().toUri();
                new RestTemplate().delete(uri);
            });
        }
    }

    @RequestMapping(READ_ALL_DATA)
    public ReadAllFromAllResponse getAllData() {
        HashMap<String, Object> data = new HashMap<>();
        for (NodeAddress address : chordService.getChordAddresses().values()) {
            URI uri = UriComponentsBuilder.fromUriString("http://" + address.getFullAddress()).path(READ_MY_DATA).build().toUri();
            data.put(address.getFullAddress(), new RestTemplate().getForObject(uri, List.class));
        }
        return new ReadAllFromAllResponse(data);
    }

    @RequestMapping(ALL_ADDRESSES)
    public List<NodeAddress> getAllAddresses() {
        return new ArrayList<>(chordService.getChordAddresses().values());
    }

    @RequestMapping(READ_MY_DATA)
    public List<Object> getMyData() {
        InMemoryDatabase database = InMemoryDatabase.INSTANCE;
        return database.keySet().stream().map(key ->
                key + ":" + database.get(key))
                .collect(Collectors.toList());
    }

    @RequestMapping(PRINT_CHORD_STATE)
    public List<NodeAddress> printChordState() {
        // Prints the current positions of nodes in a chord (starting with my node).

        int count = chordService.getChordCount();
        List<NodeAddress> addressList = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            addressList.add(chordService.getAddressInChordByOffset(i));
        }
        return addressList;
    }
}
