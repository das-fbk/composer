package eu.fbk.das.composer.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.fbk.das.composer.api.elements.AbstractGoal;
import eu.fbk.das.composer.api.elements.CFExpression;
import eu.fbk.das.composer.api.elements.Effect;
import eu.fbk.das.composer.api.elements.Precondition;
import eu.fbk.das.composer.api.elements.ServiceAction;
import eu.fbk.das.composer.api.elements.SyncPoint;
import eu.fbk.das.composer.api.exceptions.CompositionDuplicateOidException;
import eu.fbk.das.composer.api.exceptions.CompositionDuplicateSidException;
import eu.fbk.das.composer.api.exceptions.InvalidCompositionAbstractGoalException;
import eu.fbk.das.composer.api.exceptions.InvalidCompositionEffectException;
import eu.fbk.das.composer.api.exceptions.InvalidCompositionFaultException;
import eu.fbk.das.composer.api.exceptions.InvalidCompositionNextActionException;
import eu.fbk.das.composer.api.exceptions.InvalidCompositionPreconditionException;
import eu.fbk.das.composer.api.exceptions.InvalidCompositionPriorityException;
import eu.fbk.das.composer.api.exceptions.InvalidCompositionSyncPointException;
import eu.fbk.das.composer.api.exceptions.ObjectCurrentStateNullException;
import eu.fbk.das.composer.api.exceptions.ObjectWithNoStatesException;
import eu.fbk.das.composer.api.exceptions.ServiceCurrentStateNullException;
import eu.fbk.das.process.engine.api.domain.ObjectDiagram;
import eu.fbk.das.process.engine.api.domain.ServiceDiagram;

/**
 * A complete specification of a composition problem that can be translated into
 * SMV and processed by the composition engine
 */
public class CompositionProblem {

    private static final Logger logger = LogManager
	    .getLogger(CompositionProblem.class);

    // Data taken from external sources (e.g., XML files)
    private String compId;
    private List<ObjectDiagram> objectDiagrams;
    private List<ServiceDiagram> serviceDiagrams;
    private List<Precondition> preconditions;
    private List<AbstractGoal> aGoals;
    private List<Precondition> objectPreconditions;
    private List<Effect> effects;

    private Map<SyncPoint, Integer> syncPoints;
    private List<ServiceAction> nextActions;
    private List<CFExpression> requirements;

    private List<ServiceAction> faults;

    // Generated data
    private List<ServiceAction> globalInputs;
    private List<ServiceAction> globalOutputs;
    private List<ServiceAction> globalAbstracts;

    private ArrayList<ServiceAction> globalConcretes;

