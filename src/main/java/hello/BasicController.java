package hello;

import hello.struct.TestResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;

@RestController
public class BasicController {

    private static final Logger logger = LoggerFactory
            .getLogger(BasicController.class);
    private long counter = 0;

    @RequestMapping("/")
    public TestResponse respond() {

        logger.info("Responding to " + ++counter + " request");

        return new TestResponse(
                "127.0.0.1",
                "",
                "",
                new HashSet<>());
    }

}
