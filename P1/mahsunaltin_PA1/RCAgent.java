package Agent;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.transform.Templates;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionFeedback;
import edu.cwru.sepia.action.ActionResult;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.ResourceNode.Type;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Template;
import edu.cwru.sepia.environment.model.state.Template.TemplateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.environment.model.state.UnitTemplate;
import edu.cwru.sepia.experiment.Configuration;
import edu.cwru.sepia.experiment.ConfigurationValues;
import edu.cwru.sepia.agent.Agent;

public class RCAgent extends Agent {
	private static final long serialVersionUID = -4047208702628325380L;
	private static final Logger logger = Logger.getLogger(RCAgent.class.getCanonicalName());

	private int goldRequired;
	private int woodRequired;

	private int step;

	public RCAgent(int playernum, String[] arguments) {
		super(playernum);

		goldRequired = Integer.parseInt(arguments[0]);
		woodRequired = Integer.parseInt(arguments[1]);
	}

	StateView currentState;
	private boolean isFarm = false;
	private boolean isBarracks = false;
	private boolean isFootmen = false;

	@Override
	public Map<Integer, Action> initialStep(StateView newstate, History.HistoryView statehistory) {
		step = 0;
		return middleStep(newstate, statehistory);
	}

	@Override
	public Map<Integer, Action> middleStep(StateView newState, History.HistoryView statehistory) {
		step++;
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("=> Step: " + step);
		}

		Map<Integer, Action> builder = new HashMap<Integer, Action>();
		currentState = newState;

