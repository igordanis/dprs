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

    public void handleChangesInChord(
            Map<Integer, NodeAddress> oldChordAddresses,
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
            logger.info("Next node failed: " + oldNextAddress);
            handleFailureOfNeighborNode(oldNextAddress, newChordAddresses, oldChordAddresses);
        } else if (!newNextAddress.equals(oldNextAddress)) {
            logger.info("Found new next node: " + newNextAddress);
            handleNewNeighborNode(newNextAddress, newChordAddresses, oldChordAddresses);
        }

        if (!newChordAddresses.containsKey(oldPreviousAddress.getHash())) {
            logger.info("Previous node failed: " + oldPreviousAddress);
            handleFailureOfNeighborNode(oldPreviousAddress, newChordAddresses, oldChordAddresses);
        } else if (!newPreviousAddress.equals(oldPreviousAddress)) {
            logger.info("Found new previous node: " + newPreviousAddress);
            handleNewNeighborNode(newPreviousAddress, newChordAddresses, oldChordAddresses);
        }
    }

    /**
     * Finds data that should be replicated on the new node and replicates it.
     */
    private void handleNewNeighborNode(
            NodeAddress newNode,
            Map<Integer, NodeAddress> newChordAddresses,
            Map<Integer, NodeAddress> oldChordAddresses
    ) {
        // Find all data that should be replicated to new node
        InMemoryDatabase database = InMemoryDatabase.INSTANCE;
        Map<String, DatabaseEntry> dataForNewNode = database.entrySet().stream().filter(entry ->
                chordService.findDestinationAddressesForKeyInList(newChordAddresses.values(), entry.getKey(), replicationQuorum)
                        .contains(newNode)
        ).collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()));

        filterAndSendData(dataForNewNode, newChordAddresses, oldChordAddresses);
    }

    /**
     * Finds data that used to be replicated on the failed node and re-replicates it
     */
    private void handleFailureOfNeighborNode(NodeAddress failedNode,
                                             Map<Integer, NodeAddress> newChordAddresses,
                                             Map<Integer, NodeAddress> oldChordAddresses
    ) {
        // Find all data that used to be replicated on the failed node
        InMemoryDatabase database = InMemoryDatabase.INSTANCE;
        Map<String, DatabaseEntry> dataFromFailedNode = database.entrySet().stream().filter(entry ->
                chordService.findDestinationAddressesForKeyInList(oldChordAddresses.values(), entry.getKey(), replicationQuorum)
                        .contains(failedNode)
        ).collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()));

        filterAndSendData(dataFromFailedNode, newChordAddresses, oldChordAddresses);
    }

    /**
     * Creates a list of data that should be sent and sends it
     */
    private void filterAndSendData(
            Map<String, DatabaseEntry> data,
            Map<Integer, NodeAddress> newChordAddresses,
            Map<Integer, NodeAddress> oldChordAddresses
    ) {
        // Find data that should be replicated
        Map<NodeAddress, Map<String, DatabaseEntry>> dataToSend = getAllDataForAddresses(data, newChordAddresses, oldChordAddresses);

        // Send data to all addresses
        final String transactionId = randomUUID().toString();
        for (NodeAddress address : dataToSend.keySet()) {
            sendBulkWriteToAddress(transactionId, address, dataToSend.get(address));
        }
    }

    /**
     * Transports all data to an address
     */
    private void sendBulkWriteToAddress(
            String transactionId,
            NodeAddress address,
            Map<String, DatabaseEntry> dataToSend
    ) {
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
        try {
            new RestTemplate().getForObject(destinationUri, DynamoBulkWriteResponse.class);
        } catch (Exception e) {
            logger.error(transactionId + ": Failed to send bulk write to " + destinationUri);
        }
    }

    /**
     * Creates a map: key = address, value = data to send to this address
     */
    private Map<NodeAddress, Map<String, DatabaseEntry>> getAllDataForAddresses(
            Map<String, DatabaseEntry> data,
            Map<Integer, NodeAddress> newChordAddresses,
            Map<Integer, NodeAddress> oldChordAddresses
    ) {
        Map<NodeAddress, Map<String, DatabaseEntry>> dataToSend = new HashMap<>();

        for (String key : data.keySet()) {
            List<NodeAddress> newAddressList = chordService.findDestinationAddressesForKeyInList(newChordAddresses.values(), key, replicationQuorum);
            List<NodeAddress> oldAddressList = chordService.findDestinationAddressesForKeyInList(oldChordAddresses.values(), key, replicationQuorum);

            // Remove nodes that already contain the data
            newAddressList.removeAll(oldAddressList);

            for (NodeAddress address : newAddressList) {
                if (!dataToSend.containsKey(address)) {
                    dataToSend.put(address, new HashMap<>());
                }

                dataToSend.get(address).put(key, data.get(key));
            }
        }

        return dataToSend;
    }
}
