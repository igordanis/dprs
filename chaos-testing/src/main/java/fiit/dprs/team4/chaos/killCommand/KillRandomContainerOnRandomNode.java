package fiit.dprs.team4.chaos.killCommand;

import com.github.dockerjava.api.model.Container;
import fiit.dprs.team4.chaos.utils.Loggable;
import fiit.dprs.team4.chaos.utils.NamedDockerClient;
import org.javatuples.Pair;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;


public class KillRandomContainerOnRandomNode extends AbstractKillCommand implements Loggable{


    public KillRandomContainerOnRandomNode(Set<Pair<NamedDockerClient, Container>> containersInNodes) {
        super(containersInNodes);
    }

    @Override
    public void kill() {

        logger().info("Killing random container on random node:");

        if(this.containersInNodes.isEmpty())
            return;

        int rand = ThreadLocalRandom.current().nextInt(this.containersInNodes.size());

        Pair<NamedDockerClient, Container> pair = containersInNodes.stream()
                .collect(Collectors .toList())
                .get(rand);

        this.killContainerInNode(pair);
    }
}

