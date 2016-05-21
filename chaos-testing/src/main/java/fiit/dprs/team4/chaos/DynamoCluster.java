package fiit.dprs.team4.chaos;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import fiit.dprs.team4.chaos.utils.NamedDockerClient;
import org.javatuples.Pair;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
public class DynamoCluster implements InitializingBean{

    Set<NamedDockerClient> allMachines = new HashSet<>();

    @Value("${swarm-node-02.ip}")
    private String node2ip;

    @Value("${swarm-node-02.cert}")
    private String node2certPath;

    @Value("${swarm-node-03.ip}")
    private String node3ip;

    @Value("${swarm-node-03.cert}")
    private String node3certPath;



    @Override
    public void afterPropertiesSet() throws Exception {
        DockerClientConfig swarm_node_02_config = DockerClientConfig.createDefaultConfigBuilder()
                .withUri(node2ip)
                .withDockerCertPath(node2certPath)
                .build();

        DockerClientConfig swarm_node_03_config = DockerClientConfig.createDefaultConfigBuilder()
                .withUri(node3ip)
                .withDockerCertPath(node3certPath)
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
