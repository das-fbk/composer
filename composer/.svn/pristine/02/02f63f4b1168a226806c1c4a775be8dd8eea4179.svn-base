package eu.fbk.das.composer.impl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.fbk.das.composer.api.ComposerInterface;
import eu.fbk.das.composer.api.CompositionProblem;
import eu.fbk.das.composer.api.CompositionStatus;
import eu.fbk.das.composer.api.DotParser;
import eu.fbk.das.composer.api.elements.AbstractGoal;
import eu.fbk.das.composer.api.elements.Effect;
import eu.fbk.das.composer.api.elements.ObjectEvent;
import eu.fbk.das.composer.api.elements.Precondition;
import eu.fbk.das.composer.api.elements.ServiceAction;
import eu.fbk.das.composer.api.elements.SyncPoint;
import eu.fbk.das.process.engine.api.domain.ObjectDiagram;
import eu.fbk.das.process.engine.api.domain.ObjectTransition;
import eu.fbk.das.process.engine.api.domain.ServiceDiagram;
import eu.fbk.das.process.engine.api.domain.ServiceDiagramActionType;
import eu.fbk.das.process.engine.api.domain.ServiceTransition;

/**
 * Composer generic implementation
 * 
 * {@link ComposerInterface} {@link CompositionProblem}
 * {@link CompositionStatus}
 */
public class Composer implements ComposerInterface {

    private static final String LINE_SEPARATOR = System
	    .getProperty("file.separator");

    private static final String COMPOSITIONS_DIR = "compositions";

    public static final Map<CompositionStatus, String> statusMessages = new HashMap<CompositionStatus, String>();

    private static int cCounter = 0;

    private static final Logger logger = LogManager.getLogger(Composer.class);

    private static final String MAC_WINE_PATH = "macWinePath";

    private static final String MAC_WSYNTH_PATH = "macwsynthPath";

    static boolean useFaultEventsAndActions = true;

    static {
	statusMessages.put(CompositionStatus.OK, "OK");
	statusMessages.put(CompositionStatus.NO_GOAL, "No goals identified");
	statusMessages.put(CompositionStatus.PLAN_NOT_FOUND, "Plan not found");
	statusMessages.put(CompositionStatus.SMV_INCORRECT,
		"Errors in generated SMV");
    }

    private static int getNextId() {
	return cCounter++;
    }

    private String folderPath;

    private Properties properties;

    public Composer(String workPathname) {
	this.folderPath = workPathname;
	this.properties = new Properties();
	try {
	    properties.load(getClass().getResourceAsStream(
		    "configuration.properties"));
	} catch (IOException e) {
	    logger.error(e.getMessage(), e);
	}
    }

    /**
     * @param problem
     * @param outputName
     * @param showImprobable
     *            indicates if plan contains improbable non-deterministic
     *            actions
     * @param captevoHome
     * @return
     */
    @Override
    public CompositionStatus compose(CompositionProblem problem,
	    String outputName, boolean showImprobable, boolean optimize,
	    String captevoHome) {

	if (problem.getSyncPoints().isEmpty()
		&& problem.getNextActions().isEmpty())
	    return CompositionStatus.NO_GOAL;

	if (optimize) {
	    logger.info("Optimization started");
	    long startTime = System.currentTimeMillis();

	    // Optimization
	    problem = optimize(problem);

	    long endTime = System.currentTimeMillis();
	    logger.debug("Optimization time: "
		    + ((float) (endTime - startTime)) / 1000 + " seconds\n");
	}

	logger.debug("Generating SMV...");
	long startTimeGen = System.currentTimeMillis();
	generateSmv(outputName, problem, showImprobable, captevoHome);
	long endTimeGen = System.currentTimeMillis();
	logger.debug("SMV generation time: "
		+ ((float) (endTimeGen - startTimeGen)) / 1000 + " seconds\n");

	logger.debug("Composing...");
	long startTimeComp = System.currentTimeMillis();
	CompositionStatus cs = composeFromSmv(outputName, captevoHome);
	long endTimeComp = System.currentTimeMillis();
	System.out
		.println("Composition time: "
			+ ((float) (endTimeComp - startTimeComp)) / 1000
			+ " seconds\n");

	if (cs != CompositionStatus.OK) {
	    System.err.println("PROBLEM!\n");
	    return cs;
	}
	logger.debug("Processing output process...");
	long startTimeProc = System.currentTimeMillis();
	DotParser.buildProcess(getFolderPath() + LINE_SEPARATOR
		+ COMPOSITIONS_DIR + LINE_SEPARATOR + outputName
		+ LINE_SEPARATOR + outputName + ".dot");
	long endTimeProc = System.currentTimeMillis();
	System.out
		.println("Procession time: "
			+ ((float) (endTimeProc - startTimeProc)) / 1000
			+ " seconds\n");

	return CompositionStatus.OK;
    }

    @Override
    public String getFolderPath() {
	return folderPath;
    }

    @Override
    public String getStatusMessage(CompositionStatus cs) {
	return statusMessages.get(cs);
    }

    /**
     * We optimize the composition problem to improve the performance of the
     * composer. The optimization is not complete and we will probably improve
     * it in the future
     * 
     * @param cp
     *            composition problem to be optimized
     * @return composition problem optimized
     */

