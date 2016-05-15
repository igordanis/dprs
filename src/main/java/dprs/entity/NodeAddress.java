package dprs.entity;

public class NodeAddress implements Comparable<NodeAddress> {

    private String address;

    public NodeAddress(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getHash() {
        return new StringBuilder(address).reverse().toString().hashCode();
    }

    @Override
    public int compareTo(NodeAddress nodeAddress) {
        return getHash().compareTo(nodeAddress.getHash());
    }

    @Override
    public boolean equals(Object nodeAddress) {
        return nodeAddress == null || address.equals(((NodeAddress) nodeAddress).getAddress());
    }

    @Override
    public String toString() {
        return "NodeAddress{" +
                "address='" + address + '\'' +
                '}';
    }
}
