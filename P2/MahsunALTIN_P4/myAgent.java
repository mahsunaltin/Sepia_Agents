package Agent;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.util.Direction;

@SuppressWarnings("serial")
public class myAgent extends Agent {

	double hitProbOfTower = 0.75;

	int startingPeasants = 0;
	Map<Integer, Integer> peasantHealth = new HashMap<Integer, Integer>();
	Map<Integer, Pair<Integer, Integer>> peasantLocations = new HashMap<Integer, Pair<Integer, Integer>>();
	gameMap Map;
	boolean randomWalk = true;

	boolean foundGoldMine = false;
	Pair<Integer, Integer> estGoldMineLocation;

	private StateView lastState;

	@Override
	public Map<Integer, Action> initialStep(StateView newstate, History.HistoryView statehistory) {

		lastState = newstate;

		int width = lastState.getXExtent();
		int height = lastState.getYExtent();

		estGoldMineLocation = new Pair<Integer, Integer>(width - 2, 2);

		for (UnitView unit : lastState.getUnits(playernum)) {
			String unitTypeName = unit.getTemplateView().getName();
			if (unitTypeName.equals("Peasant")) {
				startingPeasants++;
				peasantHealth.put(unit.getID(), unit.getHP());
				peasantLocations.put(unit.getID(),
						new Pair<Integer, Integer>(unit.getXPosition(), unit.getYPosition()));
			}
		}

		Map = new gameMap(width, height, 0.01);

		return middleStep(newstate, statehistory);
	}

