package dprs.service;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.agent.model.Member;
import com.ecwid.consul.v1.catalog.model.CatalogService;
import dprs.entity.NodeAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by igordanis on 16/05/16.
 */
@Service
public class Chord {

    @Autowired
    ConsulClient consulClient;

    @Value("${spring.application.name}")
    String applicationName;

    private static final Logger logger = LoggerFactory.getLogger(Chord.class);

    Map<Integer, NodeAddress> chordAddresses = new HashMap<>();


    @Scheduled(fixedDelay = 5000)
    public void updateNodeAddresses() {
        logger.info("Polling current chord state");

        List<CatalogService> catalogServiceList = consulClient.getCatalogService(applicationName,
                QueryParams.DEFAULT).getValue();


        if (chordChanged(catalogServiceList)) {
            logger.info("   Found changes in chord: ");

            addedAddresses(catalogServiceList).stream().forEach(a -> {
                logger.info("       Found new machine in chord: " + a.getIP());
                chordAddresses.put(a.getHash(), a);
            });

            removedAdresses(catalogServiceList).stream().forEach(b -> {
                logger.info("       Found removed machine in chord: " + b.getIP());
                chordAddresses.remove(b.getHash());
            });

        } else {
            logger.info("   No changes found in chord.");
        }
        logger.info("Current machines in chord: " + chordAddresses.values().stream()
                .map(a ->  a.getHash() + " - " + a.getIP().toString() + ":" + new Integer(a
                        .getPort())
                        .toString())
                .collect(Collectors.toList())
        );
    }

    private boolean chordChanged(List<CatalogService> catalogServiceList) {

        boolean changed = false;

        //nieco sa pridalo alebo ubralo
        if (catalogServiceList.size() != chordAddresses.size())
            return true;

        //zmenil sa nejaky konkretny uzol
        if (addedAddresses(catalogServiceList).size() > 0
                || removedAdresses(catalogServiceList).size() > 0)
            return true;

        return false;
    }


    private List<NodeAddress> addedAddresses(List<CatalogService> catalogServiceList) {

        final List<NodeAddress> listOfAdditions = catalogServiceList.stream()
                .filter(address -> !containsAdress(address))
                .map(address -> new NodeAddress(address))
                .collect(Collectors.toList());

        return listOfAdditions;
    }


    private List<NodeAddress> removedAdresses(List<CatalogService> catalogServiceList) {

        final Set<Integer> consulAdresses = catalogServiceList.stream()
                .map(a -> new NodeAddress(a).getHash())
                .collect(Collectors.toSet());

        final List<NodeAddress> removedAdresses = chordAddresses.values().stream()
                .filter(chordAdress -> !consulAdresses.contains(chordAdress.getHash()))
                .collect(Collectors.toList());

        return removedAdresses;
    }



    private boolean containsAdress(CatalogService c) {
        return chordAddresses.containsKey(new NodeAddress(c).getHash());
    }


    public List<NodeAddress> findDestinationAdressesForKey(String key, int numberOfAdresses){

        Integer keyIndex = key.hashCode();

        final List<String> strings = chordAddresses.entrySet().stream()
                .sorted((o1, o2) -> o1.getKey().compareTo(o2.getKey()))
                .map(e -> "\n" + e.getKey() + " - " + e.getValue().toString())
                .collect(Collectors.toList());

        logger.info("Searching adresses for key " + key + " : with index " + keyIndex +
                "Available nodes: " + strings);


        //ak je v chorde menej uzlov ako pozadujeme
        if(chordAddresses.size() <  numberOfAdresses){

            logger.warn("Chord contains only " + chordAddresses.size() + " #nodes. Client " +
                    "requires " + numberOfAdresses);

            return chordAddresses.values().stream()
                    .collect(Collectors.toList());
        }


        final List<Map.Entry<Integer, NodeAddress>> collect = chordAddresses.entrySet().stream()
                //zosortujem nody podla indexu v chorde
                .sorted((o1, o2) -> o1.getKey().compareTo(o2.getKey()))
                //najdem nody s vacsim indexom ako hash
                .filter(o -> o.getKey() >= keyIndex)
                .collect(Collectors.toList());

        //vyberiem iba prvych N alebo menej ak som nakonci listu - quorum
        final List<NodeAddress> nodeAddress = collect
                .subList(0, numberOfAdresses <= collect.size() ? numberOfAdresses : collect.size())
                .stream()
                //vratim zoznam adries
                .map(a -> a.getValue())
                .collect(Collectors.toList());


        //ak je v nodeAdresach menej ako numberOfAdresses, znamena ze chord je nutne
        //pretiect
        List<NodeAddress> overflownAdresses = new ArrayList<>();
        if(nodeAddress.size() < numberOfAdresses){
            overflownAdresses = chordAddresses.entrySet().stream()
                    //zosortujem nody podla indexu v chorde
                    .sorted((o1, o2) -> o1.getKey().compareTo(o2.getKey()))
                    .collect(Collectors.toList())
                    .subList(0, numberOfAdresses - nodeAddress.size())
                    .stream()
                    .map(a -> a.getValue())
                    .collect(Collectors.toList());

            final ArrayList<NodeAddress> result = new ArrayList<>();
            result.addAll(nodeAddress);
            result.addAll(overflownAdresses);

            logger.warn("Required adresses needed overflow");

            return result;
        }else{

            logger.warn("Required adresses found without overflow");

            return nodeAddress;
        }
    }


    @Autowired
    Environment environment;


    public Integer getSelfIndexInChord(){

        Integer port = Integer.valueOf(environment.getProperty("local.server.port"));

        Member self = consulClient.getAgentSelf()
                .getValue()
                .getMember();

        return new NodeAddress(self.getAddress(), port).getHash();
    }

    public NodeAddress getSelfAddresssInChord(){
        return this.chordAddresses.get(getSelfIndexInChord());
    }

}
