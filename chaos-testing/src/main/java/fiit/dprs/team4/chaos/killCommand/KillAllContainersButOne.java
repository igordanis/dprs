package fiit.dprs.team4.chaos.killCommand;

import com.github.dockerjava.api.model.Container;
import fiit.dprs.team4.chaos.utils.Loggable;
import fiit.dprs.team4.chaos.utils.NamedDockerClient;
import org.javatuples.Pair;

import java.util.Set;

/**
 * Created by igordanis on 20/05/16.
 */

public class KillAllContainersButOne extends AbstractKillCommand implements Loggable{

    public KillAllContainersButOne(Set<Pair<NamedDockerClient, Container>> containersInNodes) {
        super(containersInNodes);
    }

    @Override
    public void kill() {

        logger().info("Killing all containers but one.");
        containersInNodes.stream()
                .skip(1)
                .forEach(clientContainerPair -> killContainerInNode(clientContainerPair));
    }

}

