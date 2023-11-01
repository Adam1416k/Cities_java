public class NodeDistance<T> implements Comparable<NodeDistance<T>> {
    private final T node;
    private final double distance;

    public NodeDistance(T node, double distance) {
        this.node = node;
        this.distance = distance;
    }

    public T getNode() {
        return node;
    }

    public double getDistance() {
        return distance;
    }

    @Override
    public int compareTo(NodeDistance<T> other) {
        return Double.compare(distance, other.distance);
    }
}