	@Override
	public Map<Integer, Action> middleStep(StateView newState, History.HistoryView statehistory) {

		Map<Integer, Action> builder = new HashMap<Integer, Action>();
		lastState = newState;

		int currentGold = lastState.getResourceAmount(0, ResourceType.GOLD);
		if (currentGold >= 2000) {
			System.out.println("Congratulations! It is finished :)");
			System.exit(0);
			return null;
		} else {

			List<UnitView> peasantsID = new ArrayList<UnitView>();
			List<UnitView> townhallsID = new ArrayList<UnitView>();

			for (UnitView unit : lastState.getUnits(playernum)) {
				String unitTypeName = unit.getTemplateView().getName();
				if (unitTypeName.equals("TownHall")) {
					townhallsID.add(unit);
				} else if (unitTypeName.equals("Peasant")) {
					peasantsID.add(unit);
					peasantLocations.put(unit.getID(),
							new Pair<Integer, Integer>(unit.getXPosition(), unit.getYPosition()));
				}
			}

			UnitView myTownHallID = townhallsID.get(0);

			if (!peasantsID.isEmpty() && peasantsID.size() < startingPeasants
					&& currentGold >= peasantsID.get(0).getTemplateView().getGoldCost()) {

				int townhallID = townhallsID.get(0).getID();
				int peasantTemplateID = lastState.getTemplate(playernum, "Peasant").getID();
				builder.put(townhallID, Action.createCompoundProduction(townhallID, peasantTemplateID));
			} else if (peasantsID.isEmpty()) {
				System.out.println("One of the small probability happened!!! You lost all your peasants,\n"
						+ "before found the gold mine. Please, play again. :)");
				System.exit(0);
			}

			List<UnitView> hitList = new ArrayList<>();

			for (UnitView peasant : peasantsID) {
				int x = peasant.getXPosition();
				int y = peasant.getYPosition();

				updatePeasantViewRange(x, y);

				Map.incrementVisits(x, y);

				if (!peasantHealth.containsKey(peasant.getID())) {
					peasantHealth.put(peasant.getID(), peasant.getHP());
					peasantLocations.put(peasant.getID(),
							new Pair<Integer, Integer>(peasant.getXPosition(), peasant.getYPosition()));
				}

				if (peasantHealth.get(peasant.getID()) > peasant.getHP()) {

					Map.incrementHits(x, y);
					updateFromHit(x, y, true);
					randomWalk = false;
					hitList.add(peasant);
				} else {
					updateFromHit(x, y, false);
				}
				peasantHealth.put(peasant.getID(), peasant.getHP());
			}

			for (UnitView peasant : peasantsID) {
				if (randomWalk) {
					Node nextStep = randomAdjacentNode(peasant.getXPosition(), peasant.getYPosition());
					Direction direction = getDirection(nextStep.getX() - peasant.getXPosition(),
							nextStep.getY() - peasant.getYPosition());

					Action a = Action.createPrimitiveMove(peasant.getID(), direction);
					builder.put(peasant.getID(), a);

					peasantLocations.put(peasant.getID(), new Pair<Integer, Integer>(nextStep.getX(), nextStep.getY()));
				} else if (peasant.getCargoAmount() == 0) {
					if (isAdjacent(peasant.getXPosition(), peasant.getYPosition(), estGoldMineLocation.getX(),
							estGoldMineLocation.getY()) && foundGoldMine) {
						Action a = Action.createCompoundGather(peasant.getID(),
								lastState.resourceAt(estGoldMineLocation.getX(), estGoldMineLocation.getY()));
						builder.put(peasant.getID(), a);
					} else {
						List<Pair<Integer, Integer>> bestPath = getBestPath(
								new Pair<Integer, Integer>(peasant.getXPosition(), peasant.getYPosition()),
								estGoldMineLocation);
						Pair<Integer, Integer> nextStep = bestPath.get(0);

						if (hitList.contains(peasant) && Math.random() < 1) {
							Node node = randomAdjacentNode(peasant.getXPosition(), peasant.getYPosition());
							nextStep = new Pair<Integer, Integer>(node.getX(), node.getY());
						}

						Direction direction = getDirection(nextStep.getX() - peasant.getXPosition(),
								nextStep.getY() - peasant.getYPosition());

						Action a = Action.createPrimitiveMove(peasant.getID(), direction);
						builder.put(peasant.getID(), a);

						peasantLocations.put(peasant.getID(),
								new Pair<Integer, Integer>(nextStep.getX(), nextStep.getY()));
					}
				} else {
					if (isAdjacent(peasant.getXPosition(), peasant.getYPosition(), myTownHallID.getXPosition(),
							townhallsID.get(0).getYPosition())) {
						Action a = Action.createCompoundDeposit(peasant.getID(), myTownHallID.getID());
						builder.put(peasant.getID(), a);
					} else {
						List<Pair<Integer, Integer>> bestPath = getBestPath(
								new Pair<Integer, Integer>(peasant.getXPosition(), peasant.getYPosition()),
								new Pair<Integer, Integer>(myTownHallID.getXPosition(), myTownHallID.getYPosition()));
						Pair<Integer, Integer> nextStep = bestPath.get(0);

						if (hitList.contains(peasant) && Math.random() < 1) {
							Node node = randomAdjacentNode(peasant.getXPosition(), peasant.getYPosition());
							nextStep = new Pair<Integer, Integer>(node.getX(), node.getY());
						}

						Direction direction = getDirection(nextStep.getX() - peasant.getXPosition(),
								nextStep.getY() - peasant.getYPosition());

						Action a = Action.createPrimitiveMove(peasant.getID(), direction);
						builder.put(peasant.getID(), a);

						peasantLocations.put(peasant.getID(),
								new Pair<Integer, Integer>(nextStep.getX(), nextStep.getY()));
					}
				}
			}

			return builder;
		}
	}

	private Node randomAdjacentNode(int locX, int locY) {

		Node current = new Node(locX, locY, getHitProbability(locX, locY));

		Node fudge = new Node(estGoldMineLocation.getX(), estGoldMineLocation.getY(),
				getHitProbability(estGoldMineLocation.getX(), estGoldMineLocation.getY()));
		List<Node> adjacentNodes = getAdjacentNodes(current, new ArrayList<Node>(), fudge);

		Random random = new Random();
		return adjacentNodes.get(random.nextInt(adjacentNodes.size()));
	}

