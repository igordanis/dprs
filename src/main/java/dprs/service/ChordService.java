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
import org.w3c.dom.Node;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by igordanis on 16/05/16.
 */
@Service
public class ChordService {

    @Autowired
    ConsulClient consulClient;

    @Autowired
    DataManagerService dataManagerService;

    @Value("${spring.application.name}")
    String applicationName;

    @Value("${server.port}")
    private int serverPort;

    private static final Logger logger = LoggerFactory.getLogger(ChordService.class);

    Map<Integer, NodeAddress> chordAddresses = new HashMap<>();

    @Scheduled(fixedDelay = 5000)
    public void updateNodeAddresses() {
        List<CatalogService> catalogServiceList = consulClient.getCatalogService(applicationName,
                QueryParams.DEFAULT).getValue();

        if (chordChanged(catalogServiceList)) {
            logger.info("Found changes in chord: ");

            final HashMap<Integer, NodeAddress> oldChordAddresses = new HashMap<>(chordAddresses);

            addedAddresses(catalogServiceList).stream().forEach(address -> {
                logger.info("   Found new machine in chord: " + address.getFullAddress());
                chordAddresses.put(address.getHash(), address);
            });

            removedAdresses(catalogServiceList).stream().forEach(address -> {
                logger.info("   Found removed machine in chord: " + address.getFullAddress());
                chordAddresses.remove(address.getHash());
            });

            dataManagerService.handleChangesInChord(oldChordAddresses, chordAddresses);

            logger.info("Current machines in chord: " + chordAddresses.values().stream()
                    .map(a -> a.getHash() + " - " + a.getIP() + ":" + a.getPort())
                    .collect(Collectors.toList())
            );
        }
    }

    private boolean chordChanged(List<CatalogService> catalogServiceList) {
        // nieco sa pridalo alebo ubralo
        if (catalogServiceList.size() != chordAddresses.size())
            return true;

        // zmenil sa nejaky konkretny uzol
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

    public List<NodeAddress> findDestinationAddressesForKeyInChord(String key, int numberOfAddresses) {
        return findDestinationAddressesForKeyInList(chordAddresses.values(), key, numberOfAddresses);
    }

    public List<NodeAddress> findDestinationAddressesForKeyInList(Collection<NodeAddress> addressList, String key, int numberOfAddresses) {
        List<NodeAddress> sortedAddresses = addressList.stream().sorted().collect(Collectors.toList());

        if (chordAddresses.size() < numberOfAddresses) {
            logger.warn("Chord contains only " + chordAddresses.size() + " #nodes. Client requires " + numberOfAddresses);
            return sortedAddresses;
        }

        // Find first address
        Optional<NodeAddress> firstAddressFind = sortedAddresses.stream().filter(a -> a.getHash() >= key.hashCode()).findFirst();
        NodeAddress firstAddress;

        // If first address is not found by hash, the last address is responsible for key.
        if (firstAddressFind.isPresent()) {
            firstAddress = firstAddressFind.get();
        } else {
            firstAddress = sortedAddresses.get(sortedAddresses.size() - 1);
        }

        int firstAddressIndex = sortedAddresses.indexOf(firstAddress);

        // Collect #numbeOfAddresses addresses starting with first address (with overflow)
        List<NodeAddress> resultAddresses = IntStream.range(0, numberOfAddresses).mapToObj(i -> {
            int targetIndex = (firstAddressIndex + i) % addressList.size();
            return sortedAddresses.get(targetIndex);
        }).collect(Collectors.toList());

        return resultAddresses;
    }

    public Integer getSelfIndexInChord() {
//        Integer port = Integer.valueOf(environment.getProperty("local.server.port"));

        Member self = consulClient.getAgentSelf()
                .getValue()
                .getMember();

        return new NodeAddress(self.getAddress(), serverPort).getHash();
    }

    public NodeAddress getSelfAddressInChord() {
        return this.chordAddresses.get(getSelfIndexInChord());
    }

    public NodeAddress getAddressInChordByOffset(int offset) {
        return getAddressInMapByOffset(chordAddresses, offset);
    }

    public NodeAddress getAddressInMapByOffset(Map<Integer, NodeAddress> addressMap, int offset) {
        if (addressMap.size() == 0) {
            return null;
        }

        NodeAddress myAddress = getSelfAddressInChord();
        List<NodeAddress> addressList = new ArrayList<>(addressMap.values());
        Collections.sort(addressList);

        int myIndex = addressList.indexOf(myAddress);
        int targetAddressIndex = (myIndex + offset + addressList.size()) % addressList.size();

        return addressList.get(targetAddressIndex);
    }

    public Map<Integer, NodeAddress> getChordAddresses() {
        return chordAddresses;
    }

    public int getChordCount() { return chordAddresses.size(); }

//    public List<NodeAddress> oldFindDestinationAddressesForKey(String key, int numberOfAddresses) {
//        Integer keyIndex = key.hashCode();
//
//        final List<String> strings = chordAddresses.entrySet().stream()
//                .sorted((o1, o2) -> o1.getKey().compareTo(o2.getKey()))
//                .map(e -> "\n" + e.getKey() + " - " + e.getValue().toString())
//                .collect(Collectors.toList());
//
//        logger.info("Searching adresses for key " + key + " : with index " + keyIndex +
//                "Available nodes: " + strings);
//
//
//        //ak je v chorde menej uzlov ako pozadujeme
//        if(chordAddresses.size() <  numberOfAddresses){
//
//            logger.warn("Chord contains only " + chordAddresses.size() + " #nodes. Client " +
//                    "requires " + numberOfAddresses);
//
//            return chordAddresses.values().stream()
//                    .collect(Collectors.toList());
//        }
//
//        final List<Map.Entry<Integer, NodeAddress>> collect = chordAddresses.entrySet().stream()
//                //zosortujem nody podla indexu v chorde
//                .sorted((o1, o2) -> o1.getKey().compareTo(o2.getKey()))
//                //najdem nody s vacsim indexom ako hash
//                .filter(o -> o.getKey() >= keyIndex)
//                .collect(Collectors.toList());
//
//        //vyberiem iba prvych N alebo menej ak som nakonci listu - quorum
//        final List<NodeAddress> nodeAddress = collect
//                .subList(0, numberOfAddresses <= collect.size() ? numberOfAddresses : collect.size())
//                .stream()
//                //vratim zoznam adries
//                .map(a -> a.getValue())
//                .collect(Collectors.toList());
//
//        //ak je v nodeAdresach menej ako numberOfAdresses, znamena ze chord je nutne
//        //pretiect
//        List<NodeAddress> overflownAdresses = new ArrayList<>();
//        if(nodeAddress.size() < numberOfAddresses){
//            overflownAdresses = chordAddresses.entrySet().stream()
//                    //zosortujem nody podla indexu v chorde
//                    .sorted((o1, o2) -> o1.getKey().compareTo(o2.getKey()))
//                    .collect(Collectors.toList())
//                    .subList(0, numberOfAddresses - nodeAddress.size())
//                    .stream()
//                    .map(a -> a.getValue())
//                    .collect(Collectors.toList());
//
//            final ArrayList<NodeAddress> result = new ArrayList<>();
//            result.addAll(nodeAddress);
//            result.addAll(overflownAdresses);
//
//            logger.warn("Required adresses needed overflow");
//
//            return result;
//        } else {
//
//            logger.warn("Required adresses found without overflow");
//
//            return nodeAddress;
//        }
//    }
}
