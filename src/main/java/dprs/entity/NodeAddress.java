package dprs.entity;

public class NodeAddress implements Comparable<NodeAddress> {

    private String address;
    private int port;

    public NodeAddress(String address) {
        this.address = address;
    }

    public NodeAddress(String address, int port) {
        this.address = address;
        this.port = port;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Integer getHash() {
        return new StringBuilder(address + port).reverse().toString().hashCode();
    }

    @Override
    public int compareTo(NodeAddress nodeAddress) {
        return getHash().compareTo(nodeAddress.getHash());
    }

    @Override
    public boolean equals(Object nodeAddress) {
        if (nodeAddress == null) {
            return false;
        }
        NodeAddress targetAddress = (NodeAddress) nodeAddress;
        return address.equals(targetAddress.address) && port == targetAddress.port;
    }

    @Override
    public String toString() {
        return "NodeAddress{" +
                "address='" + address + '\'' +
                '}';
    }
}
