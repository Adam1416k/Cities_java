import java.util.*;
import java.io.Serializable;

public class ListGraph<T> implements Graph<T>, Serializable {
    
    private final Map<T, Set<Edge<T>>> nodes = new HashMap<>();

    public void add(T node) {
        if (!nodes.containsKey(node)) {
            nodes.put(node, new HashSet<>());
        }
    }

    public void remove(T node) {
        if (!nodes.containsKey(node)) {
            throw new NoSuchElementException();
        }
        for (T n : nodes.keySet()) {
            nodes.get(n).removeIf(edge -> edge.getDestination().equals(node));
        }
        nodes.remove(node);
    }

    public void connect(T node1, T node2, String name, int weight) {
        if (!nodes.containsKey(node1) || !nodes.containsKey(node2)) {
            throw new NoSuchElementException();
        }
        if (weight < 0) {
            throw new IllegalArgumentException();
        }
        for (Edge<T> edge : nodes.get(node1)) {
            if (edge.getDestination().equals(node2)) {
                throw new IllegalStateException();
            }
        }
        Edge<T> from = new Edge<>(node1, node2, name, weight);
        Edge<T> to = new Edge<>(node2, node1, name, weight);
        nodes.get(node1).add(from);
        nodes.get(node2).add(to);
    }

    public void disconnect(T node1, T node2) {
        if (!nodes.containsKey(node1) || !nodes.containsKey(node2)) {
            throw new NoSuchElementException();
        }
        boolean removed = false;
        for (Edge<T> edge : nodes.get(node1)) {
            if (edge.getDestination().equals(node2)) {
                nodes.get(node1).remove(edge);
                removed = true;
                break;
            }
        }
        if (!removed) {
            throw new IllegalStateException();
        }
        removed = false;
        for (Edge<T> edge : nodes.get(node2)) {
            if (edge.getDestination().equals(node1)) {
                nodes.get(node2).remove(edge);
                removed = true;
                break;
            }
        }
        if (!removed) {
            throw new IllegalStateException();
        }
    }

    public void setConnectionWeight(T node1, T node2, int weight) {
        if (!nodes.containsKey(node1) || !nodes.containsKey(node2)) {
            throw new NoSuchElementException();
        }
        if (weight < 0) {
            throw new IllegalArgumentException();
        }
        boolean updated = false;
        for (Edge<T> edge : nodes.get(node1)) {
            if (edge.getDestination().equals(node2)) {
                edge.setWeight(weight);
                updated = true;
                break;
            }
        }
        if (!updated) {
            throw new NoSuchElementException();
        }
        updated = false;
        for (Edge<T> edge : nodes.get(node2)) {
            if (edge.getDestination().equals(node1)) {
                edge.setWeight(weight);
                updated = true;
                break;
            }
        }
        if (!updated) {
            throw new NoSuchElementException();
        }
    }
    
    public Set<T> getNodes() {
        return new HashSet<>(nodes.keySet());
    }

    public Set<Edge<T>> getEdgesFrom(T node) {
        if (!nodes.containsKey(node)) {
            throw new NoSuchElementException();
        }
        return new HashSet<>(nodes.get(node));
    }

    public Edge<T> getEdgeBetween(T node1, T node2) {
        if (!nodes.containsKey(node1) || !nodes.containsKey(node2)) {
            throw new NoSuchElementException();
        }
        for (Edge<T> edge : nodes.get(node1)) {
            if (edge.getDestination().equals(node2)) {
                return edge;
            }
        }
        return null;
    }

    public boolean pathExists(T from, T to) {
        if (!nodes.containsKey(from) || !nodes.containsKey(to)) {
            return false;
        }
        Set<T> visited = new HashSet<>();
        return depthFirstSearch(from, to, visited);
    }

    private boolean depthFirstSearch(T from, T to, Set<T> visited) {
        visited.add(from);
        if (from.equals(to)) {
            return true;
        }
        for (Edge<T> edge : nodes.get(from)) {
            T next = edge.getDestination();
            if (!visited.contains(next)) {
                if (depthFirstSearch(next, to, visited)) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<Edge<T>> getPath(T from, T to) {
        if (!nodes.containsKey(from) || !nodes.containsKey(to)) {
            return null;
        }

        Map<T, Double> distances = new HashMap<>();
        Map<T, Edge<T>> previousEdges = new HashMap<>();
        PriorityQueue<NodeDistance<T>> queue = new PriorityQueue<>();
        Set<T> visited = new HashSet<>();

        distances.put(from, 0.0);
        queue.offer(new NodeDistance<>(from, 0.0));

        while (!queue.isEmpty()) {
            T current = queue.poll().getNode();
            if (current.equals(to)) {
                break;
            }
            visited.add(current);
            for (Edge<T> edge : nodes.get(current)) {
                T next = edge.getDestination();
                if (!visited.contains(next)) {
                    double newDistance = distances.get(current) + edge.getWeight();
                    if (!distances.containsKey(next) || newDistance < distances.get(next)) {
                        distances.put(next, newDistance);
                        previousEdges.put(next, edge);
                        queue.offer(new NodeDistance<>(next, newDistance));
                    }
                }
            }
        }
        if (!previousEdges.containsKey(to)) {
            return null;
        }
        List<Edge<T>> path = new ArrayList<>();
        Edge<T> edge = previousEdges.get(to);
        while (edge != null) {
            path.add(0, edge);
            edge = previousEdges.get(edge.getFrom());
        }
        return path;
    }
   
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (T node : nodes.keySet()) {
            sb.append(node.toString()).append("\n");
            for (Edge<T> edge : nodes.get(node)) {
                sb.append("  ").append(edge.toString()).append("\n");
            }
        }
        return sb.toString();
    }

    public void clear() {
        nodes.clear();
    }
}