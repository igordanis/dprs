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
                                     @RequestParam(value = "redirected", defaultValue = "false") boolean redirected) {
        NodeAddress address = backupService.getAddressByHash(key.hashCode());

        if (!redirected && address != null && !address.getAddress().equals(backupService.getAddressSelf().getAddress())) {
            if (address == null) {
                return new SaveResponse(new WriteException(""));
            } else {
                // TODO update params vectorClock functionality
                URI uri = UriComponentsBuilder.fromUriString("http://" + address.getAddress() + ":8080").path(SAVE)
                        .queryParam("key", key)
                        .queryParam("value", value)
                        .build().toUri();
                logger.info("Redirecting to " + uri.toString());
                return new RestTemplate().getForObject(uri, SaveResponse.class);
            }
        } else {
            return saveValue(key, value);
        }

    }

    private SaveResponse saveValue(String key, int value) {
        logger.info("Saving " + key + ":" + value);

        int currentBackup = backupService.getCurrentBackup(key);
        logger.info("Current backup: " + currentBackup);
        int quorum = 1;
        InMemoryDatabase database = InMemoryDatabase.INSTANCE;

        DatabaseEntry entry = new DatabaseEntry(value, new VectorClock(), writeQuorum, currentBackup);
        database.put(key, entry);

        if (backupService.getAddressSelf().equals(backupService.getAddressByHash(key.hashCode()))) {
            HashMap<String, Object> params = new HashMap<>();
            params.put("key", key);
            params.put("value", value);
            params.put("redirected", true);

            for (int i = 1; i < writeQuorum; i++) {

                NodeAddress address = backupService.getAddressByOffset(i);
                if (address != null) {
                    if (backupService.sendData(address.getAddress(), SAVE, params) != null) {
                        quorum++;
                    }
                }
            }
        }

        return new SaveResponse(quorum >= writeQuorum);
    }
}
