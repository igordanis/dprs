package dprs;

import dprs.domain.PersonDao;
import dprs.entity.Person;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ApiController {
    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    @Autowired
    PersonDao personDao;

    @RequestMapping("/testread")
    public List<Person> testRead() {
        return personDao.getAllPersons();
    }

    @RequestMapping("/testsave")
    public String testSave() {
        logger.info("Calling test save");
        return "Saved person with id: " + personDao.savePerson(new Person("Janko", "Hrasko"));
    }
}
