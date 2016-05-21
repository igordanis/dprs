package fiit.dprs.team4.chaos.killCommand;

import com.github.dockerjava.api.model.Container;
import fiit.dprs.team4.chaos.utils.Loggable;
import fiit.dprs.team4.chaos.utils.NamedDockerClient;
import org.javatuples.Pair;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;


public class KillAllContainersOnSingleNode extends AbstractKillCommand implements Loggable {

    public KillAllContainersOnSingleNode(Set<Pair<NamedDockerClient, Container>>
                                                 containersInNodes) {
        super(containersInNodes);
    }

    @Override
    public void kill() {

        Set<NamedDockerClient> clients = this.containersInNodes.stream()
                .map(dockerClientContainerTuple -> dockerClientContainerTuple.getValue0())
                .collect(Collectors.toSet());

        if(clients.isEmpty())
            return;

        final int randomNodeIndex = ThreadLocalRandom.current().nextInt(clients.size());

        final NamedDockerClient randomDockerClient = (NamedDockerClient) clients.toArray()
                [randomNodeIndex];

        logger().info("Killing all containers on node: {}", randomDockerClient.getName());

        this.containersInNodes.stream()
                .filter(clientContainerPair -> clientContainerPair.getValue0() == randomDockerClient)
                .forEach(clientContainerPair -> killContainerInNode(clientContainerPair));


    }
}