    @Override
    public CompositionProblem optimize(CompositionProblem cp) {
	List<ServiceDiagram> sds = new ArrayList<ServiceDiagram>(
		cp.getServiceDiagrams());
	List<ObjectDiagram> ods = new ArrayList<ObjectDiagram>(
		cp.getObjectDiagrams());
	List<ServiceAction> gis = new ArrayList<ServiceAction>(
		cp.getGlobalInputs());
	List<ServiceAction> gos = new ArrayList<ServiceAction>(
		cp.getGlobalOutputs());
	List<Precondition> ps = new ArrayList<Precondition>(
		cp.getPreconditions());
	List<Effect> es = new ArrayList<Effect>(cp.getEffects());
	List<ServiceAction> nas = new ArrayList<ServiceAction>(
		cp.getNextActions());
	Map<SyncPoint, Integer> sps = new HashMap<SyncPoint, Integer>(
		cp.getSyncPoints());
	// fix point indicator
	boolean isUpdated = true;
	while (isUpdated) {
	    isUpdated = false;
	    // We remove redundant effects for which there are no actions to
	    // trigger
	    Set<Effect> aeff = new HashSet<Effect>();
	    List<ServiceAction> actions = new ArrayList<ServiceAction>();
	    actions.addAll(gis);
	    actions.addAll(gos);
	    for (Effect eff : es) {
		boolean isAcc = false;
		for (ServiceAction sa : actions) {
		    if (sa.getSid().equals(eff.getSid())
			    && sa.getAction().equals(eff.getAction())) {
			isAcc = true;
			break;
		    }
		}
		if (isAcc)
		    aeff.add(eff);
	    }
	    isUpdated |= es.retainAll(aeff);

	    // We remove all object transitions that cannot be triggered
	    // We find all reachable states for each object diagram
	    // ae - events that are possible in the current state of the
	    // composition environment

	    // For abstract activities, we consider all states that are
	    // mentioned in abstract goals as reachable until corresponding
	    // actions are active

	    Set<ObjectEvent> ae = new HashSet<ObjectEvent>();
	    for (ObjectDiagram obj : ods) {
		Set<String> accessibleSt = new HashSet<String>();
		accessibleSt.add(obj.getCurrentState());

		// Here we calcuate all object states that are reachable
		// through active abstract transitions

		List<String> reachableAbstract = new ArrayList<String>();

		for (AbstractGoal ag : cp.getaGoals()) {
		    boolean isAbActive = false;
		    for (ServiceAction sa : gis) {
			if (sa.getSid().equals(ag.getSid())
				&& sa.getAction().equals(ag.getAction())) {
			    isAbActive = true;
			    break;
			}
		    }
		    if (isAbActive)
			for (Map<String, List<String>> point : ag
				.getOid2states()) {
			    if (point.get(obj.getOid()) != null)
				reachableAbstract
					.addAll(point.get(obj.getOid()));
			}
		}
		if (!reachableAbstract.isEmpty())
		    accessibleSt.addAll(reachableAbstract);

		Set<ObjectTransition> accessibleTr = new HashSet<ObjectTransition>();

		// We remove all object transitions that cannot be triggered
		for (ObjectTransition tr : obj.getTransitions()) {
		    boolean isAcc = false;
		    for (Effect eff : es) {
			if (eff.getOid().equals(obj.getOid())
				&& eff.getEvent().equals(tr.getEvent())) {
			    isAcc = true;
			    break;
			}
		    }
		    if (isAcc)
			accessibleTr.add(tr);
		}
		isUpdated |= obj.getTransitions().retainAll(accessibleTr);

		// here we calculate reachable states
		boolean isUpd = true;
		while (isUpd) {
		    isUpd = false;
		    for (ObjectTransition tr : obj.getTransitions()) {
			if (accessibleSt.contains(tr.getFrom())) {
			    if (accessibleSt.add(tr.getTo()))
				isUpd = true;
			}
		    }
		}

		accessibleTr = new HashSet<ObjectTransition>();
		Set<String> accessibleEvents = new HashSet<String>();
		for (ObjectTransition tr : obj.getTransitions()) {
		    if (accessibleSt.contains(tr.getFrom())) {
			accessibleTr.add(tr);
			accessibleEvents.add(tr.getEvent());
			ae.add(new ObjectEvent(obj.getOid(), tr.getEvent()));
		    }
		}

		isUpdated |= obj.getStates().retainAll(accessibleSt);
		isUpdated |= obj.getInitialStates().retainAll(accessibleSt);
		isUpdated |= obj.getEvents().retainAll(accessibleEvents);
		isUpdated |= obj.getTransitions().retainAll(accessibleTr);

	    }

	    // For each object diagram we perform the reachability analysis that
	    // removes all states where
	    // the goal is unsat
	    if (nas.isEmpty())
		for (ObjectDiagram obj : ods) {
		    Set<String> satSt = new HashSet<String>();

		    // Here we calcuate all object states that are reachable
		    // through active abstract transitions

		    List<String> reachableAbstract = new ArrayList<String>();

		    for (AbstractGoal ag : cp.getaGoals()) {
			boolean isAbActive = false;
			for (ServiceAction sa : gis) {
			    if (sa.getSid().equals(ag.getSid())
				    && sa.getAction().equals(ag.getAction())) {
				isAbActive = true;
				break;
			    }
			}
			if (isAbActive)
			    for (Map<String, List<String>> point : ag
				    .getOid2states()) {
				if (point.get(obj.getOid()) != null)
				    reachableAbstract.addAll(point.get(obj
					    .getOid()));
			    }
		    }

		    // complete reachability variable
		    boolean fullR = false;
		    for (SyncPoint sp : sps.keySet()) {
			if (sp.getOid2state().get(obj.getOid()) == null
				|| sp.getOid2state().get(obj.getOid())
					.isEmpty()) {
			    fullR = true;
			    satSt.addAll(obj.getStates());
			    break;
			} else {
			    satSt.addAll(sp.getOid2state().get(obj.getOid()));
			}
		    }

		    // if at least one goal state is reachable through abstract
		    // action
		    // goal can be reached from any state
		    reachableAbstract.retainAll(satSt);
		    if (!reachableAbstract.isEmpty())
			satSt.addAll(obj.getStates());
		    else {
			// reachability analysis
			if (!fullR) {
			    boolean fix = false;
			    while (!fix) {
				fix = true;
				for (ObjectTransition tr : obj.getTransitions()) {
				    if (satSt.contains(tr.getTo())) {
					fix &= !(satSt.add(tr.getFrom()));
				    }
				}
			    }
			}
		    }

		    Set<ObjectTransition> accessibleTr = new HashSet<ObjectTransition>();
		    Set<String> accessibleEvents = new HashSet<String>();
		    for (ObjectTransition tr : obj.getTransitions()) {
			if (satSt.contains(tr.getTo())) {
			    accessibleTr.add(tr);
			    accessibleEvents.add(tr.getEvent());
			    ae.add(new ObjectEvent(obj.getOid(), tr.getEvent()));
			}
		    }

		    isUpdated |= obj.getStates().retainAll(satSt);
		    isUpdated |= obj.getInitialStates().retainAll(satSt);
		    isUpdated |= obj.getEvents().retainAll(accessibleEvents);
		    isUpdated |= obj.getTransitions().retainAll(accessibleTr);
		}

	    // Remove all effects that cannot really affect objects
	    // (corresponding transitions
	    // are unreachable)
	    // aeff - accessible effects
	    aeff = new HashSet<Effect>();
	    for (Effect eff : es) {
		boolean isAcc = false;
		for (ObjectEvent event : ae) {
		    if (event.getOid().equals(eff.getOid())
			    && event.getEvent().equals(eff.getEvent())) {
			isAcc = true;
			break;
		    }
		}
		if (isAcc)
		    aeff.add(eff);
	    }
	    isUpdated |= es.retainAll(aeff);

	    // We identify active services (i.e. services that still can cause
	    // effects)
	    // IMPORTANT: if a service is a fragment with abstract activities,
	    // it cannot be removed!
	    List<ServiceDiagram> activeServices = new ArrayList<ServiceDiagram>();
	    List<ServiceAction> activeInputs = new ArrayList<ServiceAction>();
	    List<ServiceAction> activeOutputs = new ArrayList<ServiceAction>();
	    for (ServiceDiagram sd : sds) {
		boolean isActive = false;
		if (sd.getAbstracts() != null && !sd.getAbstracts().isEmpty())
		    isActive = true;
		Set<String> allActions = new HashSet<String>();
		allActions.addAll(sd.getInputs());
		allActions.addAll(sd.getOutputs());
		for (String action : allActions) {
		    for (Effect eff : es) {
			if (eff.getSid().equals(sd.getSid())
				&& eff.getAction().equals(action)) {
			    isActive = true;
			    break;
			}
		    }
		}
		if (isActive) {
		    activeServices.add(sd);
		    for (ServiceAction sa : gos)
			if (sa.getSid().equals(sd.getSid())) {
			    activeOutputs.add(sa);
			}
		    for (ServiceAction sa : gis)
			if (sa.getSid().equals(sd.getSid())) {
			    activeInputs.add(sa);
			}
		}
	    }
	    isUpdated |= sds.retainAll(activeServices);
	    isUpdated |= gis.retainAll(activeInputs);
	    isUpdated |= gos.retainAll(activeOutputs);

	    // Here we remove preconditions for non-existing actions
	    // We also collect service actions that cannot be executed becuase
	    // one of its preconditions is unsatisfiable

	    List<ServiceAction> blockedActions = new ArrayList<ServiceAction>();
	    List<Precondition> accPr = new ArrayList<Precondition>();
	    for (Precondition pr : ps) {
		boolean isActive = false;
		for (ServiceAction sa : gis) {
		    if (sa.getSid().equals(pr.getSid())
			    && sa.getAction().equals(pr.getAction())) {
			isActive = true;
			break;
		    }
		}
		if (!isActive)
		    for (ServiceAction sa : gos) {
			if (sa.getSid().equals(pr.getSid())
				&& sa.getAction().equals(pr.getAction())) {
			    isActive = true;
			    break;
			}
		    }

		// Here we remove unsatisfiable preconditions and mark
		// corresponding actions as blocked
		List<Map<String, List<String>>> pointsToRemove = new ArrayList<Map<String, List<String>>>();
		for (Map<String, List<String>> point : pr.getOid2states()) {

		    // Here we remove all unreachable states from the
		    // precondition
		    for (String oid : point.keySet()) {
			ObjectDiagram od = null;
			for (ObjectDiagram odd : ods) {
			    if (odd.getOid().equals(oid)) {
				od = odd;
				break;
			    }
			}
			point.get(oid).retainAll(od.getStates());
			// if the list of states of a certain object is empty
			// then this point is unreachable and can be removed
			// from the precondition
			if (point.get(oid).isEmpty()) {
			    pointsToRemove.add(point);
			    break;
			}
		    }
		}
		pr.getOid2states().removeAll(pointsToRemove);

		if (pr.getOid2states().isEmpty()) {
		    blockedActions.add(new ServiceAction(pr.getSid(), pr
			    .getAction()));
		    isActive = false;
		}
		// ----------------------------------------------

		if (isActive) {
		    accPr.add(pr);
		}

	    }
	    isUpdated |= ps.retainAll(accPr);

	    // removing blocked actions from services and further reachability
	    // analysis

	    for (ServiceDiagram sd : sds) {
		Set<String> blocked = new HashSet<String>();
		Set<String> allActions = new HashSet<String>();
		allActions.addAll(sd.getInputs());
		allActions.addAll(sd.getOutputs());
		for (String action : allActions) {
		    boolean isBlocked = false;
		    for (ServiceAction ba : blockedActions) {
			if (ba.getSid().equals(sd.getSid())
				&& ba.getAction().equals(action)) {
			    isBlocked = true;
			    break;
			}
		    }
		    if (isBlocked)
			blocked.add(action);
		}
		isUpdated |= sd.getInputs().removeAll(blocked);
		isUpdated |= sd.getOutputs().removeAll(blocked);

		// removing transitions
		Set<ServiceTransition> blockedTr = new HashSet<ServiceTransition>();
		for (ServiceTransition tr : sd.getTransitions()) {
		    boolean isBlocked = false;
		    for (ServiceAction ba : blockedActions) {
			if (ba.getSid().equals(sd.getSid())
				&& ba.getAction().equals(tr.getAction())) {
			    isBlocked = true;
			    break;
			}
		    }
		    if (isBlocked)
			blockedTr.add(tr);
		}

		isUpdated |= sd.getTransitions().removeAll(blockedTr);
	    }

	    // removing blocked actions from global
	    Set<ServiceAction> allActions = new HashSet<ServiceAction>();
	    allActions.addAll(gis);
	    allActions.addAll(gos);
	    Set<ServiceAction> blocked = new HashSet<ServiceAction>();
	    for (ServiceAction sa : allActions) {
		for (ServiceAction ba : blockedActions) {
		    if (ba.getSid().equals(sa.getSid())
			    && ba.getAction().equals(sa.getAction())) {
			blocked.add(sa);
		    }
		}
	    }
	    isUpdated |= gis.removeAll(blocked);
	    isUpdated |= gos.removeAll(blocked);

	    // We find all reachable states for each service diagram
	    // aa - actions that are possible in the current state of the
	    // composition environment
	    List<ServiceAction> aa = new ArrayList<ServiceAction>();
	    for (ServiceDiagram sd : sds) {
		Set<String> accessibleSt = new HashSet<String>();
		accessibleSt.add(sd.getCurrentState());
		Set<ServiceTransition> accessibleTr = new HashSet<ServiceTransition>();

		// here we calculate reachable states
		boolean isUpd = true;
		while (isUpd) {
		    isUpd = false;
		    for (ServiceTransition tr : sd.getTransitions()) {
			if (accessibleSt.contains(tr.getFrom())) {
			    if (accessibleSt.add(tr.getTo()))
				isUpd = true;
			}
		    }
		}

		accessibleTr = new HashSet<ServiceTransition>();
		Set<String> accessibleActions = new HashSet<String>();
		for (ServiceTransition tr : sd.getTransitions()) {
		    if (accessibleSt.contains(tr.getFrom())) {
			accessibleTr.add(tr);
			accessibleActions.add(tr.getAction());
			aa.add(new ServiceAction(sd.getSid(), tr.getAction()));
		    }
		}

		isUpdated |= sd.getStates().retainAll(accessibleSt);
		isUpdated |= sd.getInitialStates().retainAll(accessibleSt);
		isUpdated |= sd.getInputs().retainAll(accessibleActions);
		isUpdated |= sd.getOutputs().retainAll(accessibleActions);
		isUpdated |= sd.getTransitions().retainAll(accessibleTr);
	    }

	    List<ServiceAction> aIn = new ArrayList<ServiceAction>();
	    List<ServiceAction> aOut = new ArrayList<ServiceAction>();

	    for (ServiceAction action : gis) {
		boolean isAcc = false;
		for (ServiceAction act : aa)
		    if (action.getSid().equals(act.getSid())
			    && action.getAction().equals(act.getAction())) {
			isAcc = true;
			break;
		    }
		if (isAcc)
		    aIn.add(action);
	    }
	    isUpdated |= gis.retainAll(aIn);
	    for (ServiceAction action : gos) {
		boolean isAcc = false;
		for (ServiceAction act : aa)
		    if (action.getSid().equals(act.getSid())
			    && action.getAction().equals(act.getAction())) {
			isAcc = true;
			break;
		    }
		if (isAcc)
		    aOut.add(action);
	    }
	    isUpdated |= gos.retainAll(aOut);
	}
	Set<SyncPoint> spToRemove = new HashSet<SyncPoint>();
	for (SyncPoint point : sps.keySet()) {
	    for (String oid : point.getOid2state().keySet()) {
		for (ObjectDiagram od : ods) {
		    if (od.getOid().equals(oid))
			point.getOid2state().get(oid).retainAll(od.getStates());
		    // IF one of the objects will never reach its goal state,
		    // the whole syncPoint is unreachable
		    if (point.getOid2state().get(oid).isEmpty()) {
			spToRemove.add(point);
			break;
		    }
		}
	    }
	}

	for (SyncPoint point : spToRemove) {
	    sps.remove(point);
	}

	// we reorder sync point priorities so that min pr = 0
	// and all priorities between minPr and maxPr are occupied
	int maxPr = 0;
	for (SyncPoint point : sps.keySet()) {
	    if (maxPr < sps.get(point))
		maxPr = sps.get(point);
	}

	int sub = 0;
	for (int i = 0; i <= maxPr; i++) {
	    boolean exists = false;
	    for (SyncPoint point : sps.keySet()) {
		if (sps.get(point) == i) {
		    exists = true;
		    sps.put(point, sps.get(point) - sub);
		}
	    }
	    if (!exists)
		sub++;
	}

	// Updating next action according to optimized domain (removing all next
	// actions which are blocked)
	List<ServiceAction> actions = new ArrayList<ServiceAction>();
	actions.addAll(gis);
	actions.addAll(gos);

	List<ServiceAction> nasToLeave = new ArrayList<ServiceAction>();
	for (ServiceAction na : nas) {
	    boolean toLeave = false;
	    for (ServiceAction sa : actions) {
		if (sa.getSid().equals(na.getSid())
			&& sa.getAction().equals(na.getAction())) {
		    toLeave = true;
		    break;
		}
	    }
	    if (toLeave)
		nasToLeave.add(na);
	}
	nas.retainAll(nasToLeave);

	// Updating list of faults
	List<ServiceAction> newFaults = new ArrayList<ServiceAction>();

	for (ServiceAction sa : cp.getFaults()) {
	    for (ServiceAction oa : gos)
		if (oa.getSid().equals(sa.getSid())
			&& oa.getAction().equals(sa.getAction())) {
		    newFaults.add(sa);
		    break;
		}
	}

	return cp;
    }