	private Direction getDirection(int x, int y) {
		if (x == 1 && y == 0) {
			return Direction.EAST;
		} else if (x == 1 && y == -1) {
			return Direction.NORTHEAST;
		} else if (x == 0 && y == -1) {
			return Direction.NORTH;
		} else if (x == -1 && y == -1) {
			return Direction.NORTHWEST;
		} else if (x == -1 && y == 0) {
			return Direction.WEST;
		} else if (x == -1 && y == 1) {
			return Direction.SOUTHWEST;
		} else if (x == 0 && y == 1) {
			return Direction.SOUTH;
		} else  {
			return Direction.SOUTHEAST;
		}
	}

	private void updatePeasantViewRange(int x, int y) {
		for (int i = -2; i <= 2; i++) {
			for (int j = -2; j <= 2; j++) {
				updateSeen(x + i, y + j);
			}
		}
	}

	private void updateSeen(int x, int y) {
		if (!lastState.inBounds(x, y)) {
			return;
		}

		Map.setSeen(x, y, true);

		if (lastState.isResourceAt(x, y)) {
			ResourceView resource = lastState.getResourceNode(lastState.resourceAt(x, y));
			if (resource.getType().equals(ResourceNode.Type.GOLD_MINE)) {
				foundGoldMine = true;
				estGoldMineLocation = new Pair<Integer, Integer>(x, y);
			} else if (resource.getType().equals(ResourceNode.Type.TREE)) {
				Map.setHasTree(x, y, true);
			}

			Map.settp(x, y, 0);
		} else if (lastState.isUnitAt(x, y)) {
			int unitID = lastState.unitAt(x, y);

			String unitName = lastState.getUnit(unitID).getTemplateView().getName();
			if (unitName.equalsIgnoreCase("ScoutTower")) {
				Map.settp(x, y, 1);
			} else {
				Map.settp(x, y, 0);
			}
		}

		if (!foundGoldMine && Map.getSeen(estGoldMineLocation.getX(), estGoldMineLocation.getY())) {
			updateGoldMineEstimate();
		}
	}

	private void updateGoldMineEstimate() {
		int x = lastState.getXExtent() - 2;
		int y = 2;
		for (int range = 1; range < Math.max(lastState.getXExtent(), lastState.getYExtent()); range++) {
			for (int i = x - range; i <= x + range; i++) {
				for (int j = y - range; j <= y + range; j++) {
					if (lastState.inBounds(i, j) && !Map.getSeen(i, j)) {
						estGoldMineLocation = new Pair<Integer, Integer>(i, j);
						return;
					}
				}
			}
		}
	}

	private double getHitProbability(int x, int y) {
		double probability = 0;

		for (int i = -4; i <= 4; i++) {
			for (int j = -4; j <= 4; j++) {
				int curX = x + i;
				int curY = y + j;
				if (lastState.inBounds(curX, curY) && distance(x, y, curX, curY) <= 4) {

					probability = (probability + Map.gettp(curX, curY)) - (probability * Map.gettp(curX, curY));
				}
			}
		}

		return probability * hitProbOfTower;
	}

	private void updateFromHit(int x, int y, boolean hit) {
		double[][] old = Map.getMapCopy();
		int fromX = Math.max(x - 4, 0);
		int toX = Math.min(old.length, x + 4);
		int fromY = Math.max(y - 4, 0);
		int toY = Math.min(old[0].length, y + 4);
		for (int r = fromX; r < toX; r++) {
			for (int c = fromY; c < toY; c++) {
				if (Map.getSeen(r, c) || distance(x, y, r, c) <= 4) {
					continue;
				}

				double phn;
				double pht;
				if (hit) {
					phn = 1;
					for (int rr = fromX; rr < toX; rr++) {
						for (int cc = fromY; cc < toY; cc++) {
							if (rr == r && cc == c)
								continue;
							phn *= (1 - old[rr][cc]) + (old[rr][cc] * (1 - hitProbOfTower));
						}
					}
					phn = 1 - phn;

					pht = 1 - ((1 - phn) * (1 - hitProbOfTower));
				} else {
					phn = 1;
					for (int rr = fromX; rr < toX; rr++) {
						for (int cc = fromY; cc < toY; cc++) {
							if (rr == r && cc == c)
								continue;
							phn *= (1 - old[rr][cc]) + (old[rr][cc] * (1 - hitProbOfTower));
						}
					}

					pht = phn * (1 - hitProbOfTower);
				}

				Map.settp(r, c, pht * old[r][c] / (pht * old[r][c] + phn * (1 - old[r][c])));
			}
		}
	}

