package dprs.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dprs.components.InMemoryDatabase;
import dprs.entity.DatabaseEntry;
import dprs.response.TransportDataResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

@EnableAutoConfiguration
@RestController
public class TransportController {
    private static final Logger logger = LoggerFactory.getLogger(TransportController.class);

    public static final String TRANSPORT_DATA = "/transportData";

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
}
