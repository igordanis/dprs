package dprs.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dprs.InMemoryDatabase;
import dprs.entity.DatabaseEntry;
import dprs.entity.NodeAddress;
import dprs.response.ReadResponse;
import dprs.response.SaveResponse;
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

import java.util.HashMap;

@EnableAutoConfiguration
@EnableDiscoveryClient
@RestController
public class ApiController {
    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    public static final String READ = "/read";
    public static final String SAVE = "/save";
    public static final String TRANSPORT_DATA = "/transportData";

    @Autowired
    BackupService backupService;
    @Value("${backup.max}")
    int defaultMaxBackups;

    @RequestMapping(READ)
    public ReadResponse readAll() {
        return new ReadResponse(new HashMap(InMemoryDatabase.INSTANCE));
    }

    @RequestMapping(SAVE)
    public SaveResponse saveValue(@RequestParam(value = "key") String key,
                                  @RequestParam(value = "value") int value,
                                  @RequestParam(value = "backup", defaultValue = "true") boolean backup,
                                  @RequestParam(value = "maxBackups", required = false) Integer maxBackups,
                                  @RequestParam(value = "currentBackup", required = false) Integer currentBackup) {
        currentBackup = currentBackup == null ? defaultMaxBackups : currentBackup;
        maxBackups = maxBackups == null ? defaultMaxBackups : maxBackups;
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

        return new SaveResponse(quorum);
    }

    @RequestMapping(TRANSPORT_DATA)
    public TransportDataResponse transportData(@RequestParam(value = "data") String data) {
        InMemoryDatabase database = InMemoryDatabase.INSTANCE;

        HashMap<String, DatabaseEntry> dataMap = new Gson()
                .fromJson(data, new TypeToken<HashMap<String, DatabaseEntry>>() {}.getType());

        database.putAll(dataMap);
        return new TransportDataResponse(true);
    }

    @Scheduled(fixedDelay = 5000)
    public void updateNodeAddresses() {
        backupService.updateNodeAddresses();
    }
}