    /**
     * This constructor checks composition problem specification for consistency
     * 
     * @param objectDiagrams
     * @param serviceDiagrams
     * @param preconditions
     * @param effects
     * @param syncPoints
     * @throws CompositionDuplicateOidException
     * @throws CompositionDuplicateSidException
     * @throws InvalidCompositionPreconditionException
     * @throws InvalidCompositionEffectException
     * @throws InvalidCompositionSyncPointException
     * @throws InvalidCompositionNextActionException
     * @throws InvalidCompositionPriorityException
     * @throws ObjectCurrentStateNullException
     * @throws ServiceCurrentStateNullException
     * @throws InvalidCompositionFaultException
     * @throws ObjectWithNoStatesException
     */
    public CompositionProblem(String compId,
	    List<ObjectDiagram> objectDiagrams,
	    List<ServiceDiagram> serviceDiagrams,
	    List<Precondition> preconditions,
	    List<Precondition> objectPreconditions, List<AbstractGoal> abGoals,
	    List<Effect> effects, Map<SyncPoint, Integer> syncPoints,
	    List<ServiceAction> nextActions, List<ServiceAction> faults,
	    List<CFExpression> requirements)
	    throws CompositionDuplicateOidException,
	    CompositionDuplicateSidException,
	    InvalidCompositionAbstractGoalException,
	    InvalidCompositionPreconditionException,
	    InvalidCompositionEffectException,
	    InvalidCompositionSyncPointException,
	    InvalidCompositionNextActionException,
	    InvalidCompositionPriorityException,
	    ObjectCurrentStateNullException, ServiceCurrentStateNullException,
	    InvalidCompositionFaultException, ObjectWithNoStatesException {

	this.compId = compId;
	this.objectDiagrams = new ArrayList<ObjectDiagram>(objectDiagrams);
	// We check for objects with duplicating oid
	for (ObjectDiagram obj : objectDiagrams) {
	    int count = 0;
	    for (ObjectDiagram obj2 : objectDiagrams)
		if (obj.getOid().equals(obj2.getOid()))
		    count++;
	    if (count > 1)
		throw new CompositionDuplicateOidException();
	    if (obj.getCurrentState() == null)
		throw new ObjectCurrentStateNullException();
	    if (obj.getStates() == null || obj.getStates().isEmpty()) {
		logger.error("It is likely all the states are pruned during the optimization");
		throw new ObjectWithNoStatesException();
	    }

	}

	this.serviceDiagrams = new ArrayList<ServiceDiagram>(serviceDiagrams);
	// We check for services with duplicating sid
	for (ServiceDiagram srv : serviceDiagrams) {
	    int count = 0;
	    for (ServiceDiagram srv2 : serviceDiagrams)
		if (srv.getSid().equals(srv2.getSid()))
		    count++;
	    if (count > 1)
		throw new CompositionDuplicateSidException();
	    if (srv.getCurrentState() == null)
		throw new ServiceCurrentStateNullException();
	}

	// We check each precondition for consistensy while adding it to the
	// problem
	this.preconditions = new ArrayList<Precondition>();
	for (Precondition pr : preconditions) {
	    ServiceDiagram sd = getService(pr.getSid());

	    if (sd == null)
		throw new InvalidCompositionPreconditionException();
	    if (!sd.getInputs().contains(pr.getAction())
		    && !sd.getOutputs().contains(pr.getAction()))
		throw new InvalidCompositionPreconditionException();

	    for (Map<String, List<String>> o2s : pr.getOid2states())
		for (String oid : o2s.keySet()) {
		    ObjectDiagram od = getObject(oid);
		    if (od == null) {
			System.out.println(pr);
			throw new InvalidCompositionPreconditionException();

		    }

		    if (!od.getStates().containsAll(o2s.get(oid))) {
			throw new InvalidCompositionPreconditionException();
		    }
		}
	    this.preconditions.add(pr);
	}

	// We check each effect for consistensy while adding it to the problem
	this.effects = new ArrayList<Effect>();
	for (Effect eff : effects) {
	    ServiceDiagram sd = getService(eff.getSid());
	    if (sd == null)
		throw new InvalidCompositionEffectException();
	    if (!sd.getInputs().contains(eff.getAction())
		    && !sd.getOutputs().contains(eff.getAction()))
		throw new InvalidCompositionEffectException();
	    ObjectDiagram od = getObject(eff.getOid());
	    if (od == null)
		throw new InvalidCompositionEffectException();
	    if (!od.getEvents().contains(eff.getEvent())) {
		throw new InvalidCompositionEffectException();
	    }
	    this.effects.add(eff);
	}

	this.syncPoints = new HashMap<SyncPoint, Integer>();
	if (syncPoints.size() > 0) {
	    // We check sync points for consistensy
	    int maxPr = 0;
	    for (SyncPoint point : syncPoints.keySet()) {
		for (String oid : point.getOid2state().keySet()) {
		    ObjectDiagram od = getObject(oid);
		    if (od == null)
			throw new InvalidCompositionSyncPointException();
		    if (!od.getStates().containsAll(
			    point.getOid2state().get(oid)))
			throw new InvalidCompositionSyncPointException();
		}
		this.syncPoints.put(point, syncPoints.get(point));
		if (maxPr < syncPoints.get(point))
		    maxPr = syncPoints.get(point);
	    }

	    // We guarantee that if the max priority = n then there is at least
	    // one syncPoint
	    // for each priority < n
	    boolean[] prs = new boolean[maxPr + 1];
	    for (int i = 0; i <= maxPr; i++) {
		prs[i] = false;
	    }
	    for (SyncPoint sp : this.syncPoints.keySet()) {
		prs[syncPoints.get(sp)] = true;
	    }

	    for (int i = 0; i <= maxPr; i++) {
		if (!prs[i])
		    throw new InvalidCompositionPriorityException();
	    }
	}
	// We check next actions for consistensy
	this.nextActions = new ArrayList<ServiceAction>();
	for (ServiceAction sa : nextActions) {
	    ServiceDiagram sd = getService(sa.getSid());
	    if (sd == null)
		throw new InvalidCompositionNextActionException();
	    if (!sd.getInputs().contains(sa.getAction())
		    && !sd.getOutputs().contains(sa.getAction()))
		throw new InvalidCompositionNextActionException();
	    this.nextActions.add(sa);
	}

	// Here we generate auxiliary data
	globalInputs = new ArrayList<ServiceAction>();
	for (ServiceDiagram sd : this.serviceDiagrams) {
	    for (String action : sd.getInputs()) {
		globalInputs.add(new ServiceAction(sd.getSid(), action));
	    }
	}

	globalOutputs = new ArrayList<ServiceAction>();
	for (ServiceDiagram sd : this.serviceDiagrams) {
	    for (String action : sd.getOutputs()) {
		// System.out.println("ACTION-----"+action);
		globalOutputs.add(new ServiceAction(sd.getSid(), action));
	    }
	}

	globalAbstracts = new ArrayList<ServiceAction>();
	for (ServiceDiagram sd : this.serviceDiagrams) {
	    if (sd.getAbstracts() != null)
		for (String action : sd.getAbstracts()) {
		    // System.out.println("ACTION-----"+action);
		    globalAbstracts.add(new ServiceAction(sd.getSid(), action));
		}
	}
	globalConcretes = new ArrayList<ServiceAction>();
	for (ServiceDiagram sd : this.serviceDiagrams) {
	    if (sd.getConcretes() != null)
		for (String action : sd.getConcretes()) {
		    // System.out.println("ACTION-----"+action);
		    globalConcretes.add(new ServiceAction(sd.getSid(), action));
		}
	}

	// We collect all faults as global actions;
	this.faults = new ArrayList<ServiceAction>();
	for (ServiceAction sa : faults)
	    if (globalOutputs.contains(sa)) {
		this.faults.add(sa);
	    } else {
		throw new InvalidCompositionFaultException();
	    }

	objectPreconditions = new ArrayList<Precondition>();
	this.objectPreconditions = new ArrayList<Precondition>(
		objectPreconditions);

	this.aGoals = new ArrayList<AbstractGoal>();
	for (AbstractGoal ag : abGoals) {
	    ServiceDiagram sd = getService(ag.getSid());

	    if (sd == null)
		throw new InvalidCompositionAbstractGoalException();
	    if (!sd.getInputs().contains(ag.getAction())
		    || !sd.getAbstracts().contains(ag.getAction()))
		throw new InvalidCompositionAbstractGoalException();

	    for (Map<String, List<String>> o2s : ag.getOid2states())
		for (String oid : o2s.keySet()) {
		    ObjectDiagram od = getObject(oid);
		    if (od == null) {
			System.out.println(ag);
			throw new InvalidCompositionAbstractGoalException();

		    }
		    if (!od.getStates().containsAll(o2s.get(oid))) {
			System.out.println(ag);
			throw new InvalidCompositionAbstractGoalException();
		    }
		}
	    this.aGoals.add(ag);
	}

	this.requirements = new ArrayList<CFExpression>();
	if (requirements != null)
	    this.requirements.addAll(requirements);

    }

