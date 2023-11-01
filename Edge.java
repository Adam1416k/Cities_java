import java.io.Serializable;

public class Edge<T> implements Serializable {
    private final T from;
    private final T to;
    private int weight;
    private final String name;
    
    public Edge(T from, T to, String name, int weight) {
        this.from = from;
        this.to = to;
        this.name = name;
        setWeight(weight);
    }

    public T getDestination() {
        return to;
    }

    public T getFrom() {
        return from;
    }
    
    public int getWeight() {
        return weight;
    }
    
    public void setWeight(int weight) {
        if (weight < 0) {
            throw new IllegalArgumentException("Vikten kan inte vara negativ");
        }
        this.weight = weight;
    }

    public String getName() {
        return this.name;
    }

    public String toString() {
        return String.format("till %s med %s -> %s tar %d", to.toString(), from.toString(), to.toString(), weight);
    }
}