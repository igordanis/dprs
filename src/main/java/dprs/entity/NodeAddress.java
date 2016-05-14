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

    public String hash() {
        return address;
    }

    @Override
    public int compareTo(NodeAddress nodeAddress) {
        return hash().compareTo(nodeAddress.hash());
    }

    @Override
    public String toString() {
        return "NodeAddress{" +
                "address='" + address + '\'' +
                '}';
    }
}
