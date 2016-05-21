package dprs.controller.dynamo;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dprs.components.InMemoryDatabase;
import dprs.entity.DatabaseEntry;
import dprs.entity.NodeAddress;
import dprs.entity.VectorClock;
import dprs.response.dynamo.DynamoWriteResponse;
import dprs.service.ChordService;
import dprs.service.DataManagerService;
import dprs.response.dynamo.DynamoBulkWriteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.UUID.randomUUID;

//import dprs.wthrash.BackupService;

@EnableAutoConfiguration
@RestController
public class WriteController {
    private static final Logger logger = LoggerFactory.getLogger(WriteController.class);

    public static final String WRITE = "/write";
    public static final String DYNAMO_SINGLE_WRITE = "/dynamoWrite";
    public static final String DYNAMO_BULK_WRITE = "/bulkWrite";

    @Autowired
    ChordService chordService;

    @Autowired
    DataManagerService dataManagerService;

    @Value("${quorum.write}")
    int writeQuorum;

    @Value("${quorum.replication}")
    int replicationQuorum;

    @RequestMapping(WriteController.WRITE)
    public DynamoWriteResponse write(
            @RequestParam(value = "key") String key,
            @RequestParam(value = "value") String value,
            @RequestParam(value = "vectorClock", required = false) String receivedVectorClock,
            @RequestParam(value = "transactionId", required = false) String transactionId
    ) {
        final String receivedTransactionId = transactionId != null ? transactionId : randomUUID().toString();

        // Find part of chord which manages given key
        List<NodeAddress> destinationAddresses = chordService.findDestinationAddressesForKeyInChord(key, replicationQuorum);

        // If not coordinator for key, forward to it
        if (!destinationAddresses.get(0).equals(chordService.getSelfAddressInChord())) {
            URI destinationUri = UriComponentsBuilder
                    .fromUriString("http://" + destinationAddresses.get(0).getFullAddress())
                    .path(WRITE)
                    .queryParam("key", key)
                    .queryParam("value", value)
                    .queryParam("transactionId", receivedTransactionId)
                    .queryParam("vectorClock", receivedVectorClock)
                    .build()
                    .toUri();
            logger.info(receivedTransactionId + ": Forwarding write request to " + destinationUri);
            return new RestTemplate().getForObject(destinationUri, DynamoWriteResponse.class);
        }

        // If received vector clock is empty, create a new one.
        final String forwardedVectorClock;
        if (receivedVectorClock == null || receivedVectorClock.equals("")) {
            forwardedVectorClock = new VectorClock().toJSON();
        } else {
            forwardedVectorClock = receivedVectorClock;
        }

        // Forwards write request to all nodes and collects the returned vector clocks.
        Set<DynamoWriteResponse> allResponses = destinationAddresses.stream()
                .map(destinationAddress -> {
                    URI destinationUri = UriComponentsBuilder
                            .fromUriString("http://" + destinationAddress.getFullAddress())
                            .path(DYNAMO_SINGLE_WRITE)
                            .queryParam("key", key)
                            .queryParam("value", value)
                            .queryParam("transactionId", receivedTransactionId)
                            .queryParam("vectorClock", forwardedVectorClock)
                            .build()
                            .toUri();

                    logger.info(receivedTransactionId + ": Forwarding single write request to: " + destinationUri);

                    // If an exception is thrown while sending request to other node, it was unsuccessful
                    try {
                        return new RestTemplate().getForObject(destinationUri, DynamoWriteResponse.class);
                    } catch (Exception e) {
                        logger.error(receivedTransactionId + ": Failed to send single write request to " + destinationUri);
                        return null;
                    }
                })
                .collect(Collectors.toSet());

        Set<VectorClock> allVectorClocks = allResponses.stream().map(response
                -> VectorClock.fromJSON(response.getVectorClock())
        ).collect(Collectors.toSet());

        // Merge all collected vector clocks to one
        VectorClock mergedVectorClock = VectorClock.mergeToNewer(allVectorClocks);

        // Count successful updates
        long countUpdates = allResponses.stream().filter(response -> response.isUpdated()).count();
        boolean successful = countUpdates >= writeQuorum || countUpdates >= chordService.getChordCount();

        return new DynamoWriteResponse(successful, mergedVectorClock.toJSON());
    }

    @RequestMapping(WriteController.DYNAMO_SINGLE_WRITE)
    public DynamoWriteResponse dynamoSingleWrite(
            @RequestParam(value = "key") String key,
            @RequestParam(value = "value") String value,
            @RequestParam(value = "transactionId", required = true) String transactionId,
            @RequestParam(value = "vectorClock", required = true) String vectorClockJson
    ) {
        InMemoryDatabase database = InMemoryDatabase.INSTANCE;
        logger.info(transactionId + ": Received request for concrete write: " + key);

        final VectorClock vectorClock = VectorClock.fromJSON(vectorClockJson);
        final Integer selfIndexInChord = chordService.getSelfIndexInChord();
        boolean successful = true;

        if (database.containsKey(key)) {
            DatabaseEntry oldValue = database.get(key);

            if (vectorClock.isThisNewerThan(oldValue.getVectorClock())) {
                // Received vector clock is newer than existing. Combine them.
                vectorClock.addDisjunctiveValuesFrom(oldValue.getVectorClock());

                // If vector clock does not have value for current component, save it.
                if (!vectorClock.containsValueForComponent(selfIndexInChord)) {
                    vectorClock.setValueForComponent(selfIndexInChord, 1);
                }

                database.put(key, new DatabaseEntry(value, vectorClock));
            } else {
                // Received vector clock is older than existing
                successful = false;
            }
        } else {
            // When value is saved to this node for the first time, insert value for this node.
            if (!vectorClock.containsValueForComponent(selfIndexInChord)) {
                vectorClock.setValueForComponent(selfIndexInChord, 1);
            }
            database.put(key, new DatabaseEntry(value, vectorClock));
        }

        // Final merge of vector clocks (if this node has values that the sent vector clock doesnt have)
        vectorClock.addDisjunctiveValuesFrom(database.get(key).getVectorClock());

        logger.info(transactionId + ": Single write successful: " + successful);
        return new DynamoWriteResponse(successful, vectorClock.toJSON());
    }

    @RequestMapping(DYNAMO_BULK_WRITE)
    public DynamoBulkWriteResponse bulkWrite(
            @RequestParam(value = "data") String data,
            @RequestParam(value = "transactionId") String transactionId
    ) {
        logger.info(transactionId + ": Received bulk write with data: " + data);
        InMemoryDatabase database = InMemoryDatabase.INSTANCE;

        HashMap<String, DatabaseEntry> dataMap = new Gson()
                .fromJson(data, new TypeToken<HashMap<String, DatabaseEntry>>() {
                }.getType());

        // Store data that I don't have or is newer than my data
        dataMap.entrySet().stream().forEach(entry -> {
            DatabaseEntry value = entry.getValue();
            String key = entry.getKey();

            if (!database.containsKey(key) ||
                    value.getVectorClock().isThisNewerThan(database.get(key).getVectorClock())
                    ) {
                database.put(entry.getKey(), entry.getValue());
            }
        });

        return new DynamoBulkWriteResponse(true);
    }
}
