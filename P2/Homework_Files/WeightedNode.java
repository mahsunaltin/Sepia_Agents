public class WeightedNode {
    public static GraphNode target = null;

    private GraphNode node;
    private WeightedNode parent;
    private int accumCost;

    public WeightedNode(GraphNode node, WeightedNode parent) {
        this.node = node;
        this.parent = parent;
        this.accumCost = -1;
    }

    public GraphNode getNode() {
        return this.node;
    }

    public WeightedNode getParent() {
        return this.parent;
    }

    public int getAccumulatedCost() {
        if (this.accumCost == -1)
            accumulateCost();
        return this.accumCost;
    }

    public int getHeuristicCost() {
        int x = Math.abs(node.x - target.x);
        int y = Math.abs(node.y - target.y);
        int cost = Math.max(x, y);
        return cost;
    }

    public int getCost() {
        return this.getHeuristicCost() + this.getAccumulatedCost();
    }

    // Sets the accumulated cost of the node.
    private void accumulateCost() {
        if (parent == null)
            accumCost = 0;
        else
            accumCost = parent.accumCost + 1;
    }
    
    @Override
    public String toString() {
    	return String.format("%d,%d g(n)=%d, h(n)=%d, f(n)=%d", node.x, node.y, getAccumulatedCost(), getHeuristicCost(), getCost());
    }
}
