package dprs.controller;

import dprs.components.InMemoryDatabase;
import dprs.entity.DatabaseEntry;
import dprs.entity.NodeAddress;
import dprs.entity.VectorClock;
import dprs.exceptions.WriteException;
import dprs.response.SaveResponse;
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
import java.util.HashMap;

@EnableAutoConfiguration
@RestController
public class WriteController {
    private static final Logger logger = LoggerFactory.getLogger(WriteController.class);

    public static final String SAVE = "/save";

    @Autowired
    BackupService backupService;

    @Value("${quorum.write}")
    int writeQuorum;

    @RequestMapping(SAVE)
    public SaveResponse redirectSave(@RequestParam(value = "key") String key,
                                     @RequestParam(value = "value") int value,
                                     @RequestParam(value = "vectorClock", required = false) String vectorClockJson,
                                     @RequestParam(value = "redirected", defaultValue = "false") boolean redirected) throws WriteException {
        NodeAddress address = backupService.getAddressByHash(key.hashCode());

        if (!redirected && address != null && !address.getAddress().equals(backupService.getSelfAddresss().getAddress())) {
            if (address == null) {
                throw new WriteException("Unknown target node for key.");
            } else {
                URI uri = UriComponentsBuilder.fromUriString("http://" + address.getAddress() + ":" + address.getPort()).path(SAVE)
                        .queryParam("key", key)
                        .queryParam("value", value)
                        .queryParam("vectorClock", vectorClockJson)
                        .build().toUri();
                logger.info("Redirecting to " + uri.toString());
                return new RestTemplate().getForObject(uri, SaveResponse.class);
            }
        } else {
            return saveValue(key, value, vectorClockJson, redirected);
        }

    }

    private SaveResponse saveValue(String key, int value, String vectorClockJson, boolean redirected) throws WriteException {
        logger.info("Saving " + key + ":" + value);
        InMemoryDatabase database = InMemoryDatabase.INSTANCE;

        int currentBackup = backupService.getCurrentBackup(key);
        int quorum = 0;

        VectorClock vectorClock = VectorClock.fromJSON(vectorClockJson);
        if (vectorClock == null) {
            vectorClock = VectorClock.fromAddressList(backupService.getAllAddresses());
        }

        int index = backupService.getAddressSelfIndex();
        vectorClock.incrementValueForComponent(index);
        DatabaseEntry entry = new DatabaseEntry(value, vectorClock, writeQuorum, currentBackup);

        if (database.get(key) != null && !vectorClock.isThisNewerThan(database.get(key).getVectorClock(), index)) {
            if (!backupService.getSelfAddresss().equals(backupService.getAddressByHash(key.hashCode()))) {
                throw new WriteException("A newer version already exists: " + database.get(key));
            }
        } else {
            quorum++;
            database.put(key, entry);
        }

        if (!redirected) {
            HashMap<String, Object> params = new HashMap<>();
            params.put("key", key);
            params.put("value", value);
            params.put("vectorClock", vectorClock.toJSON());
            params.put("redirected", true);

            for (int i = 1; i < writeQuorum; i++) {

                NodeAddress address = backupService.getAddressByOffset(i);
                if (address != null && !address.equals(backupService.getSelfAddresss())) {
                    try {
                        backupService.sendData(address, SAVE, params);
                        quorum++;
                    } catch (Exception e) {
                        logger.error("Failed to send write data to " + address);
                    }
                }
            }
        }

        return new SaveResponse(quorum >= writeQuorum, vectorClock.toJSON());
    }



}
