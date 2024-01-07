package fr.aym.gtwnpc.path;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RouteNode implements Comparable<RouteNode> {
    private final PathNode current;
    private PathNode previous;
    private double routeScore;
    private double estimatedScore;

    public RouteNode(PathNode current) {
        this(current, null, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    public RouteNode(PathNode current, PathNode previous, double routeScore, double estimatedScore) {
        this.current = current;
        this.previous = previous;
        this.routeScore = routeScore;
        this.estimatedScore = estimatedScore;
    }

    @Override
    public int compareTo(RouteNode other) {
        if (this.estimatedScore > other.estimatedScore) {
            return 1;
        } else if (this.estimatedScore < other.estimatedScore) {
            return -1;
        } else {
            return 0;
        }
    }
}