		int currentGold = currentState.getResourceAmount(0, ResourceType.GOLD);
		int currentWood = currentState.getResourceAmount(0, ResourceType.WOOD);

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Current Gold: " + currentGold);
		}
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Current Wood: " + currentWood);
		}

		List<Integer> allUnitIds = currentState.getAllUnitIds();
		List<Integer> peasantIds = new ArrayList<Integer>();
		List<Integer> townhallIds = new ArrayList<Integer>();
		List<Integer> barracksIds = new ArrayList<Integer>();
		List<Integer> footmenIds = new ArrayList<Integer>();

		for (int i = 0; i < allUnitIds.size(); i++) {
			int id = allUnitIds.get(i);
			UnitView unit = currentState.getUnit(id);
			String unitTypeName = unit.getTemplateView().getName();
			if (unitTypeName.equals("TownHall"))
				townhallIds.add(id);
			if (unitTypeName.equals("Peasant"))
				peasantIds.add(id);
			if (unitTypeName.equals("Barracks"))
				barracksIds.add(id);
			if (unitTypeName.equals("Footman"))
				footmenIds.add(id);
		}

		List<Integer> enemyUnitIDs = currentState.getUnitIds(1);
		List<Integer> enemyIds = new ArrayList<Integer>();

		for (int i = 0; i < enemyUnitIDs.size(); i++) {
			int id = enemyUnitIDs.get(i);
			UnitView unit = currentState.getUnit(id);
			String unitTypeName = unit.getTemplateView().getName();
			if (unitTypeName.equals("Footman"))
				enemyIds.add(id);
		}

		if (peasantIds.size() >= 3) { // collect resources

			if (currentGold < 1200) {

				// First peasant is working here.
				int peasantId = peasantIds.get(0);
				int townhallId = townhallIds.get(0);
				Action b = null;
				if (currentState.getUnit(peasantId).getCargoType() == ResourceType.GOLD
						&& currentState.getUnit(peasantId).getCargoAmount() > 0)
					b = new TargetedAction(peasantId, ActionType.COMPOUNDDEPOSIT, townhallId);
				else {
					List<Integer> resourceIds = currentState.getResourceNodeIds(Type.GOLD_MINE);
					b = new TargetedAction(peasantId, ActionType.COMPOUNDGATHER, resourceIds.get(0));
				}
				builder.put(peasantId, b);

				// Second peasant is working here.
				int peasantId1 = peasantIds.get(1);
				int townhallId1 = townhallIds.get(0);
				Action b1 = null;
				if (currentState.getUnit(peasantId1).getCargoType() == ResourceType.GOLD
						&& currentState.getUnit(peasantId1).getCargoAmount() > 0)
					b1 = new TargetedAction(peasantId1, ActionType.COMPOUNDDEPOSIT, townhallId1);
				else {
					List<Integer> resourceIds = currentState.getResourceNodeIds(Type.GOLD_MINE);
					b1 = new TargetedAction(peasantId1, ActionType.COMPOUNDGATHER, resourceIds.get(0));
				}
				builder.put(peasantId1, b1);
			}

			if (currentWood < 400) {
				// Third peasant is working here.
				int peasantId = peasantIds.get(2);
				int townhallId = townhallIds.get(0);
				Action b = null;
				if (currentState.getUnit(peasantId).getCargoAmount() > 0)
					b = new TargetedAction(peasantId, ActionType.COMPOUNDDEPOSIT, townhallId);
				else {
					List<Integer> resourceIds = currentState.getResourceNodeIds(Type.TREE);
					b = new TargetedAction(peasantId, ActionType.COMPOUNDGATHER, resourceIds.get(0));
				}
				builder.put(peasantId, b);
			}

			if (currentGold >= 500 && currentWood >= 250 && isFarm == false) {
				int peasantId = peasantIds.get(2);
				builder.put(peasantId, Action.createCompoundBuild(peasantId,
						currentState.getTemplate(playernum, "Farm").getID(), 5, 5));
				isFarm = true;
			}

			if (currentGold >= 700 && currentWood >= 400 && isBarracks == false) {
				int peasantId = peasantIds.get(2);
				builder.put(peasantId, Action.createCompoundBuild(peasantId,
						currentState.getTemplate(playernum, "Barracks").getID(), 6, 5));
				isBarracks = true;
			}

			if (footmenIds.size() < 3 && currentGold >= 1200) {
				builder.put(barracksIds.get(0), Action.createCompoundProduction(barracksIds.get(0),
						currentState.getTemplate(playernum, "Footman").getID()));
			}

			if (footmenIds.size() >= 3) {
				for (ActionResult feedback : statehistory.getCommandFeedback(playernum, currentState.getTurnNumber() - 1).values()) {
					if (feedback.getFeedback() != ActionFeedback.INCOMPLETE) {
						int unitID = feedback.getAction().getUnitId();
						builder.put(unitID, Action.createCompoundAttack(unitID, enemyUnitIDs.get(0)));
					}
				}
			}

		} else { // build peasant
			if (currentGold >= 400) {
				// System.out.println("Building peasant");
				if (logger.isLoggable(Level.FINE)) {
					logger.fine("already have enough gold to produce a new peasant.");
				}
				TemplateView peasanttemplate = currentState.getTemplate(playernum, "Peasant");
				int peasanttemplateID = peasanttemplate.getID();
				if (logger.isLoggable(Level.FINE)) {
					logger.fine(String.valueOf(peasanttemplate.getID()));
				}
				int townhallID = townhallIds.get(0);
				builder.put(townhallID, Action.createCompoundProduction(townhallID, peasanttemplateID));
			} else {
				// System.out.println("Collecting gold");
				int peasantId = peasantIds.get(0);
				int townhallId = townhallIds.get(0);
				Action b = null;
				if (currentState.getUnit(peasantId).getCargoType() == ResourceType.GOLD
						&& currentState.getUnit(peasantId).getCargoAmount() > 0) {
					b = new TargetedAction(peasantId, ActionType.COMPOUNDDEPOSIT, townhallId);
				} else {
					List<Integer> resourceIds = currentState.getResourceNodeIds(Type.GOLD_MINE);
					b = new TargetedAction(peasantId, ActionType.COMPOUNDGATHER, resourceIds.get(0));
				}
				builder.put(peasantId, b);
			}
		}
		return builder;
	}

	@Override
	public void terminalStep(StateView newstate, History.HistoryView statehistory) {
		step++;
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("=> Step: " + step);
		}

		int currentGold = newstate.getResourceAmount(0, ResourceType.GOLD);
		int currentWood = newstate.getResourceAmount(0, ResourceType.WOOD);

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Current Gold: " + currentGold);
		}
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Current Wood: " + currentWood);
		}
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Congratulations! You have finished the task!");
		}
	}

	public static String getUsage() {
		return "Two arguments, amount of gold to gather and amount of wood to gather";
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
