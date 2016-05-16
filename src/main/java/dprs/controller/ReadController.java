package dprs.controller;

import dprs.components.InMemoryDatabase;
import dprs.entity.DatabaseEntry;
import dprs.entity.NodeAddress;
import dprs.entity.VectorClock;
import dprs.response.DynamoReadResponse;
import dprs.response.ReadAllResponse;
import dprs.response.ReadResponse;
import dprs.response.SaveResponse;
import dprs.service.BackupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Node;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.UUID.randomUUID;

@EnableAutoConfiguration
@RestController
public class ReadController {
    private static final Logger logger = LoggerFactory.getLogger(ReadController.class);

    public static final String READ = "/read";
    public static final String DYNAMO_READ = "/dynamoRead";

    @Autowired
    BackupService backupService;


    @RequestMapping(ReadController.READ)
    public ReadResponse read(
            @RequestParam(value = "key") String key,
            @RequestParam(value = "readQuorum", required = true) Integer readQuorum,
            @RequestParam(value = "vectorClock", required = false) VectorClock vectorClock
    ) {
        String transactionId = randomUUID().toString();

        /*
         * Find part of chord which manages given key
         */
        Set<NodeAddress> set = new HashSet<NodeAddress>();
        NodeAddress firstDestinationAdress = this.backupService.getAddressByHash(key.hashCode());

        List<Optional<NodeAddress>> destinationAddresses = IntStream.rangeClosed(0, readQuorum)
                .mapToObj(i -> this.backupService.getAddressByOffset(firstDestinationAdress, i))
                .collect(Collectors.toList());

        /*
         * Ziska read odpovede od vsetkych dynamo uzlov ktore maju obsahovat dany objekt
         */
        Set<DynamoReadResponse> allResponses = destinationAddresses.stream()
                .map(destinationAddress -> {
                    URI destinationUri = UriComponentsBuilder
                            .fromUriString("http://" + destinationAddress + ":8080")
                            .path(ReadController.DYNAMO_READ)
                            .queryParam("key", key)
                            .queryParam("readQuorum", readQuorum)
                            .queryParam("transactionId", vectorClock)
                            .build()
                            .toUri();

                    ReadController.logger.info(transactionId, "Forwarding read request to: "
                            + destinationUri);

                    return new RestTemplate()
                            .getForObject(destinationUri, DynamoReadResponse.class);

                })
                .collect(Collectors.toSet());

        HashMap<VectorClock, DynamoReadResponse> uniqueValuesByVectorClock = new HashMap();
        allResponses.stream()
                .forEach(resp -> uniqueValuesByVectorClock.put(resp.getVectorClock(), resp));

        List<Object> uniqValues = uniqueValuesByVectorClock.values().stream()
                .map(uniqueResponse -> uniqueResponse.getValue())
                .collect(Collectors.toList());

        return new ReadResponse(uniqValues, true);
    }




    @RequestMapping(ReadController.DYNAMO_READ)
    public DynamoReadResponse dynamoRead(
            @RequestParam(value = "key") String key,
            @RequestParam(value = "readQuorum", required = true) Integer readQuorum,
            @RequestParam(value = "vectorClock", required = true) VectorClock vectorClock,
            @RequestParam(value = "transactionId", required = true) String transactionId
    ) {
        final DatabaseEntry databaseEntry = InMemoryDatabase.INSTANCE.get(key);
        final DynamoReadResponse dynamoReadResponse = new DynamoReadResponse();
        dynamoReadResponse.setKey(key);
        dynamoReadResponse.setValue(databaseEntry.getValue());
        dynamoReadResponse.setVectorClock(databaseEntry.getVectorClock());

        return dynamoReadResponse;
    }

}
