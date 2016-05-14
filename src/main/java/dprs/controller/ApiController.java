package dprs.controller;

import dprs.service.BackupService;
import dprs.InMemoryDatabase;
import dprs.entity.NodeAddress;
import dprs.response.ReadResponse;
import dprs.response.SaveResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

@EnableAutoConfiguration
@EnableDiscoveryClient
@RestController
public class ApiController {
    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    private static final String READ = "/read";
    private static final String SAVE = "/save";

    @Autowired
    BackupService backupService;
    @Value("${spring.application.name}")
    int writeQuorum;

    @RequestMapping(READ)
    public ReadResponse readAll() {
        return new ReadResponse(new HashMap(InMemoryDatabase.INSTANCE));
    }

    @RequestMapping(SAVE)
    public SaveResponse saveValue(@RequestParam(value = "key") String key,
                                  @RequestParam(value = "value") int value,
                                  @RequestParam(value = "backup", defaultValue = "true") boolean backup,
                                  @RequestParam(value = "writeQuorum", required = false) Integer writeQuorum) {
        writeQuorum = writeQuorum != null ? writeQuorum : this.writeQuorum;
        int quorum = 1;

        InMemoryDatabase database = InMemoryDatabase.INSTANCE;
        database.put(key, value);

        if (backup) {
            HashMap<String, Object> params = new HashMap<>();
            params.put("key", key);
            params.put("value", value);
            params.put("backup", false);

            backupService.updateNodeAddresses();

            for (int i = 1; i < writeQuorum; i++) {
                NodeAddress address = backupService.getNextAddress(i);
                if (address != null) {
                    if (backupService.sendData(address.getAddress(), SAVE, params) != null) {
                        quorum++;
                    }
                }
            }
        }

        return new SaveResponse(quorum);
    }
}
