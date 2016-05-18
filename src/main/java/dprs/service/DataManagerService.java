package dprs.service;

import dprs.components.InMemoryDatabase;
import dprs.entity.NodeAddress;
import dprs.response.dynamo.DynamoWriteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import static dprs.controller.dynamo.WriteController.DYNAMO_SINGLE_WRITE;
import static java.util.UUID.randomUUID;


@Service
public class DataManagerService {

    @Autowired
    ChordService chordService;

    @Value("${quorum.persist}")
    int persistQuorum;

    private static final Logger logger = LoggerFactory.getLogger(DataManagerService.class);

    @Scheduled(fixedDelay = 5000)
    public void constistencyCheck() {

        logger.info("Performing consistency check");

        InMemoryDatabase.INSTANCE.entrySet().stream().forEach((entrySet) -> {
            final String key = entrySet.getKey();

            if(isThisSyncCoordinator(key)){

                logger.info("This node is sync coordinator for key " + key);

                String transactionId = randomUUID().toString();

                final List<NodeAddress> destinationAdressesForKey = chordService
                        .findDestinationAdressesForKey(key, persistQuorum)
                        .stream()
                        .filter(nodeAddress -> {
                            return nodeAddress.getHash().compareTo(chordService.getSelfIndexInChord())
                                    != 0;
                        })
                        .collect(Collectors.toList());

                destinationAdressesForKey.stream().map(destinationAddress -> {

                        URI destinationUri = UriComponentsBuilder
                                .fromUriString("http://" + destinationAddress.getIP() + ":" +
                                        destinationAddress.getPort())
                                .path(DYNAMO_SINGLE_WRITE)
                                .queryParam("key", key)
                                .queryParam("value", entrySet.getValue().getValue())
                                .queryParam("transactionId", transactionId)
                                .queryParam("vectorClock", entrySet.getValue().getVectorClock().toJSON())
                                .build()
                                .toUri();

                        logger.info(transactionId, "Forwarding write request to: "
                                + destinationUri);

                        return new RestTemplate()
                                .getForObject(destinationUri, DynamoWriteResponse.class);

                }).collect(Collectors.toList());

            }else{
                logger.info("This node is not sync coordinator " + key);
            }

        });
        /*
         * Pre kazdy zaznam v db skontroluje, ci by ho nemal obsahovat niekto iny
         */
//        InMemoryDatabase.INSTANCE.entrySet().stream().forEach((entrySet) -> {
//            final String key = entrySet.getKey();
//
//            final List<NodeAddress> destinationAdressesForKey = chord
//                    .findDestinationAdressesForKey(key, persistQuorum);
//
//
//            final long count = destinationAdressesForKey.stream()
//                    .filter(nodeAddress -> nodeAddress.getHash().equals(chord.getSelfIndexInChord
//                            ()))
//                    .count();
//
//
//            if (count == 0) {
//                logger.info("Node contains unneeded values for key: " +key+ "  Deleting");
//                InMemoryDatabase.INSTANCE.entrySet().remove(key);
//            } else {
//                logger.info("Node contains only required values");
//            }
//
//        });

    }


    private boolean isThisSyncCoordinator(String key) {
        final Integer selfIndexInChord = chordService.getSelfIndexInChord();
        final Integer destinationAdressesForKey = chordService.findDestinationAdressesForKey
                (key, 1).get(0).getHash();
        return selfIndexInChord.equals(destinationAdressesForKey);
    }


}
