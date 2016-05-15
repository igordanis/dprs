package dprs.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dprs.components.InMemoryDatabase;
import dprs.entity.DatabaseEntry;
import dprs.entity.NodeAddress;
import dprs.exceptions.ReadException;
import dprs.exceptions.WriteException;
import dprs.response.ReadAllResponse;
import dprs.response.SaveResponse;
import dprs.response.ReadResponse;
import dprs.response.TransportDataResponse;
import dprs.service.BackupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;

@EnableAutoConfiguration
@EnableDiscoveryClient
@RestController
public class ApiController {
    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    public static final String READ_ALL = "/readAll";
    public static final String READ = "/readAll";
    public static final String SAVE = "/save";
    public static final String TRANSPORT_DATA = "/transportData";

    @Autowired
    BackupService backupService;
    @Value("${backup.max}")
    int defaultMaxBackups;
    @Value("${quorum.write}")
    int defaultWriteQuorum;

    @RequestMapping(READ_ALL)
    public ReadAllResponse readAll() {
        return new ReadAllResponse(new HashMap(InMemoryDatabase.INSTANCE));
    }
    
    @RequestMapping(READ)
    public ReadAllResponse read(@RequestParam(value = "key") String key,
            @RequestParam(value = "readQuorum", required = true) Integer readQuorum) {
        NodeAddress address = backupService.getAddressByHash(key.hashCode());
        if (address != null && !address.getAddress().equals(backupService.getAddressSelf().getAddress())) {
             if (address == null) {
                //return new ReadResponse(new ReadException(""));
             }
                else {
                // TODO update params vectorClock functionality
                /*URI uri = UriComponentsBuilder.fromUriString("http://" + address.getAddress() + ":8080").path(SAVE)
                        .queryParam("key", key)
                        .queryParam("value", value)
                        .queryParam("backup", backup)
                        .queryParam("maxBackups", maxBackups)
                        .queryParam("currentBackup", currentBackup)
                        .queryParam("writeQuorum", writeQuorum)
                        .build().toUri();
                logger.info("Redirecting to " + uri.toString());
                return new RestTemplate().getForObject(uri, ReadResponse.class);*/
                        }
        }
        return new ReadAllResponse(new HashMap(InMemoryDatabase.INSTANCE)); // TODO replace
    }

    @RequestMapping(SAVE)
    public SaveResponse saveValue(@RequestParam(value = "key") String key,
                                  @RequestParam(value = "value") int value,
                                  @RequestParam(value = "backup", defaultValue = "true") boolean backup,
                                  @RequestParam(value = "maxBackups", required = false) Integer maxBackups,
                                  @RequestParam(value = "currentBackup", required = false) Integer currentBackup,
                                  @RequestParam(value = "writeQuorum", required = false) Integer writeQuorum) {
        NodeAddress address = backupService.getAddressByHash(key.hashCode());

        if (backup && address != null && !address.getAddress().equals(backupService.getAddressSelf().getAddress())) {
            if (address == null) {
                return new SaveResponse(new WriteException(""));
            } else {
                // TODO update params vectorClock functionality
                URI uri = UriComponentsBuilder.fromUriString("http://" + address.getAddress() + ":8080").path(SAVE)
                        .queryParam("key", key)
                        .queryParam("value", value)
                        .queryParam("backup", backup)
                        .queryParam("maxBackups", maxBackups)
                        .queryParam("currentBackup", currentBackup)
                        .queryParam("writeQuorum", writeQuorum)
                        .build().toUri();
                logger.info("Redirecting to " + uri.toString());
                return new RestTemplate().getForObject(uri, SaveResponse.class);
            }
        } else {
            return saveValueRedirected(key, value, backup, maxBackups, currentBackup, writeQuorum);
        }

    }

    private SaveResponse saveValueRedirected(String key, int value, boolean backup, Integer maxBackups, Integer currentBackup, Integer writeQuorum) {
        logger.info("Saving " + key + ":" + value);

        currentBackup = currentBackup == null ? defaultMaxBackups : currentBackup;
        maxBackups = maxBackups == null ? defaultMaxBackups : maxBackups;
        writeQuorum = writeQuorum == null ? defaultWriteQuorum : writeQuorum;
        int quorum = 1;
        InMemoryDatabase database = InMemoryDatabase.INSTANCE;

        // TODO update vectorClock functionality
        String vectorClock = "default";

        DatabaseEntry entry = new DatabaseEntry(value, vectorClock, maxBackups, currentBackup);
        database.put(key, entry);

        if (backup) {
            HashMap<String, Object> params = new HashMap<>();
            params.put("key", key);
            params.put("value", value);
            params.put("backup", false);
            params.put("maxBackups", maxBackups);

            for (int i = 1; i < currentBackup; i++) {
                params.put("currentBackup", currentBackup - i);

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

    @RequestMapping(TRANSPORT_DATA)
    public TransportDataResponse transportData(@RequestParam(value = "data") String data) {
        InMemoryDatabase database = InMemoryDatabase.INSTANCE;

        HashMap<String, DatabaseEntry> dataMap = new Gson()
                .fromJson(data, new TypeToken<HashMap<String, DatabaseEntry>>() {
                }.getType());

        logger.info("Received data: " + data);
        database.putAll(dataMap);
        return new TransportDataResponse(true);
    }

    @Scheduled(fixedDelay = 5000)
    public void updateNodeAddresses() {
        backupService.updateNodeAddresses();
    }
}
