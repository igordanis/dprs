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

    public static final Logger logger = LoggerFactory.getLogger(DynamoClient.class);

    DockerClientConfig swarm_node_02_config = DockerClientConfig.createDefaultConfigBuilder()
            .withUri("https://192.168.99.102:2376")
            .withDockerCertPath("/Users/igordanis/.docker/machine/machines/swarm-node-02")
            .build();

    DockerClientConfig swarm_node_03_config = DockerClientConfig.createDefaultConfigBuilder()
            .withUri("https://192.168.99.103:2376")
            .withDockerCertPath("/Users/igordanis/.docker/machine/machines/swarm-node-03")
            .build();


    public DockerClient swarm_node_02_client = DockerClientBuilder.getInstance
            (swarm_node_02_config).build();

    public DockerClient swarm_node_03_client = DockerClientBuilder.getInstance
            (swarm_node_03_config).build();



    private List<Container> findDynamoContainersOnClient(DockerClient dockerClient){
        final List<Container> containersOnClient = dockerClient.listContainersCmd().exec();

        return containersOnClient.stream()
                .filter(container -> container.getNames()[0].contains("dynamo"))
                .collect(Collectors.toList());
    }

    @Scheduled(fixedDelay = 5000)
    public void scheduledRequest() {
        System.out.println("aaa");

        final List<Container> dynamoContainersOnNode2 = findDynamoContainersOnClient
                (swarm_node_02_client);

        final List<Container> dynamoContainersOnNode3 = findDynamoContainersOnClient
                (swarm_node_03_client);


        final Container containerToBeKilled = dynamoContainersOnNode3.get(0);
        logger.info("Killing container: {}", containerToBeKilled.getNames()[0]);
        swarm_node_03_client.killContainerCmd(containerToBeKilled.getId()).exec();


        System.out.print("");
    }

}
