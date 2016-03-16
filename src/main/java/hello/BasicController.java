package hello;

import hello.struct.TestResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;

@RestController
public class BasicController {

    @RequestMapping("/")
    public TestResponse respond() {
        return new TestResponse(
                "127.0.0.1",
                "",
                "",
                new HashSet<>());
    }

}
