package dprs.controller.dynamo;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dprs.components.InMemoryDatabase;
import dprs.entity.DatabaseEntry;
import dprs.entity.NodeAddress;
import dprs.entity.VectorClock;
import dprs.response.dynamo.DynamoWriteResponse;
import dprs.wthrash.WriteResponse;
//import dprs.wthrash.BackupService;
import dprs.service.ChordService;
import dprs.wthrash.TransportDataResponse;
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
import java.util.*;
import java.util.stream.Collectors;

import static java.util.UUID.randomUUID;

@EnableAutoConfiguration
@RestController
public class WriteController {
    private static final Logger logger = LoggerFactory.getLogger(WriteController.class);

    public static final String WRITE = "/write";
    public static final String DYNAMO_SINGLE_WRITE = "/dynamoWrite";
    public static final String DYNAMO_BULK_WRITE = "/bulkWrite";

    @Autowired
    ChordService chordService;

//    BackupService backupService;

    @Value("${quorum.write}")
    int writeQuorum;


    @RequestMapping(WriteController.WRITE)
    public WriteResponse write(
            @RequestParam(value = "key") String key,
            @RequestParam(value = "value") String value,
            @RequestParam(value = "vectorClock", required = false) String receivedVectorClock
    ) {
        String transactionId = randomUUID().toString();
        VectorClock deserializedClock = VectorClock.fromJSON(receivedVectorClock);

        /*
         * Find part of chord which manages given key
         */
        List<NodeAddress> destinationAddresses = chordService.findDestinationAdressesForKey(
                key, writeQuorum);



        /*
         * Ziska write odpovede od vsetkych dynamo uzlov ktore maju zapisat objekt
         */

        Set<DynamoWriteResponse> allResponses = destinationAddresses.stream()
                .map(destinationAddress -> {

                    URI destinationUri = UriComponentsBuilder
                            .fromUriString("http://" + destinationAddress.getIP() + ":" +
                                    destinationAddress.getPort())
                            .path(DYNAMO_SINGLE_WRITE)
                            .queryParam("key", key)
                            .queryParam("value", value)
                            .queryParam("transactionId", transactionId)
                            .queryParam("vectorClock", deserializedClock.toJSON())
                            .build()
                            .toUri();

                    logger.info(transactionId, "Forwarding write request to: "
                            + destinationUri);

                    return new RestTemplate()
                            .getForObject(destinationUri, DynamoWriteResponse.class);

                })
                .collect(Collectors.toSet());


        //ak obsahuje kolekcia konkurentne vectorclocky, je nutne vratit
        //mnozinu najnovsich vectorclockov ktore su konkurentne
        //inak je nutne vratit jeden najnovsi vectorclock
        //TODO: implement


        return new WriteResponse(null, key);
    }


    @RequestMapping(WriteController.DYNAMO_SINGLE_WRITE)
    public DynamoWriteResponse dynamoSingleWrite(
            @RequestParam(value = "key") String key,
            @RequestParam(value = "value") String value,
            @RequestParam(value = "transactionId", required = true) String transactionId,
            @RequestParam(value = "vectorClock", required = true) String vc
    ) {
        logger.info(transactionId + ": Received request for concrete write: " + key);

        final VectorClock vectorClock = VectorClock.fromJSON(vc);

        final Integer selfIndexInChord = chordService.getSelfIndexInChord();

        InMemoryDatabase.INSTANCE.computeIfPresent(key, (k, oldVal) -> {

            if (vectorClock.isThisNewerThan(oldVal.getVectorClock(), selfIndexInChord)) {
                logger.info("New value has been updated");
                vectorClock.incrementValueForComponent(selfIndexInChord);
                return new DatabaseEntry(value, vectorClock);
            } else {
                logger.info("Old value has been kept");
                return oldVal;
            }
        });

        InMemoryDatabase.INSTANCE.computeIfAbsent(key, k  -> {
            final VectorClock vectorClock1 = VectorClock.fromJSON(vc);
            vectorClock1.incrementValueForComponent(selfIndexInChord);
            return new DatabaseEntry(value,vectorClock1);
        });


        return new DynamoWriteResponse(true, vectorClock.toJSON());
    }

    @RequestMapping(DYNAMO_BULK_WRITE)
    public TransportDataResponse bulkWrite(@RequestParam(value = "data") String data) {

        InMemoryDatabase database = InMemoryDatabase.INSTANCE;

        HashMap<String, DatabaseEntry> dataMap = new Gson()
                .fromJson(data, new TypeToken<HashMap<String, DatabaseEntry>>() {
                }.getType());

        logger.info("Received data: " + data);
        database.putAll(dataMap);
        return new TransportDataResponse(true);
    }

}