    public List<ObjectDiagram> getObjectDiagrams() {
	return objectDiagrams;
    }

    public List<ServiceDiagram> getServiceDiagrams() {
	return serviceDiagrams;
    }

    public List<Precondition> getPreconditions() {
	return preconditions;
    }

    public List<AbstractGoal> getaGoals() {
	return aGoals;
    }

    public List<Effect> getEffects() {
	return effects;
    }

    public Map<SyncPoint, Integer> getSyncPoints() {
	return syncPoints;
    }

    public List<ServiceAction> getNextActions() {
	return nextActions;
    }

    public List<ServiceAction> getFaults() {
	return faults;
    }

    public List<ServiceAction> getGlobalInputs() {
	return globalInputs;
    }

    public List<ServiceAction> getGlobalOutputs() {
	return globalOutputs;
    }

    public List<ServiceAction> getGlobalAbstracts() {
	return globalAbstracts;
    }

    public String getCompId() {
	return compId;
    }

    /**
     * returns service with object identifier oid (if any)
     * 
     * @param oid
     * @return ObjectDiagram object or null (if not found)
     */
    public ObjectDiagram getObject(String oid) {
	for (ObjectDiagram object : objectDiagrams) {
	    if (object.getOid().equals(oid))
		return object;
	}
	return null;
    }

