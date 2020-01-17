package Agent;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Vector;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.util.Direction;

public class SAgent extends Agent {
	Comparator<ArrayList<Integer[]>> pathDistanceComparator = new Comparator<ArrayList<Integer[]>>() {
		@Override
		public int compare(ArrayList<Integer[]> s1, ArrayList<Integer[]> s2) {
			double distance1 = Math.sqrt(Math.pow(s1.get(s1.size() - 1)[0] - SAgent.target[0], 2)
					+ Math.pow(s1.get(s1.size() - 1)[1] - SAgent.target[1], 2));
			double distance2 = Math.sqrt(Math.pow(s2.get(s2.size() - 1)[0] - SAgent.target[0], 2)
					+ Math.pow(s2.get(s2.size() - 1)[1] - SAgent.target[1], 2));
			return (int) (distance1 - distance2);
		}
	};

	PriorityQueue<ArrayList<Integer[]>> pq = new PriorityQueue<>(pathDistanceComparator);

	static Integer[] target = { 15, 14 };

	int[][] map;
	int[][] otherMap;

	ArrayList<Integer[]> finalPath;

	List<Integer> footmenIds;
	List<Integer> townhallsIds;

	public SAgent(int arg0) {
		super(arg0);
	}

	@Override
	public Map<Integer, Action> initialStep(StateView currentState, HistoryView statehistory) {

		map = new int[currentState.getXExtent()][currentState.getYExtent()];
		otherMap = new int[currentState.getXExtent()][currentState.getYExtent()];

		List<ResourceNode.ResourceView> allResources = currentState.getAllResourceNodes();

		for (ResourceNode.ResourceView v : allResources)
			map[v.getXPosition()][v.getYPosition()] = 1;

		List<Integer> allUnitIds = currentState.getAllUnitIds();
		footmenIds = new ArrayList<Integer>();
		townhallsIds = new ArrayList<Integer>();
		
		for (int a : allUnitIds) {
			UnitView unit = currentState.getUnit(a);
			String unitTypeName = unit.getTemplateView().getName();
			if (unitTypeName.equals("Footman")) 
				footmenIds.add(a);
			if (unitTypeName.equals("TownHall"))
				townhallsIds.add(a);
		}

		finalPath = findPath(currentState.getUnit(footmenIds.get(0)).getXPosition(),
				currentState.getUnit(footmenIds.get(0)).getYPosition());
		finalPath.remove(0);

		return null;
	}

	@Override
	public Map<Integer, Action> middleStep(StateView currentState, HistoryView statehistory) {

		Map<Integer, Action> actions = new HashMap<Integer, Action>();

		if (finalPath.size()==0) {
			actions.put(footmenIds.get(0), Action.createPrimitiveAttack(footmenIds.get(0), townhallsIds.get(0)));
			return actions;
		}
		Integer[] t = finalPath.remove(0);
		if (t[0] > currentState.getUnit(footmenIds.get(0)).getXPosition()) {
			actions.put(footmenIds.get(0), Action.createPrimitiveMove(footmenIds.get(0), Direction.EAST));
		} else if (t[0] < currentState.getUnit(footmenIds.get(0)).getXPosition()) {
			actions.put(footmenIds.get(0), Action.createPrimitiveMove(footmenIds.get(0), Direction.WEST));
		} else if (t[1] > currentState.getUnit(footmenIds.get(0)).getYPosition()) {
			actions.put(footmenIds.get(0), Action.createPrimitiveMove(footmenIds.get(0), Direction.SOUTH));
		} else {
			actions.put(footmenIds.get(0), Action.createPrimitiveMove(footmenIds.get(0), Direction.NORTH));
		}

		return actions;
	}

	@Override
	public void savePlayerData(OutputStream arg0) {

	}

	@Override
	public void terminalStep(StateView arg0, HistoryView arg1) {

	}

	@SuppressWarnings("unchecked")
	private ArrayList<Integer[]> findPath(int x, int y) {
		ArrayList<Integer[]> temp = new ArrayList<>();
		Integer[] tint = { x, y };
		otherMap[x][y] = 1;
		temp.add(tint);
		pq.add(temp);

		while (pq.size() > 0) {
			ArrayList<Integer[]> myPath = pq.poll();

			Integer[] myCoordinat = myPath.get(myPath.size() - 1);
			if (myCoordinat[0] == target[0] && myCoordinat[1] == target[1]) {
				return myPath;
			}

			if (myCoordinat[0] > 0 && map[myCoordinat[0] - 1][myCoordinat[1]] == 0
					&& otherMap[myCoordinat[0] - 1][myCoordinat[1]] == 0) {
				ArrayList<Integer[]> left = (ArrayList<Integer[]>) myPath.clone();
				Integer[] leftI = { myCoordinat[0] - 1, myCoordinat[1] };
				left.add(leftI);
				pq.add(left);
				otherMap[myCoordinat[0] - 1][myCoordinat[1]] = 1;
			}

			if (myCoordinat[0] < map.length - 1 && map[myCoordinat[0] + 1][myCoordinat[1]] == 0
					&& otherMap[myCoordinat[0] + 1][myCoordinat[1]] == 0) {
				ArrayList<Integer[]> right = (ArrayList<Integer[]>) myPath.clone();
				Integer[] rightI = { myCoordinat[0] + 1, myCoordinat[1] };
				right.add(rightI);
				pq.add(right);
				otherMap[myCoordinat[0] + 1][myCoordinat[1]] = 1;

			}

			if (myCoordinat[1] > 0 && map[myCoordinat[0]][myCoordinat[1] - 1] == 0
					&& otherMap[myCoordinat[0]][myCoordinat[1] - 1] == 0) {
				ArrayList<Integer[]> up = (ArrayList<Integer[]>) myPath.clone();
				Integer[] upI = { myCoordinat[0], myCoordinat[1] - 1 };
				up.add(upI);
				pq.add(up);
				otherMap[myCoordinat[0]][myCoordinat[1] - 1] = 1;

			}
			if (myCoordinat[1] < map[0].length - 1 && map[myCoordinat[0]][myCoordinat[1] + 1] == 0
					&& otherMap[myCoordinat[0]][myCoordinat[1] + 1] == 0) {
				ArrayList<Integer[]> down = (ArrayList<Integer[]>) myPath.clone();
				Integer[] downI = { myCoordinat[0], myCoordinat[1] + 1 };
				down.add(downI);
				pq.add(down);
				otherMap[myCoordinat[0]][myCoordinat[1] + 1] = 1;

			}

		}
		System.out.println("No valid path!");
		return null;
	}

	@Override
	public void loadPlayerData(InputStream arg0) {

	}

}
