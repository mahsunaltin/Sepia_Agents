/**
 *  Strategy Engine for Programming Intelligent Agents (SEPIA)
    Copyright (C) 2012 Case Western Reserve University

    This file is part of SEPIA.

    SEPIA is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    SEPIA is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with SEPIA.  If not, see <http://www.gnu.org/licenses/>.
 */
//package edu.cwru.sepia.agent;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.util.Direction;

/**
 * This agent will first collect gold to produce a peasant, then the two
 * peasants will collect gold and wood separately until reach goal.
 *
 * @author Feng
 *
 */
public class SearchAgent extends Agent {
    private static final long serialVersionUID = -4047208702628325380L;

    private int step;

    public SearchAgent(int playernum, String[] arguments) {
        super(playernum);
    }

    List<GraphNode> path = new ArrayList<GraphNode>();
    StateView currentState;
    UnitView footman = null;
    UnitView townhall = null;
    GraphNode footmanPosition = null;

    // calculate the path to the town hall using A* with the Chebyshev distance as a heuristic
    private List<GraphNode> getPathToTownHall(StateView currentState, UnitView footman, UnitView townhall) {

        // Create the map, leaving occupied cells blank.
        GraphNode nodeMap[][] = new GraphNode[currentState.getXExtent()][currentState.getYExtent()];
        for (int i = 0; i < currentState.getXExtent(); i++) {
            for (int j = 0; j < currentState.getYExtent(); j++) {
                if (!currentState.isUnitAt(i, j) && !currentState.isResourceAt(i, j)) {
                    nodeMap[i][j] = new GraphNode(i, j);
                }
            }
        }

        // Get the nodes containing the our target and origin.
        GraphNode initial = new GraphNode(footman.getXPosition(), footman.getYPosition());
        GraphNode target = new GraphNode(townhall.getXPosition(), townhall.getYPosition());
        return getPathToTarget(nodeMap, initial, target);
    }

    private List<GraphNode> getPathToTarget(GraphNode map[][], GraphNode initial, GraphNode target) {
        WeightedNode.target = new GraphNode(target.x, target.y);
        WeightedNode current = new WeightedNode(initial, null);

        // Search for the town hall.
        List<WeightedNode> openSet = new ArrayList<>();
        List<GraphNode> closedSet = new ArrayList<>();
//        System.out.println("Moving to node: " + current);

        while (true) {
            openSet.remove(current);
            List<GraphNode> adjacent = getAdjacentNodes(map, current.getNode(), closedSet);

            // Find the adjacent node with the lowest heuristic cost.
            for (GraphNode node : adjacent) {
//                System.out.println("\tAdjacent Node: " + current);
                openSet.add(new WeightedNode(node, current));
            }

            // Exit search if done.
            if (openSet.isEmpty()) {
                System.out.printf("Target (%d, %d) is unreachable from position (%d, %d).\n",
                                target.x, target.y, initial.x, initial.y);
                return null;
            } else if (targetAdjacent(current)) {
                break;
            }

            // This node has been explored now.
            closedSet.add(current.getNode());

            // Find the next open node with the lowest cost.
            WeightedNode next = openSet.get(0);
            for (WeightedNode node : openSet) {
                if (node.getCost() < next.getCost()) {
                    next = node;
                }
            }
//            System.out.println("Moving to node: " + current);
            current = next;
        }

        // Collect the path.
        List<GraphNode> path = new ArrayList<>();
        while (current.getParent() != null) {
            path.add(0, current.getNode());
            current = current.getParent();
        }

        return path;
    }

    // Returns true if the target is adjacent to the given node
    private boolean targetAdjacent(WeightedNode current) {
        int x = current.getNode().x;
        int y = current.getNode().y;
        GraphNode target = WeightedNode.target;
//        System.out.printf("Target is (%d, %d), currently at (%d, %d).\n",
//                target.x, target.y, x, y);
        for (int i = x - 1; i <= x + 1; i++) {
            for (int j = y - 1; j <= y + 1; j++) {
                if (i == target.x && j == target.y) {
//                    System.out.printf("Target is adjacent!");
                    return true;
                }
            }
        }
//        System.out.printf("Target is not adjacent.");
        return false;
    }