	private double distance(int x1, int y1, int x2, int y2) {
		return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
	}

	private List<Pair<Integer, Integer>> getBestPath(Pair<Integer, Integer> curLocation, Pair<Integer, Integer> targetPoint) {
		List<Pair<Integer, Integer>> path = new LinkedList<Pair<Integer, Integer>>();

		Node current = new Node(curLocation.getX(), curLocation.getY(),
				getHitProbability(curLocation.getX(), curLocation.getY()));
		Node target = new Node(targetPoint.getX(), targetPoint.getY(), getHitProbability(targetPoint.getX(), targetPoint.getY()));
		List<Node> openSet = new ArrayList<>();
		List<Node> closedSet = new ArrayList<>();

		while (true) {
			openSet.remove(current);
			List<Node> adjacent = getAdjacentNodes(current, closedSet, target);

			for (Node neighbor : adjacent) {
				boolean inOpenset = false;
				List<Node> openSetCopy = new ArrayList<>(openSet);
				for (Node node : openSetCopy) {
					if (neighbor.equals(node)) {
						inOpenset = true;
						if (neighbor.getAccumulatedCost() < node.getAccumulatedCost()) {
							openSet.remove(node);
							openSet.add(neighbor);
						}
					}
				}

				if (!inOpenset) {
					openSet.add(neighbor);
				}
			}

			if (openSet.isEmpty()) {
				return null;
			} else if (current.equals(target)) {
				break;
			}

			closedSet.add(current);

			Node next = openSet.get(0);
			for (Node node : openSet) {
				if (node.getCost(target) < next.getCost(target)) {
					next = node;
				}
			}
			current = next;
		}

		path.add(new Pair<Integer, Integer>(curLocation.getX(), curLocation.getY()));
		while (current.getParent() != null) {
			current = current.getParent();
			path.add(0, new Pair<Integer, Integer>(current.getX(), current.getY()));
		}

		if (path.size() > 1) {
			path.remove(0);
		}

		return path;
	}

	private boolean isAdjacent(int x, int y, int targetX, int targetY) {
		for (int i = x - 1; i <= x + 1; i++) {
			for (int j = y - 1; j <= y + 1; j++) {
				if (i == targetX && j == targetY) {
					return true;
				}
			}
		}
		return false;
	}

	private List<Node> getAdjacentNodes(Node current, List<Node> closedSet, Node dest) {
		List<Node> adjacent = new ArrayList<Node>();

		for (int i = -1; i <= 1; i++) {
			inner: for (int j = -1; j <= 1; j++) {
				if (i == 0 && j == 0) {
					continue;
				}
				int x = current.getX() + i;
				int y = current.getY() + j;
				if (!lastState.inBounds(x, y) || Map.getHasTree(x, y) || peasantAt(x, y) || Map.gettp(x, y) == 1
						|| isTownHallAt(x, y, dest)) {
					continue;
				}
				Node node = new Node(x, y, getHitProbability(x, y), current);
				for (Node visitedNode : closedSet) {
					if (node.equals(visitedNode)) {
						continue inner;
					}
				}
				adjacent.add(node);
			}
		}

		return adjacent;
	}

	private boolean isTownHallAt(int x, int y, Node dest) {
		if (x == dest.getX() && y == dest.getY()) {
			return false;
		}

		if (lastState.isUnitAt(x, y)) {
			int unitID = lastState.unitAt(x, y);

			String unitName = lastState.getUnit(unitID).getTemplateView().getName();
			if (unitName.equalsIgnoreCase("Townhall")) {
				return true;
			}
		}

		return false;
	}

