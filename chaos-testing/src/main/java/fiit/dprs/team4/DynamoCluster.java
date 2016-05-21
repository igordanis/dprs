package fiit.dprs.team4;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import fiit.dprs.team4.utils.NamedDockerClient;
import org.javatuples.Pair;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
public class DynamoCluster {

    Set<NamedDockerClient> allMachines = new HashSet<>();

    public DynamoCluster(){
        DockerClientConfig swarm_node_02_config = DockerClientConfig.createDefaultConfigBuilder()
                .withUri("https://192.168.99.108:2376")
                .withDockerCertPath("/Users/igordanis/.docker/machine/machines/swarm-node-02")
                .build();

        DockerClientConfig swarm_node_03_config = DockerClientConfig.createDefaultConfigBuilder()
                .withUri("https://192.168.99.109:2376")
                .withDockerCertPath("/Users/igordanis/.docker/machine/machines/swarm-node-03")
                .build();

        DockerClient swarm_node_02_client = DockerClientBuilder.getInstance
                (swarm_node_02_config).build();

        DockerClient swarm_node_03_client = DockerClientBuilder.getInstance
                (swarm_node_03_config).build();


        allMachines.add(new NamedDockerClient(swarm_node_02_client, "swarm_node_02_client"));
        allMachines.add(new NamedDockerClient(swarm_node_03_client, "swarm_node_03_client"));
    }


    private List<Container> findDynamoContainersInClient(NamedDockerClient dockerClient){
        final List<Container> containersInClient = dockerClient.listContainersCmd().exec();

        return containersInClient.stream()
                .filter(container -> container.getNames()[0].contains("dynamo"))
                .collect(Collectors.toList());
    }


    public Set<Pair<NamedDockerClient, Container>> getAllDynamoContainersInAllNodes(){

        return allMachines.stream()
                .flatMap(dockerClient ->
                        findDynamoContainersInClient(dockerClient).stream()
                                .map(container -> new Pair<NamedDockerClient, Container>(dockerClient, container))
                )
                .collect(Collectors.toSet());
    }

}