    // Returns all nodes that are reachable and adjacent to the current node
    private List<GraphNode> getAdjacentNodes(GraphNode map[][], GraphNode current, List<GraphNode> visited) {
        int x = current.x;
        int y = current.y;
//        System.out.printf("Searching for nodes adjacent to (%d, %d).\n", x, y);
        List<GraphNode> nodes = new ArrayList<>();
        for (int i = Math.max(x - 1, 0); i < Math.min(x + 2, map.length); i++) {
            for (int j = Math.max(y - 1, 0); j < Math.min(y + 2, map[i].length); j++) {
                if (i == x && j == y) {
                    continue;
                }
                if (map[i][j] == null) {
//                    System.out.printf("Node (%d, %d) is occupied.\n", i, j);
                    continue;
                }
                if (visited.contains(map[i][j])) {
//                    System.out.printf("Node (%d, %d) is already visited.\n", i, j);
                    continue;
                }
//                System.out.printf("Node (%d, %d) is valid and adjacent!\n", i, j);
                nodes.add(map[i][j]);
            }
        }
        return nodes;
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
        } else if (x == 1 && y == 1) {
            return Direction.SOUTHEAST;
        } else {
            System.out.println("Something bad happened while calculating direction");
            return null;
        }
    }

    @Override
    public Map<Integer, Action> initialStep(StateView newstate,
            History.HistoryView statehistory) {
        step = 0;
        currentState = newstate;

        // Find the footman and town hall
        List<Integer> unitIds = currentState.getAllUnitIds();
        List<Integer> townhallIds = new ArrayList<Integer>();
        List<Integer> footmanIds = new ArrayList<Integer>();
        for (int i = 0; i < unitIds.size(); i++) {
            int id = unitIds.get(i);
            UnitView unit = currentState.getUnit(id);
            String unitTypeName = unit.getTemplateView().getName();
            if (unitTypeName.equals("TownHall"))
                townhallIds.add(id);
            if (unitTypeName.equals("Footman"))
                footmanIds.add(id);
        }

        footman = currentState.getUnit(footmanIds.get(0));
        townhall = currentState.getUnit(townhallIds.get(0));
        
        footmanPosition = new GraphNode(footman.getXPosition(), footman.getYPosition());

        // find the path to the town hall
        path = getPathToTownHall(currentState, footman, townhall);
        
        if (path == null || path.isEmpty()) {
        	System.out.printf("Target (%d, %d) is unreachable from position (%d, %d).\n",
        			townhall.getXPosition(), townhall.getYPosition(), footman.getXPosition(), footman.getYPosition());
        	System.exit(0);
        }
        
        System.out.println("Path Nodes:");
        for (GraphNode node : path) {
            System.out.println(node.x + "," + node.y);
        }

        return middleStep(newstate, statehistory);
    }

    @Override
    public Map<Integer, Action> middleStep(StateView newState, History.HistoryView statehistory) {
        step++;

        Map<Integer, Action> builder = new HashMap<Integer, Action>();
        currentState = newState;
        
        // We have reached the destination!
        if (path.size() == 0) {
        	System.out.println("Attacking!");
        	Action b = Action.createPrimitiveAttack(footman.getID(), townhall.getID());
        	builder.put(footman.getID(), b);
        	return builder;
        }
        
        GraphNode nextNode = path.remove(0);
        
//        System.out.println("Footman is at " + footmanPosition.x + "," + footmanPosition.y);
//        System.out.println("Footman is moving to " + nextNode.x + "," + nextNode.y);
        
        // Find the direction to the next square on the map
        Direction direction = getDirection(nextNode.x - footmanPosition.x, nextNode.y - footmanPosition.y);
        System.out.println("Moving: " + direction);
        
        Action b = Action.createPrimitiveMove(footman.getID(), direction);
        builder.put(footman.getID(), b);
        
        footmanPosition.x = nextNode.x;
        footmanPosition.y = nextNode.y;

        return builder;
    }

    @Override
    public void terminalStep(StateView newstate, History.HistoryView statehistory) {
        step++;
    }

    public static String getUsage() {
        return "Naviagtes a footman to an enemy town hall";
    }

    @Override
    public void savePlayerData(OutputStream os) {
        // this agent lacks learning and so has nothing to persist.
    }

    @Override
    public void loadPlayerData(InputStream is) {
        // this agent lacks learning and so has nothing to persist.
    }
}

