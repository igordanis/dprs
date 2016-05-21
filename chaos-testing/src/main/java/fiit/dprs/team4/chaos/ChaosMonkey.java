package fiit.dprs.team4.chaos;


import fiit.dprs.team4.chaos.killCommand.KillRandomContainerOnRandomNode;
import fiit.dprs.team4.chaos.utils.Loggable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;


@Service
public class ChaosMonkey implements Loggable{

    @Autowired
    DynamoCluster dynamoCluster;


//    @Scheduled(fixedDelay = 10000)
    public void scheduledRequest() {
        final int randomTest = ThreadLocalRandom.current().nextInt(1,2);

        new KillRandomContainerOnRandomNode(dynamoCluster.getAllDynamoContainersInAllNodes()).kill();

//        switch (randomTest){
//            case 1: new KillAllContainersButOne(dynamoCluster.getAllDynamoContainersInAllNodes())
//                    .kill();
//            case 2: new KillAllContainersOnSingleNode(dynamoCluster
//                    .getAllDynamoContainersInAllNodes()).kill();
//        }


    }





}
