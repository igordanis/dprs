package dprs.service;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.agent.model.Member;
import com.ecwid.consul.v1.catalog.model.CatalogService;
import com.google.gson.Gson;
import dprs.components.InMemoryDatabase;
import dprs.controller.TransportController;
import dprs.entity.DatabaseEntry;
import dprs.entity.NodeAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.*;
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
    @Value("${quorum.write}")
    int writeQuorum;

    public Object sendData(NodeAddress address, String path, Map<String, Object> params) {
        logger.info("Sending data from " + addressSelf.getAddress() + " to " + address);

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString("http://" + address.getAddress() + ":" + address.getPort())
                .path(path);

        for (String key : params.keySet()) {
            builder.queryParam(key, params.get(key));
        }
        URI targetUrl = builder.build().toUri();

        try {
            return new RestTemplate().getForObject(targetUrl, Object.class);
        } catch (Exception e) {
            logger.error("Failed to send data to " + address.getAddress() + ":" + address.getPort(), e);

            return null;
        }
    }

    public Object transportData(int offset, Map<Object, DatabaseEntry> data) {
        if (data.size() == 0) {
            return null;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("data", new Gson().toJson(data));
        NodeAddress address = getAddressByOffset(offset);
        if (address != null) {
            return sendData(getAddressByOffset(offset), TransportController.TRANSPORT_DATA, params);
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

    public Optional<NodeAddress> getAddressByOffset(NodeAddress nodeAddress, int offset) {

        final Optional<NodeAddress> first = addressList.stream()
                .filter(anyChordAddress -> anyChordAddress.getAddress()
                        .equals(nodeAddress.getAddress()))
                .findFirst();

        int providedNodeAddressIndex = addressList.indexOf(first.get());

        if (offset > addressList.size()
                || offset < -addressList.size()
                || providedNodeAddressIndex == -1) {
            return Optional.empty();
        } else {
            providedNodeAddressIndex =
                    (providedNodeAddressIndex + offset) % addressList.size();

            while (providedNodeAddressIndex < 0) {
                providedNodeAddressIndex += addressList.size();
            }

            return Optional.of(addressList.get(providedNodeAddressIndex));
        }
    }

    @Scheduled(fixedDelay = 5000)
    public void updateNodeAddresses() {
        List<NodeAddress> chordAddresses = new ArrayList<>();

        if (addressSelf == null) {
            Member consulMember = consulClient.getAgentSelf().getValue().getMember();
//            addressSelf = new NodeAddress("localhost",consulMember.getPort());
            addressSelf = new NodeAddress(consulMember.getAddress(), consulMember.getPort());
        }

        List<CatalogService> catalogServiceList = consulClient.getCatalogService(applicationName, QueryParams.DEFAULT).getValue();

        chordAddresses.addAll(catalogServiceList.stream().map(service ->
//      TODO mozno treba service.getAddress vymenit na "localhost" ked spustas na local
                        new NodeAddress(service.getAddress(), service.getServicePort())
//                        new NodeAddress("localhost", service.getServicePort())
        ).collect(Collectors.toList()));

        if (this.addressList == null || this.addressList.isEmpty()) {
            this.addressList = chordAddresses;
            updateAddressRanges();
        } else if (!chordAddresses.equals(this.addressList)) {
            backupData(chordAddresses);
        }
    }

    public List<int[]> updateAddressRanges() {
        addressRangeList = new ArrayList<>(addressList.size());

        for (int i = 0; i < addressList.size(); i++) {
            int[] range = new int[2];
            if (i == 0) {
                range[0] = Integer.MIN_VALUE;
            } else {
                range[0] = addressList.get(i - 1).getHash() + 1;
            }

            if (i == addressList.size() - 1) {
                range[1] = Integer.MAX_VALUE;
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

        // handle failure of next node
        if (nextNode != null && !addressList.contains(nextNode)) {
            logger.info(addressSelf.getAddress() + ": Next node failed");
            Map<Object, DatabaseEntry> data = databaseDeepCopy();

            // update currentBackups for following nodes
            for (int i = 1; data.size() > 0; i++) {
                increaseAllCurrentBackupValues(data, -1);
                data = data.entrySet().stream().filter(value ->
                        value.getValue().getCurrentBackup() > 0
                ).collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()));
                transportData(i, data);
            }
        }

        // handle failure of previous node
        if (previousNode != null && !addressList.contains(previousNode)) {
            logger.info(addressSelf.getAddress() + ": Previous node failed!");
            Map<Object, DatabaseEntry> data = databaseDeepCopy();

            // send all values that were stored on failed node to previous node (only first copies)
            increaseAllCurrentBackupValues(data, 1);
            data = data.entrySet().stream().filter(value ->
                    value.getValue().getCurrentBackup() == value.getValue().getMaxBackups()
            ).collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()));
            transportData(-1, data);
        }

        // handle new next node
        if (nextNode != null && !nextNode.equals(getAddressByOffset(1))) {
            logger.info(addressSelf.getAddress() + ": New next node!");
            Map<Object, DatabaseEntry> data = databaseDeepCopy();
            NodeAddress newNextNode = getAddressByOffset(1);

            // collect part of data that should be transported to new node
            Map<Object, DatabaseEntry> dataForNextNode = data.entrySet().stream().filter(value ->
                    getAddressByHash(value.getKey().hashCode()).getAddress().equals(newNextNode.getAddress())
                            && value.getValue().getCurrentBackup() == value.getValue().getMaxBackups()
            ).collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()));
            transportData(1, dataForNextNode);

            // update currentBackups for following nodes
            dataForNextNode.keySet().forEach(data::remove);
            for (int i = 1; data.size() > 0; i++) {
                increaseAllCurrentBackupValues(data, -1);
                data = data.entrySet().stream().filter(value ->
                        value.getValue().getCurrentBackup() > 0
                ).collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()));
                transportData(i, data);
            }
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
            if (hash >= range[0] && hash <= range[1]) {
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

    public NodeAddress getSelfAddresss() {
        return addressSelf;
    }

    public int getCurrentBackup(String key) {
        int keyAddress = addressList.indexOf(getAddressByHash(key.hashCode()));
        int myAddress = 0;

        for (NodeAddress address : addressList) {
            if (address.getAddress().equals(addressSelf.getAddress()) && address.getPort() == addressSelf.getPort()) {
                myAddress = addressList.indexOf(address);
            }
        }

        if (myAddress < keyAddress) {
            myAddress += addressList.size();
        }
        return myAddress - keyAddress > 0 ? writeQuorum + keyAddress - myAddress : writeQuorum;
    }

    public int getAddressSelfIndex() {
        return addressList.indexOf(addressSelf);
    }
}
