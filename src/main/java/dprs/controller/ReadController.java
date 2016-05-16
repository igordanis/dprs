package dprs.controller;

import dprs.components.InMemoryDatabase;
import dprs.entity.NodeAddress;
import dprs.response.ReadAllResponse;
import dprs.service.BackupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

@EnableAutoConfiguration
@RestController
public class ReadController {
    private static final Logger logger = LoggerFactory.getLogger(ReadController.class);

    public static final String READ_ALL = "/readAll";
    public static final String READ = "/read";

    @Autowired
    BackupService backupService;

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
}
