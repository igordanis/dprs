package dprs.controller;

import dprs.components.InMemoryDatabase;
import dprs.response.ReadAllResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

@EnableAutoConfiguration
@RestController
public class ReadController {
    private static final Logger logger = LoggerFactory.getLogger(ReadController.class);

    public static final String READ_ALL = "/readAll";

    @RequestMapping(READ_ALL)
    public ReadAllResponse readAll() {
        return new ReadAllResponse(new HashMap(InMemoryDatabase.INSTANCE));
    }

}