    /**
     * returns service with service identifier sid (if any)
     * 
     * @param sid
     * @return ServiceDiagram object or null (if not found)
     */
    public ServiceDiagram getService(String sid) {
	for (ServiceDiagram service : serviceDiagrams) {
	    if (service.getSid().equals(sid))
		return service;
	}
	return null;
    }

    public List<Precondition> getObjectPreconditions() {
	return objectPreconditions;
    }

    @Override
    public CompositionProblem clone() {
	CompositionProblem cp = null;
	try {

	    List<ObjectDiagram> ods = new ArrayList<ObjectDiagram>();
	    for (ObjectDiagram od : objectDiagrams) {
		ods.add(od.clone());
	    }
	    List<ServiceDiagram> sds = new ArrayList<ServiceDiagram>();
	    for (ServiceDiagram sd : serviceDiagrams) {
		sds.add(sd.clone());
	    }

	    List<Precondition> prs = new ArrayList<Precondition>();
	    for (Precondition pr : preconditions) {
		prs.add(pr);
	    }

	    List<AbstractGoal> ags = new ArrayList<AbstractGoal>();
	    for (AbstractGoal ag : aGoals) {
		ags.add(ag);
	    }

	    List<Effect> effs = new ArrayList<Effect>();
	    for (Effect eff : effects) {
		effs.add(new Effect(eff.getSid(), eff.getAction(),
			eff.getOid(), eff.getEvent()));
	    }

	    Map<SyncPoint, Integer> sps = new HashMap<SyncPoint, Integer>();

	    for (SyncPoint s : syncPoints.keySet()) {
		sps.put(s.clone(), syncPoints.get(s));
	    }

	    List<ServiceAction> actions = new ArrayList<ServiceAction>();
	    for (ServiceAction a : nextActions) {
		actions.add(new ServiceAction(a.getSid(), a.getAction()));
	    }

	    List<ServiceAction> fs = new ArrayList<ServiceAction>();
	    for (ServiceAction a : faults) {
		fs.add(new ServiceAction(a.getSid(), a.getAction()));
	    }

	    List<Precondition> oprs = new ArrayList<Precondition>();
	    for (Precondition pr : objectPreconditions) {
		oprs.add(pr);
	    }

	    cp = new CompositionProblem(compId, ods, sds, prs, oprs, ags, effs,
		    sps, actions, fs, null);
	} catch (CompositionDuplicateOidException
		| InvalidCompositionPreconditionException
		| CompositionDuplicateSidException
		| InvalidCompositionEffectException
		| InvalidCompositionSyncPointException
		| InvalidCompositionNextActionException
		| InvalidCompositionPriorityException
		| ObjectCurrentStateNullException
		| ServiceCurrentStateNullException
		| InvalidCompositionFaultException
		| InvalidCompositionAbstractGoalException
		| ObjectWithNoStatesException e) {
	    logger.error(e.getMessage(), e);
	}
	return cp;
    }

    public List<ServiceAction> getGlobalConcretes() {
	return globalConcretes;
    }
}
