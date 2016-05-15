package dprs.service;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.google.gson.Gson;
import dprs.InMemoryDatabase;
import dprs.controller.ApiController;
import dprs.entity.DatabaseEntry;
import dprs.entity.NodeAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BackupService {
    private static final Logger logger = LoggerFactory.getLogger(BackupService.class);

    private NodeAddress addressSelf = null;
    private List<NodeAddress> addressList = null;
    private List<int[]> addressRangeList = null;

    @Autowired
    ConsulClient consulClient;
    @Value("${spring.application.name}")
    String applicationName;

    public Object sendData(String address, String path, Map<String, Object> params) {
        logger.info("Sending data from " + addressSelf.getAddress() + " to " + address);

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("http://" + address + ":8080").path(path);

        for (String key : params.keySet()) {
            builder.queryParam(key, params.get(key));
        }
        URI targetUrl = builder.build().toUri();

        logger.info("URI: " + targetUrl);

        try {
            return new RestTemplate().getForObject(targetUrl, Object.class);
        } catch (Exception e) {
            logger.error("Failed to send data to " + address, e);
            return null;
        }
    }

    public Object transportData(int offset, Map<Object, DatabaseEntry> data) {
        Map<String, Object> params = new HashMap<>();
        params.put("data", new Gson().toJson(data));
        NodeAddress address = getAddressByOffset(offset);
        if (address != null) {
            return sendData(getAddressByOffset(offset).getAddress(), ApiController.TRANSPORT_DATA, params);
        } else {
            logger.error("Address was null. " + "Offset: " + offset);
            return null;
        }
    }

    public NodeAddress getAddressByOffset(int offset) {
        int index = -1;
        for (int i = 0; i < addressList.size(); i++) {
            if (addressList.get(i).getAddress().equals(addressSelf.getAddress())) {
                index = i;
                break;
            }
        }

        if (offset > addressList.size() || offset < -addressList.size() || index == -1) {
            return null;
        } else {
            index = (index + offset) % addressList.size();
            while (index < 0) {
                index += addressList.size();
            }
            return addressList.get(index);
        }
    }

    public List<NodeAddress> updateNodeAddresses() {
        List<NodeAddress> addressList = new ArrayList<>();

        if (addressSelf == null) {
            addressSelf = new NodeAddress(consulClient.getAgentSelf().getValue().getMember().getAddress());
        }

        addressList.addAll(
                consulClient.getHealthServices(applicationName, true, QueryParams.DEFAULT).getValue()
                        .stream().map(service ->
                        new NodeAddress(service.getNode().getAddress())
                ).sorted().collect(Collectors.toList())
        );

        if (this.addressList == null || this.addressList.isEmpty()) {
            this.addressList = addressList;
            updateAddressRanges();
        } else if (!addressList.equals(this.addressList)) {
            backupData(addressList);
        }

        return addressList;
    }

    public List<int[]> updateAddressRanges() {
        addressRangeList = new ArrayList<>(addressList.size());

        for (int i = 0; i < addressList.size(); i++) {
            int[] range = new int[2];
            if (i == addressList.size() - 1) {
                range[0] = Integer.MAX_VALUE;
            } else {
                range[0] = addressList.get(i + 1).getHash() + 1;
            }

            if (i == 0) {
                range[1] = Integer.MIN_VALUE;
            } else {
                range[1] = addressList.get(i).getHash();
            }
            addressRangeList.add(i, range);
        }

        return addressRangeList;
    }

    private void backupData(List<NodeAddress> updatedAddressList) {
        logger.info(addressSelf.getAddress() + ": Calling backup data.. " + addressList);
        NodeAddress previousNode = getAddressByOffset(-1);
        NodeAddress nextNode = getAddressByOffset(1);
        addressList = updatedAddressList;
        updateAddressRanges();
        logger.info("Changed addressList to " +addressList);

        // handle failure of next node or new next node
        if (nextNode != null && (!addressList.contains(nextNode)
                || (getAddressByOffset(1) != null && !getAddressByOffset(1).equals(nextNode)
        ))) {
            logger.info(addressSelf.getAddress() + ": Next node failed or new next node!");
            Map<Object, DatabaseEntry> data = databaseDeepCopy();

            for (int i = 1; data.size() > 0; i++) {
                increaseAllCurrentBackupValues(data, -1);
                data = data.entrySet().stream().filter(value ->
                        value.getValue().getCurrentBackup() > 0
                ).collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()));
                transportData(i, data);
            }
        }

        // handle failure of next node
        if (nextNode != null && !addressList.contains(nextNode)) {

        }

        // handle failure of previous node
        if (previousNode != null && !addressList.contains(previousNode)) {
            logger.info(addressSelf.getAddress() + ": Previous node failed!");
            Map<Object, DatabaseEntry> data = databaseDeepCopy();

            for (int i = 1; data.size() > 0; i++) {
                increaseAllCurrentBackupValues(data, 1);
                data = data.entrySet().stream().filter(value ->
                        value.getValue().getCurrentBackup() <= value.getValue().getMaxBackups()
                ).collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()));
                transportData(i, data);
            }
        }

        // handle new previous node
        if (previousNode != null && !getAddressByOffset(-1).equals(previousNode)) {
            logger.info(addressSelf.getAddress() + ": New previous node!");
        }
    }

    private Map<Object, DatabaseEntry> databaseDeepCopy() {
        InMemoryDatabase database = InMemoryDatabase.INSTANCE;
        Map<Object, DatabaseEntry> deepCopy = new HashMap<>();

        for (Object key : database.keySet()) {
            deepCopy.put(key, new DatabaseEntry((DatabaseEntry) database.get(key)));
        }
        return deepCopy;
    }

    private Map<Object, DatabaseEntry> increaseAllCurrentBackupValues(Map<Object, DatabaseEntry> data, int addedValue) {
        for (Object key : data.keySet()) {
            DatabaseEntry value = data.get(key);
            value.setCurrentBackup(value.getCurrentBackup() + addedValue);
        }
        return data;
    }

    public NodeAddress getAddressByHash(int hash) {
        for (int i = 0; i < addressRangeList.size(); i++) {
            int[] range = addressRangeList.get(i);
            if (hash >= range[1] && hash <= range[0]) {
                return addressList.get(i);
            }
        }
        return null;
    }

    public List<NodeAddress> getAllAddresses() {
        return addressList;
    }

    public List<int[]> getAddressRangeList() {
        return addressRangeList;
    }

    public NodeAddress getAddressSelf() {
        return addressSelf;
    }
}
