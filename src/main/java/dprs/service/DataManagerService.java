package dprs.service;

import com.google.gson.Gson;
import dprs.components.InMemoryDatabase;
import dprs.controller.dynamo.WriteController;
import dprs.entity.DatabaseEntry;
import dprs.entity.NodeAddress;
import dprs.response.dynamo.DynamoBulkWriteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.UUID.randomUUID;

@Service
public class DataManagerService {

    @Autowired
    ChordService chordService;

    @Value("${quorum.replication}")
    int replicationQuorum;

    private static final Logger logger = LoggerFactory.getLogger(DataManagerService.class);

    public void handleChangesInChord(Map<Integer, NodeAddress> oldChordAddresses,
                                     Map<Integer, NodeAddress> newChordAddresses
    ) {
        if (newChordAddresses.size() == 0 || oldChordAddresses.size() == 0) {
            return;
        }

        NodeAddress oldNextAddress = chordService.getAddressInMapByOffset(oldChordAddresses, 1);
        NodeAddress newNextAddress = chordService.getAddressInMapByOffset(newChordAddresses, 1);
        NodeAddress oldPreviousAddress = chordService.getAddressInMapByOffset(oldChordAddresses, -1);
        NodeAddress newPreviousAddress = chordService.getAddressInMapByOffset(newChordAddresses, -1);

        if (!newChordAddresses.containsKey(oldNextAddress.getHash())) {
            // Next node failed
            handleFailureOfNextNode(oldNextAddress, newChordAddresses, oldChordAddresses);
        } else if (!newNextAddress.equals(oldNextAddress)) {
            // New next node
            handleNewNextNode(newNextAddress, newChordAddresses);
        }

        if (!newChordAddresses.containsKey(oldPreviousAddress.getHash())) {
            // Previous node failed
            handleFailureOfPreviousNode(oldPreviousAddress, newPreviousAddress, newChordAddresses, oldChordAddresses);
        } else if (!newPreviousAddress.equals(oldPreviousAddress)) {
            // New previous node
            handleNewPreviousNode(newPreviousAddress, newChordAddresses);
        }
    }

