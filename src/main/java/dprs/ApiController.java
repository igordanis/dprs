package dprs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class ApiController {
    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    private static final String maxBackups = "3";

    @RequestMapping("/testread")
    public List<Integer> testRead() {
        InMemoryDatabase map = InMemoryDatabase.INSTANCE;
        List<Integer> values = new ArrayList<>();
        for (Object key : map.keySet()) {
            values.add((Integer) map.get(key));
        }
        return values;
    }

    @RequestMapping("/testsave")
    public void testsave(@RequestParam(value = "key") String key, @RequestParam(value = "value") int value) {
        InMemoryDatabase map = InMemoryDatabase.INSTANCE;
        map.put(key, value);
    }

}
