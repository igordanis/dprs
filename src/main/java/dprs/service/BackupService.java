package dprs.service;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import dprs.entity.NodeAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BackupService {
    private static final Logger logger = LoggerFactory.getLogger(BackupService.class);

    private NodeAddress addressSelf = null;
    private List<NodeAddress> addressList = null;

    @Autowired
    ConsulClient consulClient;
    @Value("${spring.application.name}")
    String applicationName;

    public boolean sendData(String address, String path, String key, int value) {
        URI targetUrl = UriComponentsBuilder.fromUriString("http://" + address + ":8080")
                .path(path)
                .queryParam("key", key)
                .queryParam("value", value)
                .queryParam("backup", false)
                .build()
                .toUri();

        return new RestTemplate().getForObject(targetUrl, Boolean.class);
    }

    public Object sendData(String address, String path, Map<String, Object> params) {
        logger.info("Seding data from " + addressSelf.getAddress() + " to " + address);

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("http://" + address + ":8080").path(path);

        for (String key : params.keySet()) {
            builder.queryParam(key, params.get(key));
        }
        URI targetUrl = builder.build().toUri();

        try {
            return new RestTemplate().getForObject(targetUrl, Object.class);
        } catch (Exception e) {
            logger.error("Failed to send data to " + address, e);
            return null;
        }
    }

    public List<NodeAddress> updateNodeAddresses() {
        List<NodeAddress> addressList = new ArrayList<>();

        if (addressSelf == null) {
            addressSelf = new NodeAddress(consulClient.getAgentSelf().getValue().getMember().getAddress());
        }

        addressList.addAll(
                consulClient.getHealthServices(applicationName, true, QueryParams.DEFAULT).getValue()
                        .stream().map(service ->
                        new NodeAddress(service.getNode().getAddress())
                ).sorted().collect(Collectors.toList())
        );

        if (this.addressList == null) {
            this.addressList = addressList;
        } else if (!addressList.equals(this.addressList)) {
            backupData(addressList);
        }

        return addressList;
    }

    public NodeAddress getNextAddress(int offset) {
        int index = -1;
        for (int i = 0; i < addressList.size(); i++) {
            if (addressList.get(i).getAddress().equals(addressSelf.getAddress())) {
                index = i;
                break;
            }
        }

        return offset >= addressList.size() || index == -1 ?
                null :
                addressList.get((index + offset) % addressList.size());
    }

    private void backupData(List<NodeAddress> updatedAddressList) {
        this.addressList = updatedAddressList;
    }
}
