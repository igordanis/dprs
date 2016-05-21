package fiit.dprs.team4.killCommand;

import com.github.dockerjava.api.model.Container;
import fiit.dprs.team4.utils.NamedDockerClient;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public abstract class AbstractKillCommand {

    Set<Pair<NamedDockerClient, Container>> containersInNodes;

    public static final Logger logger = LoggerFactory.getLogger(AbstractKillCommand.class);


    public AbstractKillCommand(Set<Pair<NamedDockerClient, Container>> containersInNodes){
        this.containersInNodes = containersInNodes;
    }

    protected void killContainerInNode(Pair<NamedDockerClient, Container> dockerClientContainerTuple){

            NamedDockerClient dc = dockerClientContainerTuple.getValue0();
            Container containerToBeKilled = dockerClientContainerTuple.getValue1();

            logger.info("   Killing container {}  on node {} ", containerToBeKilled.getNames
                    (), dc.getName());

            dc.killContainerCmd(containerToBeKilled.getId()).exec();
    }

    public abstract void kill();
}