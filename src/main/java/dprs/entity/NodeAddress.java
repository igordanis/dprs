package dprs.entity;

import com.ecwid.consul.v1.catalog.model.CatalogService;

public class NodeAddress {

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
        String s = address + ":" + port;
        return new StringBuilder(s)
                .reverse()
                .toString()
                .hashCode();
    }

    @Override
    public String toString() {
        return address + ":" + port;
    }


//
//    @Override
//    public int compareTo(NodeAddress nodeAddress) {
//        return getHash().compareTo(nodeAddress.getHash());
//    }
//
//    @Override
//    public boolean equals(Object nodeAddress) {
//        return nodeAddress == null || address.equals(((NodeAddress) nodeAddress).getIP());
//    }


}