	private boolean peasantAt(int x, int y) {
		Set<Integer> keys = peasantLocations.keySet();
		for (Integer id : keys) {
			Pair<Integer, Integer> pair = peasantLocations.get(id);
			if (x == pair.getX() && y == pair.getY()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void terminalStep(StateView newstate, History.HistoryView statehistory) {
	}

	public myAgent(int playernum) {
		super(playernum);
	}

	@Override
	public void savePlayerData(OutputStream os) {
	}

	@Override
	public void loadPlayerData(InputStream is) {
	}
}

class Pair<T, U> {
	private T x;
	private U y;

	public Pair(T x, U y) {
		this.x = x;
		this.y = y;
	}

	public void setX(T x) {
		this.x = x;
	}

	public void setY(U y) {
		this.y = y;
	}

	public T getX() {
		return x;
	}

	public U getY() {
		return y;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof Pair)) {
			return false;
		}

		Pair<?, ?> pair = (Pair<?, ?>) obj;
		return x.equals(pair.getX()) && y.equals(pair.getY());
	}

}

class Node {
	private int x;
	private int y;
	private double probability = 0.01;
	private Node parent;

	public Node(int x, int y, double d) {
		this.x = x;
		this.y = y;
		this.probability = d;
	}

	public Node(int x, int y, double d, Node parent) {
		this.x = x;
		this.y = y;
		this.probability = d;
		this.parent = parent;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public double getProbability() {
		return probability;
	}

	public void setProbability(double probability) {
		this.probability = probability;
	}

	public Node getParent() {
		return parent;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		if (o == this) {
			return true;
		}
		if (!(o instanceof Node)) {
			return false;
		}

		Node node = (Node) o;
		return x == node.getX() && y == node.getY();
	}

	public double getCost(Node target) {
		return this.getHeuristicCost(target) + this.getAccumulatedCost();
	}

	public double getAccumulatedCost() {
		double accumCost;
		if (parent == null) {
			accumCost = probability;
		} else {
			accumCost = parent.getAccumulatedCost() + probability;
		}
		return accumCost;
	}

	private double getHeuristicCost(Node target) {
		int x = Math.abs(this.x - target.getX());
		int y = Math.abs(this.y - target.getY());
		int cost = Math.max(x, y);
		return cost * 0.01;
	}
}

@SuppressWarnings("serial")
class gameMap implements Serializable {

	private double tp[][];
	private int visits[][];
	private int hits[][];
	private boolean seen[][];
	private boolean hasTree[][];

	public gameMap(int width, int height, double towerDensity) {
		tp = new double[width][height];
		visits = new int[width][height];
		hits = new int[width][height];
		seen = new boolean[width][height];
		hasTree = new boolean[width][height];

		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				tp[i][j] = towerDensity;
				visits[i][j] = 0;
				hits[i][j] = 0;
				seen[i][j] = false;
				hasTree[i][j] = false;
			}
		}
	}

	public double[][] getMapCopy() {
		int x = tp.length;
		int y = tp[0].length;
		double[][] newMap = new double[x][y];
		for (int i = 0; i < x; i++) {
			System.arraycopy(tp[i], 0, newMap[i], 0, y);
		}
		return newMap;
	}

	public double gettp(int x, int y) {
		return tp[x][y];
	}

	public void settp(int x, int y, double d) {
		tp[x][y] = d;
	}

	public int getVisits(int x, int y) {
		return visits[x][y];
	}

	public void incrementVisits(int x, int y) {
		visits[x][y]++;
	}

	public int getHits(int x, int y) {
		return hits[x][y];
	}

	public void incrementHits(int x, int y) {
		hits[x][y]++;
	}

	public boolean getSeen(int x, int y) {
		return seen[x][y];
	}

	public void setSeen(int x, int y, boolean seen) {
		this.seen[x][y] = seen;
	}

	public boolean getHasTree(int x, int y) {
		return hasTree[x][y];
	}

	public void setHasTree(int x, int y, boolean hasTree) {
		this.hasTree[x][y] = hasTree;
	}

}