    /**
     * generates a large commented title to be added to SMV
     * 
     * @param title
     *            title to be added
     * @return
     */
    private String addLargeTitle(String title) {
	String str = "";
	int c = 2 * title.length() + 9;
	for (int i = 0; i < c; i++) {
	    str += "-";
	}
	str += "\n--   ";
	for (int i = 0; i < title.length(); i++) {
	    str += title.charAt(i) + " ";
	}
	str += "  --\n";
	for (int i = 0; i < c; i++) {
	    str += "-";
	}
	str += "\n";

	return str;
    }

    /**
     * generates a small commented title to be added to SMV
     * 
     * @param title
     * @return
     */
    private String addSmallTitle(String title) {
	return "--	 " + title + "\n";
    }

    /**
     * generates a composition starting from SMV model
     * 
     * @param captevoHome
     * 
     * @return
     */
    private CompositionStatus composeFromSmv(String outputName,
	    String captevoHome) {
	CompositionStatus cp = CompositionStatus.OK;
	String os = System.getProperty("os.name").toLowerCase();
	// creating a command file
	String filename = "compose" + getNextId() + ".cmd";
	FileWriter writer;
	String command = "";
	try {
	    writer = new FileWriter(filename);
	    logger.debug("Current system type: " + os);
	    if (os.indexOf("win") != -1) {
		command = "\""
			+ folderPath
			+ LINE_SEPARATOR
			+ "wsynth.exe\" -model_type dn -out_type dot -algo agaf_then_acyclic_preferences -agaf states -mono -dynamic -reachability_analysis "
			+ "\"" + folderPath + LINE_SEPARATOR + COMPOSITIONS_DIR
			+ LINE_SEPARATOR + outputName + LINE_SEPARATOR
			+ outputName + ".smv\"" + " > \"" + folderPath
			+ LINE_SEPARATOR + COMPOSITIONS_DIR + LINE_SEPARATOR
			+ outputName + LINE_SEPARATOR + outputName + ".dot\"";
		writer.write(command);
		logger.debug("windows command :" + command);
	    } else if ((os.indexOf("nux") != -1)) {
		command = "\""
			+ System.getenv("CAPTEVO_HOME")
			+ LINE_SEPARATOR
			+ "wsynth\" -model_type dn -out_type dot -algo agaf_then_acyclic_preferences -agaf states -mono -dynamic -reachability_analysis "
			+ "\"" + folderPath + outputName + LINE_SEPARATOR
			+ outputName + ".smv\"" + " >" + "\"" + folderPath
			+ outputName + LINE_SEPARATOR + outputName + ".dot\"";
		logger.debug("*nux command :" + command);
		writer.write(command);

	    } else if (os.indexOf("mac") != -1) {
		// todo
		command = properties.getProperty(MAC_WINE_PATH)
			+ " "
			+ properties.getProperty(MAC_WSYNTH_PATH)
			+ "  -model_type dn -out_type dot -algo agaf_then_acyclic_preferences -agaf states -mono -dynamic -reachability_analysis "
			+ "\"" + folderPath + LINE_SEPARATOR + COMPOSITIONS_DIR
			+ LINE_SEPARATOR + outputName + LINE_SEPARATOR
			+ outputName + ".smv\"" + " > \"" + folderPath
			+ LINE_SEPARATOR + COMPOSITIONS_DIR + LINE_SEPARATOR
			+ outputName + LINE_SEPARATOR + outputName + ".dot\"";
		logger.debug("Mac command :" + command);
		writer.write(command);
	    } else {
		logger.warn("Not supported operating system. current is: " + os
			+ " , supported are windows, mac, *nux");
		writer.close();
		return CompositionStatus.PLAN_NOT_FOUND;
	    }
	    writer.close();
	} catch (IOException e) {
	    logger.error(e);
	    return CompositionStatus.PLAN_NOT_FOUND;

	}

	// Executing planner wsynth.exe
	try {
	    Process p = null;

	    if (os.indexOf("win") != -1) {
		p = Runtime.getRuntime().exec(filename);
	    } else if ((os.indexOf("nux") != -1)) {
		p = Runtime.getRuntime().exec("/bin/bash " + filename);
	    } else if (os.indexOf("mac") != -1) {
		p = Runtime.getRuntime().exec("/bin/sh " + filename);
	    }

	    InputStream stderr = p.getErrorStream();
	    boolean running = true;
	    String output = "";
	    while (running) {
		try {
		    int c = stderr.read();
		    if (c != -1) {
			output += (char) c;
			// System.out.print((char) c);
		    } else
			running = false;
		} catch (IOException e) {
		    logger.error(e.getMessage(), e);
		}
	    }

	    logger.debug("Output comando wsynth - start");
	    logger.debug(output);
	    logger.debug("Output comando wsynth - end ");

	    if (output.contains("Plan not found")) {
		logger.debug(output);
		cp = CompositionStatus.PLAN_NOT_FOUND;
	    }
	    if (output.contains("Syntax error")) {
		logger.debug(output);
		cp = CompositionStatus.SMV_INCORRECT;
	    }
	} catch (IOException e) {
	    logger.error(e);
	}

	File f = new File(filename);
	try {
	    if (f.exists()) {
		FileUtils.forceDelete(f);
	    }
	} catch (IOException e) {
	    logger.error(e.getMessage(), e);
	}
	return cp;
    }

