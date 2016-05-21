package fiit.dprs.team4;


import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by igordanis on 19/05/16.
 */
@Service
public class DynamoClient {


//    @Scheduled(fixedDelay = 5000)
    public void scheduledRequest() {
    }

}
