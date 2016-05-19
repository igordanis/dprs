package dprs.entity;

import com.ecwid.consul.v1.catalog.model.CatalogService;

public class NodeAddress implements Comparable<NodeAddress> {

    private String address;
    private int port;


    public NodeAddress(String address, int port) {
        this.address = address;
        this.port = port;
    }

    public NodeAddress(CatalogService catalogService) {
        this(catalogService.getAddress(), catalogService.getServicePort());
    }

    public String getIP() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public Integer getHash() {
        return new StringBuilder(getFullAddress())
                .reverse()
                .toString()
                .hashCode();
    }

    public String getFullAddress() {
        return address + ":" + port;
    }

    @Override
    public String toString() {
        return address + ":" + port;
    }

    @Override
    public int compareTo(NodeAddress nodeAddress) {
        return getHash().compareTo(nodeAddress.getHash());
    }

    @Override
    public boolean equals(Object nodeAddress) {
        return nodeAddress != null && getFullAddress().equals(((NodeAddress) nodeAddress).getFullAddress());
    }
}