    /**
     * generates an SMV model for the composition problem
     * 
     * @param outputName
     *            is a name of a file where the SMV model will be stored
     * @param problem
     *            is a composition problem to be translated into SMV
     * @param captevoHome
     */
    private void generateSmv(String outputName, CompositionProblem problem,
	    boolean showImprobable, String captevoHome) {
	// main contains all smv generated code
	String main = addLargeTitle("COMPOSITION");

	// Module declaration
	main += "MODULE dn_main\n\n";

	// Input/Output declaration
	main += addLargeTitle("INPUTS");
	main += "IVAR input:\n{\nUNDEF";

	if (!problem.getGlobalInputs().isEmpty())
	    main += ", ";
	else
	    main += "\n";
	main += problem.getGlobalInputs().toString()
		.replaceAll("[\\[,\\]]", "").replaceAll(" ", ", ")
		+ "\n";
	main += "};\n\n";
	main += addLargeTitle("OUTPUTS");
	main += "IVAR output:\n{\nUNDEF";
	if (!problem.getGlobalOutputs().isEmpty())
	    main += ", ";
	else
	    main += "\n";
	main += problem.getGlobalOutputs().toString()
		.replaceAll("[\\[,\\]]", "").replaceAll(" ", ", ")
		+ "\n";
	main += "};\n\n";

	// Services declaration
	main += addLargeTitle("SERVICES");
	for (ServiceDiagram sd : problem.getServiceDiagrams()) {
	    main += addSmallTitle(sd.getSid());
	    main += "VAR "
		    + sd.getSid()
		    + ": {"
		    + sd.getStates().toString().replaceAll("[\\[,\\]]", "")
			    .replaceAll(" ", ", ") + "};\n";
	    if (sd.getStates().size() > 1) {
		main += "ASSIGN\nnext(" + sd.getSid() + ") :=\ncase\n";
		for (ServiceTransition st : sd.getTransitions()) {
		    main += "(" + sd.getSid() + " = " + st.getFrom() + " & ";
		    main += (st.getType() == ServiceDiagramActionType.IN ? "input"
			    : "output")
			    + " = ";
		    main += sd.getSid() + "_" + st.getAction() + "): "
			    + st.getTo() + ";\n";
		}
		main += "1:" + sd.getSid() + ";\nesac;\n";
	    }
	    Set<String> actions = new HashSet<String>();
	    actions.addAll(sd.getInputs());
	    actions.addAll(sd.getOutputs());
	    for (String action : actions) {
		Set<String> states = new HashSet<String>();
		for (ServiceTransition st : sd.getTransitions())
		    if (st.getAction().equals(action))
			states.add(st.getFrom());
		if (!states.isEmpty()) {
		    main += "TRANS ( "
			    + (sd.getInputs().contains(action) ? "input"
				    : "output") + " = " + sd.getSid() + "_"
			    + action + ") -> (";
		    String str = states.toString().replaceAll("[\\[,\\]]", "")
			    .replaceAll(" ", " | " + sd.getSid() + " = ");
		    main += sd.getSid() + " = " + str + ")\n";
		}
	    }
	    main += "\n";
	}

	// Objects
	main += addLargeTitle("OBJECTS");
	for (ObjectDiagram od : problem.getObjectDiagrams()) {
	    main += addSmallTitle(od.getOid());
	    main += "VAR "
		    + od.getOid()
		    + ": {"
		    + od.getStates().toString().replaceAll("[\\[,\\]]", "")
			    .replaceAll(" ", ", ") + "};\n";
	    if (od.getStates().size() > 1) {
		main += "ASSIGN\nnext(" + od.getOid() + ") :=\ncase\n";
		for (ObjectTransition ot : od.getTransitions()) {
		    for (Effect eff : problem.getEffects()) {
			if (eff.getOid().equals(od.getOid())
				&& eff.getEvent().equals(ot.getEvent())) {
			    main += "(" + od.getOid() + " = " + ot.getFrom()
				    + " & ";
			    main += (problem.getService(eff.getSid())
				    .getInputs().contains(eff.getAction()) ? "input"
				    : "output")
				    + " = ";
			    main += eff.getSid() + "_" + eff.getAction();
			    main += "): " + ot.getTo() + ";\n";
			}
		    }
		}

		// Here we analyze and add abstract action effects

		for (AbstractGoal ag : problem.getaGoals()) {
		    List<String> nextStates = new ArrayList<String>();
		    for (Map<String, List<String>> point : ag.getOid2states()) {
			if (point.get(od.getOid()) != null)
			    nextStates.addAll(point.get(od.getOid()));
		    }
		    String str = "(input = " + ag.getSid() + "_"
			    + ag.getAction() + "):{";
		    if (nextStates.isEmpty())
			continue;
		    for (String state : nextStates) {
			str += "(" + state + ")";
		    }

		    main += str.replaceAll("\\)\\(", " , ") + "};\n";
		}

		main += "1:" + od.getOid() + ";\nesac;\n";
	    }
	}
	// Preconditions
	main += addLargeTitle("PRECONDITIONS");
	for (Precondition pr : problem.getPreconditions()) {
	    ServiceDiagram sd = problem.getService(pr.getSid());
	    main += "TRANS ("
		    + (sd.getInputs().contains(pr.getAction()) ? "input"
			    : "output") + " = " + pr.getSid() + "_"
		    + pr.getAction() + ") -> (";

	    String strAll = "";
	    for (Map<String, List<String>> point : pr.getOid2states()) {
		String str = "";
		for (String oid : point.keySet()) {
		    String str2 = "";
		    str2 += "(";
		    for (String state : point.get(oid)) {
			str2 += "(" + oid + " = " + state + ")";
		    }
		    str2 += ")";
		    str += str2.replaceAll("\\)\\(", ") | (");
		}
		strAll += "(" + str.replaceAll("\\)\\(", ") & (") + ")";
	    }
	    main += strAll.replaceAll("\\)\\(", ") | (") + ")\n";
	}

	// Invariants
	main += addLargeTitle("INVARIANTS");
	main += addSmallTitle("Synchronicity");
	main += "TRANS (1-(input = UNDEF)) + (1-(output = UNDEF)) = 1\n\n";

	// Sync variables
	main += addLargeTitle("SYNCHRONIZATION");

	int maxPr = -1;

	for (SyncPoint point : problem.getSyncPoints().keySet()) {
	    if (maxPr < problem.getSyncPoints().get(point))
		maxPr = problem.getSyncPoints().get(point);
	}

	for (int i = 0; i <= maxPr; i++) {
	    String strGen = "";
	    for (SyncPoint sp : problem.getSyncPoints().keySet()) {
		String str = "";
		if (problem.getSyncPoints().get(sp) == i) {
		    str += "(";
		    for (String obj : sp.getOid2state().keySet()) {
			str += "(" + obj + " in " + sp.getOid2state().get(obj)
				+ ")";
		    }
		    str += ")";
		}
		str = str.replaceAll("\\)\\(", ") & (")
			.replaceAll("\\[", "\\{").replaceAll("\\]", "\\}");
		strGen += str;
	    }
	    strGen = main += "DEFINE SYNC" + i + " := "
		    + strGen.replaceAll("\\)\\(", ") | (") + ";\n";
	}

	// We enforce synchronous message exchange
	main += addSmallTitle("Synchronicity enforcement");
	for (ServiceDiagram srv : problem.getServiceDiagrams()) {
	    String str = "";
	    str += "TRANS (" + srv.getSid() + " in ";
	    Set<String> states = new HashSet<String>();
	    for (ServiceTransition trans : srv.getTransitions()) {
		if (trans.getType() == ServiceDiagramActionType.OUT)
		    states.add(trans.getFrom());
	    }
	    if (states.isEmpty())
		break;
	    else
		for (String state : states) {
		    str += "{" + state + "}";
		}
	    main += str.replaceAll("\\}\\{", ", ");
	    main += ") -> (next(" + srv.getSid() + ") != " + srv.getSid()
		    + ")\n";
	}

	// If we ask the composer not to show improbable actions they are
	// disabled in the domain
	if (!showImprobable)
	    if (!problem.getFaults().isEmpty()) {
		main += "\n" + addSmallTitle("Improbable actions guard");
		String str = "";
		for (ServiceAction sa : problem.getFaults()) {
		    str += "("
			    + (problem.getGlobalInputs().contains(sa) ? "input"
				    : "output") + " = " + sa + ")";
		}
		main += "TRANS (" + str.replaceAll("\\)\\(", "\\) | \\(")
			+ ") -> FALSE \n\n";
	    }

	// Controllers
	main += addLargeTitle("CONTROLLERS");
	// Here and later on we use str as a temporary accumulator
	String str = "";

	if (!problem.getNextActions().isEmpty()) {
	    main += addSmallTitle("Next Controller");

	    main += "VAR NEXT_CONTROLLER: { START, STOP };\n";
	    main += "INIT NEXT_CONTROLLER = START\n\n";
	    main += "ASSIGN\n";
	    main += "next(NEXT_CONTROLLER) := \n";
	    main += "case\n";
	    main += "(NEXT_CONTROLLER = START & ";
	    str = "";
	    for (ServiceAction sa : problem.getNextActions()) {
		str += "("
			+ (problem.getGlobalInputs().contains(sa) ? "input"
				: "output") + " = " + sa.getUniqueAction()
			+ ")";
	    }
	    main += "(" + str.replaceAll("\\)\\(", ") | (") + ")";

	    main += "): STOP;\n";
	    main += "1: NEXT_CONTROLLER;\n";
	    main += "esac;\n\n";

	    main += "TRANS (input != UNDEF | output != UNDEF) -> (NEXT_CONTROLLER != STOP)";
	}

	// adding fault controller

	main += addSmallTitle("Fault Controller");

	main += "VAR FAULT_CONTROLLER: { START, STOP };\n";
	main += "INIT FAULT_CONTROLLER = START\n\n";
	main += "ASSIGN\n";
	main += "next(FAULT_CONTROLLER) := \n";
	main += "case\n";
	main += "(FAULT_CONTROLLER = START & ";

	if (!problem.getFaults().isEmpty()) {
	    str = "";
	    for (ServiceAction sa : problem.getFaults()) {
		str += sa + ", ";
	    }
	    main += "output in {" + str.substring(0, str.length() - 2) + "}";
	} else
	    main += "FALSE";

	main += "): STOP;\n";
	main += "1: FAULT_CONTROLLER;\n";
	main += "esac;\n\n";
	if (useFaultEventsAndActions)
	    main += "TRANS (output != UNDEF | input != UNDEF) -> (FAULT_CONTROLLER != STOP)\n\n";

	// Initialization

	main += addLargeTitle("INITIALIZATION");

	main += "INIT (";
	str = "";
	for (ObjectDiagram od : problem.getObjectDiagrams()) {
	    str += "(" + od.getOid() + " = " + od.getCurrentState() + ")";
	}
	main += str.replaceAll("\\)\\(", ") & (") + ")\n";

	main += "INIT (";
	str = "";
	for (ServiceDiagram sd : problem.getServiceDiagrams()) {
	    str += "(" + sd.getSid() + " = " + sd.getCurrentState() + ")";
	}
	main += str.replaceAll("\\)\\(", ") & (") + ")\n\n";

	main += addLargeTitle("GOAL");
	main += "MODULE main\n";
	main += "VAR dn_main : dn_main;\n\n";

	main += "GOAL ONEOF(\n";

	// THIS IS A PATCH!!! IF WE HAVE ONLY 1 PRIORITY THEN PLANNER SWITCHES
	// TO
	// A STRONG PLANNING ALGO, WHICH CONTAINS ERRORS. TO AVOID THIS, FOR NOW
	// WE
	// KEEP ON USING WEAK PLANNING EVEN FOR 1PRIORITY PROBLEMS
	// counter contains the number of goals
	if (!problem.getNextActions().isEmpty())
	    main += "(dn_main.NEXT_CONTROLLER = STOP), " + (maxPr + 2) + ";\n";

	// ---------------------------------

	for (int i = maxPr; i >= 0; i--) {
	    main += "(dn_main.SYNC" + i + "), " + (i + 1) + ";\n";
	}

	main += "(dn_main.FAULT_CONTROLLER = STOP), 0\n";
	main += "\n)";

	// Saving generated file
	FileWriter writer;
	try {
	    writer = new FileWriter(folderPath + LINE_SEPARATOR
		    + "Compositions" + LINE_SEPARATOR + outputName
		    + LINE_SEPARATOR + outputName + ".smv");
	    writer.write(main);
	    writer.close();
	} catch (IOException e) {
	    logger.error(e);
	}
    }

}
