package dprs.controller;

import dprs.components.InMemoryDatabase;
import dprs.entity.NodeAddress;
import dprs.entity.VectorClock;
import dprs.response.ReadAllResponse;
import dprs.response.ReadResponse;
import dprs.service.BackupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.UUID;

import static java.util.UUID.randomUUID;

@EnableAutoConfiguration
@RestController
public class ReadController {
    private static final Logger logger = LoggerFactory.getLogger(ReadController.class);

    public static final String READ = "/read";
    public static final String DYNAMO_READ = "/dynamoRead";

    @Autowired
    BackupService backupService;


    @RequestMapping(READ)
    public ReadResponse read(
            @RequestParam(value = "key") String key,
            @RequestParam(value = "readQuorum", required = true) Integer readQuorum,
            @RequestParam(value = "vectorClock", required = false) VectorClock vectorClock
    ) {
        String transactionId = randomUUID().toString();
        return dynamoRead(key, readQuorum, vectorClock, transactionId);
    }



    @RequestMapping(DYNAMO_READ)
    public ReadResponse dynamoRead(
            @RequestParam(value = "key") String key,
            @RequestParam(value = "readQuorum", required = true) Integer readQuorum,
            @RequestParam(value = "vectorClock", required = false) VectorClock vectorClock,
            @RequestParam(value = "transactionId", required = true) String transactionId
    ) {

        NodeAddress destinationAdress = backupService.getAddressByHash(key.hashCode());

        if (!destinationAdress.getAddress().equals(backupService.getSelfAddresss().getAddress())) {
            logger.info("Trying to read from wrong partiton. Forwarding: " + transactionId);
            return forwardReadToProperTarget(key, readQuorum, vectorClock, transactionId);
        } else {
            logger.info("Trying to read from correct partiton. Processing: " + transactionId);
            return processReadRequest(key, readQuorum, vectorClock, transactionId);
        }
    }

    private ReadResponse forwardReadToProperTarget(String key, Integer readQuorum,
                                                   VectorClock vectorClock, String transactionId) {
        return null;
    };

    private ReadResponse processReadRequest(String key, Integer readQuorum, VectorClock vectorClock,
                                    String transactionId) {
        return null;
    };

}