    private void handleNewNextNode(NodeAddress newNode,
                                   Map<Integer, NodeAddress> newChordAddresses
    ) {
        logger.info("Found new next node: " + newNode);

        // Find all data that should be replicated to new node
        InMemoryDatabase database = InMemoryDatabase.INSTANCE;
        Map<String, DatabaseEntry> dataToSend = database.entrySet().stream().filter(entry ->
                chordService.findDestinationAddressesForKeyInList(newChordAddresses.values(), entry.getKey(), replicationQuorum)
                        .contains(newNode)
        ).collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()));

        final String transactionId = randomUUID().toString();
        sendBulkWriteToAddress(transactionId, newNode, dataToSend);
    }

    private void handleNewPreviousNode(NodeAddress newNode,
                                       Map<Integer, NodeAddress> newChordAddresses
    ) {
        logger.info("Found new previous node: " + newNode);

        // Find all data whose master should be the new node
        InMemoryDatabase database = InMemoryDatabase.INSTANCE;
        Map<String, DatabaseEntry> dataToSend = database.entrySet().stream().filter(entry ->
                chordService.findDestinationAddressesForKeyInList(newChordAddresses.values(), entry.getKey(), 1)
                        .contains(newNode)
        ).collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()));

        final String transactionId = randomUUID().toString();
        sendBulkWriteToAddress(transactionId, newNode, dataToSend);
    }

    private void handleFailureOfNextNode(NodeAddress failedNode,
                                         Map<Integer, NodeAddress> newChordAddresses,
                                         Map<Integer, NodeAddress> oldChordAddresses
    ) {
        logger.info("Next node failed: " + failedNode);

        // Find all data that used to be replicated on the next node
        InMemoryDatabase database = InMemoryDatabase.INSTANCE;
        Map<String, DatabaseEntry> dataFromFailedNode = database.entrySet().stream().filter(entry ->
                chordService.findDestinationAddressesForKeyInList(oldChordAddresses.values(), entry.getKey(), replicationQuorum)
                        .contains(failedNode)
        ).collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()));

        // Find data that should be replicated on each of the new nodes
        Map<NodeAddress, Map<String, DatabaseEntry>> dataToSend = getAllDataForAddresses(dataFromFailedNode, newChordAddresses);

        // Send data to all addresses
        final String transactionId = randomUUID().toString();
        for (NodeAddress address : dataToSend.keySet()) {
            sendBulkWriteToAddress(transactionId, address, dataToSend.get(address));
        }
    }

    private void handleFailureOfPreviousNode(NodeAddress failedNode,
                                             NodeAddress newPreviousNode,
                                             Map<Integer, NodeAddress> newChordAddresses,
                                             Map<Integer, NodeAddress> oldChordAddresses
    ) {
        logger.info("Previous node failed: " + failedNode);

        // Find all data whose master used to be the previous node
        InMemoryDatabase database = InMemoryDatabase.INSTANCE;
        Map<String, DatabaseEntry> dataFromFailedNode = database.entrySet().stream().filter(entry ->
                chordService.findDestinationAddressesForKeyInList(oldChordAddresses.values(), entry.getKey(), 1)
                        .contains(failedNode)
        ).collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()));

        // Divide the data into two parts: master is new previous node and master is current node
        Map<String, DatabaseEntry> dataForNewPreviousNode = dataFromFailedNode.entrySet().stream().filter(entry ->
                chordService.findDestinationAddressesForKeyInList(newChordAddresses.values(), entry.getKey(), 1)
                        .contains(newPreviousNode)
        ).collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()));

        NodeAddress myAddress = chordService.getSelfAddressInChord();
        Map<String, DatabaseEntry> dataForMe = dataFromFailedNode.entrySet().stream().filter(entry ->
                chordService.findDestinationAddressesForKeyInList(newChordAddresses.values(), entry.getKey(), 1)
                        .contains(myAddress)
        ).collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()));

        // Store data for each address in a map
        Map<NodeAddress, Map<String, DatabaseEntry>> dataToSend = getAllDataForAddresses(dataForMe, newChordAddresses);
        dataToSend.put(newPreviousNode, dataForNewPreviousNode);

        // Send data to all addresses
        final String transactionId = randomUUID().toString();
        for (NodeAddress address : dataToSend.keySet()) {
            sendBulkWriteToAddress(transactionId, address, dataToSend.get(address));
        }
    }

    private void sendBulkWriteToAddress(String transactionId,
                                        NodeAddress address,
                                        Map<String, DatabaseEntry> dataToSend) {
        // If there is no data to send, don't send a request
        if (dataToSend.size() == 0) {
            return;
        }

        String dataToSendJson = new Gson().toJson(dataToSend);

        // Send all data that should be replicated to new node
        URI destinationUri = UriComponentsBuilder
                .fromUriString("http://" + address.getFullAddress())
                .path(WriteController.DYNAMO_BULK_WRITE)
                .queryParam("data", dataToSendJson)
                .queryParam("transactionId", transactionId)
                .build()
                .toUri();

        logger.info(transactionId + ": Sending bulk write to " + destinationUri);
        new RestTemplate().getForObject(destinationUri, DynamoBulkWriteResponse.class);
    }

    private Map<NodeAddress, Map<String, DatabaseEntry>> getAllDataForAddresses(
            Map<String, DatabaseEntry> data,
            Map<Integer, NodeAddress> newChordAddresses
    ) {
        // Creates a map: key = address, value = data to send to this address

        Map<NodeAddress, Map<String, DatabaseEntry>> dataToSend = new HashMap<>();
        NodeAddress myAddress = chordService.getSelfAddressInChord();

        for (String key : data.keySet()) {
            List<NodeAddress> addressList = chordService.findDestinationAddressesForKeyInList(newChordAddresses.values(), key, replicationQuorum);

            for (NodeAddress address : addressList) {
                if (!dataToSend.containsKey(address)) {
                    dataToSend.put(address, new HashMap<>());
                }

                dataToSend.get(address).put(key, data.get(key));
            }
        }

        // Remove my address from map
        dataToSend.remove(myAddress);

        return dataToSend;
    }
}
