package dprs.controller;

import dprs.components.InMemoryDatabase;
import dprs.entity.DatabaseEntry;
import dprs.entity.NodeAddress;
import dprs.exceptions.ReadException;
import dprs.response.ReadResponse;
import dprs.service.BackupService;
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

import static java.util.UUID.randomUUID;

@EnableAutoConfiguration
@RestController
public class ReadController {
    private static final Logger logger = LoggerFactory.getLogger(ReadController.class);

    public static final String READ = "/read";
    public static final String DYNAMO_READ = "/dynamoRead";

    @Autowired
    BackupService backupService;
    @Value("${quorum.read}")
    int readQuorum;

    @RequestMapping(READ)
    public ReadResponse read(
            @RequestParam(value = "key") String key,
            @RequestParam(value = "readQuorum", required = true) Integer readQuorum,
            @RequestParam(value = "redirected", defaultValue = "false") boolean redirected
    ) {
        String transactionId = randomUUID().toString();
        NodeAddress address = backupService.getAddressByHash(key.hashCode());

        if (!redirected && address != null && !address.getAddress().equals(backupService.getSelfAddresss().getAddress())) {
            if (address == null) {
                return new ReadResponse(new ReadException("Cannot find address for key"));
            } else {
                URI uri = UriComponentsBuilder.fromUriString("http://" + address.getAddress() + ":8080").path(READ)
                        .queryParam("key", key)
                        .queryParam("readQuorum", readQuorum)
                        .build().toUri();
                logger.info("Redirecting to " + uri.toString());
                return new RestTemplate().getForObject(uri, ReadResponse.class);
            }
        } else {
            return readValue(key, readQuorum, redirected);
        }
    }

    private ReadResponse readValue(String key, Integer readQuorum, boolean redirected) {
        InMemoryDatabase database = InMemoryDatabase.INSTANCE;
        readQuorum = readQuorum != null ? readQuorum : this.readQuorum;

        Set<Object> values = new HashSet<>();
        int quorum = 0;

        if (database.containsKey(key)) {
            quorum++;
            values.add(database.get(key).getValue());
        }

        if (!redirected) {
            HashMap<String, Object> params = new HashMap<>();
            params.put("key", key);
            params.put("value", readQuorum);
            params.put("redirected", true);

            for (int i = 1; i < readQuorum; i++) {

                NodeAddress address = backupService.getAddressByOffset(i);
                if (address != null) {
                    try {
                        ReadResponse response = (ReadResponse) backupService.sendData(address, READ, params);
                        values.addAll(response.getValues());
                    } catch (Exception e) {
                        logger.error("Failed to send read data to " + address);
                    }
                }
            }
        }

        return new ReadResponse(values, quorum >= readQuorum);
    }

}
