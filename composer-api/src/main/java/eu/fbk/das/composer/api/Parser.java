package eu.fbk.das.composer.api;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.fbk.das.composer.api.elements.AbstractGoal;
import eu.fbk.das.composer.api.elements.CFClause;
import eu.fbk.das.composer.api.elements.CFClauseAction;
import eu.fbk.das.composer.api.elements.CFClauseAnd;
import eu.fbk.das.composer.api.elements.CFClauseEvent;
import eu.fbk.das.composer.api.elements.CFClauseOr;
import eu.fbk.das.composer.api.elements.CFClauseSituation;
import eu.fbk.das.composer.api.elements.CFClauseThen;
import eu.fbk.das.composer.api.elements.CFClauseTryCatch;
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
import eu.fbk.das.composer.api.exceptions.InvalidServiceCurrentStateException;
import eu.fbk.das.composer.api.exceptions.InvalidServiceInitialStateException;
import eu.fbk.das.composer.api.exceptions.InvalidServiceObjectAssignmentException;
import eu.fbk.das.composer.api.exceptions.InvalidServiceTransitionException;
import eu.fbk.das.composer.api.exceptions.ObjectCurrentStateNullException;
import eu.fbk.das.composer.api.exceptions.ObjectWithNoStatesException;
import eu.fbk.das.composer.api.exceptions.ServiceCurrentStateNullException;
import eu.fbk.das.composer.api.exceptions.ServiceDuplicateActionException;
import eu.fbk.das.composer.api.exceptions.ServiceGroundingTypeMismatchException;
import eu.fbk.das.process.engine.api.domain.ObjectDiagram;
import eu.fbk.das.process.engine.api.domain.ObjectTransition;
import eu.fbk.das.process.engine.api.domain.ServiceDiagram;
import eu.fbk.das.process.engine.api.domain.ServiceDiagramActionType;
import eu.fbk.das.process.engine.api.domain.ServiceTransition;
import eu.fbk.das.process.engine.api.domain.exceptions.InvalidObjectCurrentStateException;
import eu.fbk.das.process.engine.api.domain.exceptions.InvalidObjectInitialStateException;
import eu.fbk.das.process.engine.api.domain.exceptions.InvalidObjectTransitionException;
import eu.fbk.das.process.engine.api.jaxb.ActionTypeValues;
import eu.fbk.das.process.engine.api.jaxb.ClauseType.Point;
import eu.fbk.das.process.engine.api.jaxb.Composition;
import eu.fbk.das.process.engine.api.jaxb.ControlFlowType;
import eu.fbk.das.process.engine.api.jaxb.DomainProperty;
import eu.fbk.das.process.engine.api.jaxb.DomainPropertyEventType;
import eu.fbk.das.process.engine.api.jaxb.EffectType.Event;
import eu.fbk.das.process.engine.api.jaxb.Fragment;
import eu.fbk.das.process.engine.api.jaxb.Fragment.Action;
import eu.fbk.das.process.engine.api.jaxb.Fragment.Transition;
import eu.fbk.das.process.engine.api.util.UnmarshalHelper;

/**
 * Parser class to read composition files
 */
public class Parser {

	private static final Logger logger = LogManager.getLogger(Parser.class);

	private String compositionXsd;
	private String objectXsd;
	private String serviceXsd;
	private String repositoryHome;

	public Parser(String schemataHome, String repositoryHome) {
		compositionXsd = schemataHome + System.getProperty("file.separator")
				+ "Composition.xsd";
		objectXsd = schemataHome + System.getProperty("file.separator")
				+ "Object.xsd";
		serviceXsd = schemataHome + System.getProperty("file.separator")
				+ "Service.xsd";
		this.repositoryHome = repositoryHome;

	}

	/**
	 * This method instansiates a new ObjectDiagram of class type
	 * 
	 * @param oid
	 *            identifier of a new object diagram
	 * @param type
	 *            class to be used for instantiation
	 * @return object of class ObjectDiagram
	 * @throws JAXBException
	 */
	public ObjectDiagram parseObjectDiagram(String oid, String type) {
		String objectXml = repositoryHome
				+ System.getProperty("file.separator") + "domainProperties"
				+ System.getProperty("file.separator") + type + ".xml";

		DomainProperty objectJAXB = (eu.fbk.das.process.engine.api.jaxb.DomainProperty) UnmarshalHelper
				.doUnmarshal("Composition",
						"eu.fbk.das.process.engine.api.jaxb", objectXsd,
						objectXml);

		// Here we instantiate a new object diagram
		Set<String> states = new HashSet<String>();
		Set<String> initialStates = new HashSet<String>();
		Set<String> events = new HashSet<String>();
		Set<ObjectTransition> transitions = new HashSet<ObjectTransition>();
		for (DomainProperty.State state : objectJAXB.getState()) {
			states.add(state.getValue());
			if (state.isIsInitial() != null && state.isIsInitial())
				initialStates.add(state.getValue());
		}

		if (objectJAXB.getEvent() != null)
			for (DomainPropertyEventType event : objectJAXB.getEvent()) {
				events.add(event.getValue());
			}

		if (objectJAXB.getTransition() != null)
			for (DomainProperty.Transition tr : objectJAXB.getTransition())
				transitions.add(new ObjectTransition(tr.getFrom().getValue(),
						tr.getTo().getValue(), tr.getEvent().getValue()));

		ObjectDiagram diagram = null;
		try {
			diagram = new ObjectDiagram(oid, type, states, initialStates,
					events, transitions);
		} catch (InvalidObjectInitialStateException
				| InvalidObjectTransitionException e) {
			logger.error(e.getMessage(), e);
			return null;
		}
		return diagram;
	}

	/**
	 * This method instansiates a new ServiceDiagram of class type
	 * 
	 * @param sid
	 *            identifiaer of a new service instance
	 * @param type
	 *            type of a new service instance
	 * @return
	 */
	public ServiceDiagram parseServiceDiagram(String sid, String type,
			boolean isCyclic) {
		String serviceXml = repositoryHome
				+ System.getProperty("file.separator") + "fragments"
				+ System.getProperty("file.separator")
				+ System.getProperty("file.separator") + "" + type + ".xml";

		Fragment fragment = null;
		Unmarshaller un;
		try {
			JAXBContext context;
			context = JAXBContext.newInstance(Fragment.class);
			un = context.createUnmarshaller();
			fragment = (Fragment) un.unmarshal(new File(serviceXml));
		} catch (JAXBException e) {
			logger.error(e.getMessage(), e);
		}

		// Here we instantiate a new object diagram
		Set<String> states = new HashSet<String>();
		Set<String> initialStates = new HashSet<String>();
		Set<String> inputs = new HashSet<String>();
		Set<String> outputs = new HashSet<String>();
		Set<String> abstracts = new HashSet<String>();
		Set<String> concretes = new HashSet<String>();
		Set<ServiceTransition> transitions = new HashSet<ServiceTransition>();
		for (Fragment.State state : fragment.getState()) {
			states.add(state.getName());
			if (state.isIsInitial() != null && state.isIsInitial())
				initialStates.add(state.getName());
		}
		for (Action action : fragment.getAction()) {
			if (action.getActionType().equals(ActionTypeValues.ABSTRACT)) {
				inputs.add(action.getName());
				abstracts.add(action.getName());
				// System.out.println(action.getValue().getValue());
			} else if (action.getActionType().equals(ActionTypeValues.INPUT))
				inputs.add(action.getName());
			else if (action.getActionType().equals(ActionTypeValues.OUTPUT))
				outputs.add(action.getName());
			if (action.getActionType().equals(ActionTypeValues.CONCRETE)) {
				concretes.add(action.getName());
				outputs.add(action.getName());
			}

		}

		for (Transition tr : fragment.getTransition()) {
			ServiceDiagramActionType t = getServiceDiagramActionType(tr);
			String actionName;
			if (tr.getAction() != null
					&& tr.getAction().getActionType()
							.equals(ActionTypeValues.OUTPUT))
				actionName = tr.getAction().getName();
			else if (tr.getAction() != null
					&& tr.getAction().getActionType()
							.equals(ActionTypeValues.INPUT))
				actionName = tr.getAction().getName();
			else
				actionName = tr.getAction().getName();

			transitions.add(new ServiceTransition(tr.getInitialState()
					.getValue(), tr.getFinalState().getValue(), actionName, t));
		}

		ServiceDiagram diagram = null;
		try {
			diagram = new ServiceDiagram(sid, type,
					fragment.getConsumerEntityType(), states, initialStates,
					inputs, outputs, abstracts, concretes, transitions,
					isCyclic);
		} catch (InvalidServiceInitialStateException
				| InvalidServiceTransitionException
				| ServiceDuplicateActionException e) {
			logger.error(e.getMessage(), e);
			return null;
		}
		return diagram;
	}

	private ServiceDiagramActionType getServiceDiagramActionType(Transition tr) {
		if (tr.getAction() != null
				&& tr.getAction().getActionType()
						.equals(ActionTypeValues.OUTPUT)) {
			return ServiceDiagramActionType.OUT;
		} else if (tr.getAction() != null
				&& tr.getAction().getActionType()
						.equals(ActionTypeValues.INPUT)) {
			return ServiceDiagramActionType.IN;
		} else if (tr.getAction() != null
				&& tr.getAction().getActionType()
						.equals(ActionTypeValues.CONCRETE)) {
			return ServiceDiagramActionType.OUT;
		}

		return ServiceDiagramActionType.IN;
	}

	// it is used to get grounding info
	// returns a map oName2ObjType
	public Map<String, String> parseServiceGrounding(String type) {
		String serviceXml = repositoryHome
				+ System.getProperty("file.separator") + "fragments"
				+ System.getProperty("file.separator") + "" + type + ".xml";

		Fragment serviceJAXB = null;
		Unmarshaller un;
		try {
			JAXBContext context;
			context = JAXBContext.newInstance(Fragment.class);
			un = context.createUnmarshaller();
			serviceJAXB = (Fragment) un.unmarshal(new File(serviceXml));
		} catch (JAXBException e) {
			logger.error(e.getMessage(), e);
		}

		Map<String, String> oName2type = new HashMap<String, String>();

		for (Action act : serviceJAXB.getAction()) {
			if (act.getEffect() != null) {
				for (Event event : act.getEffect().getEvent()) {
					oName2type.put(event.getDpName(), event.getDpName());
				}
			}
			if (act.getPrecondition() != null) {
				for (Point point : act.getPrecondition().getPoint()) {
					for (eu.fbk.das.process.engine.api.jaxb.ClauseType.Point.DomainProperty dp : point
							.getDomainProperty()) {
						oName2type.put(dp.getDpName(), dp.getDpName());
					}
				}
			}
			if (act.getGoal() != null) {
				for (Point p : act.getGoal().getPoint()) {
					for (eu.fbk.das.process.engine.api.jaxb.ClauseType.Point.DomainProperty dp : p
							.getDomainProperty()) {
						oName2type.put(dp.getDpName(), dp.getDpName());
					}
				}
			}
		}
		return oName2type;

	}

	/**
	 * This method instansiates effects for a certain
	 * 
	 * @param srv
	 *            service diagram for which instantiate
	 * @param object2oid
	 *            relates abstract names in service annotations to concrete
	 *            instances
	 * @return
	 * @throws InvalidServiceObjectAssignmentException
	 */
	public List<Effect> parseServiceEffects(ServiceDiagram srv,
			Map<String, String> object2oid)
			throws InvalidServiceObjectAssignmentException {
		List<Effect> effs = new ArrayList<Effect>();
		String serviceXml = repositoryHome
				+ System.getProperty("file.separator") + "fragments"
				+ System.getProperty("file.separator") + "" + srv.getType()
				+ ".xml";

		Fragment serviceJAXB = null;
		Unmarshaller un;
		try {
			JAXBContext context;
			context = JAXBContext.newInstance(Fragment.class);
			un = context.createUnmarshaller();
			serviceJAXB = (Fragment) un.unmarshal(new File(serviceXml));
		} catch (JAXBException e) {
			logger.error(e.getMessage(), e);
			return null;
		}

		// Here we instantiate effects
		for (Action act : serviceJAXB.getAction()) {
			if (act.getEffect() != null && act.getEffect().getEvent() != null) {
				for (Event event : act.getEffect().getEvent()) {
					effs.add(new Effect(srv.getSid(), act.getName(), event
							.getDpName(), event.getEventName()));
				}
			}
		}
		return effs;
	}

	/**
	 * This method instansiates effects for a certain
	 * 
	 * @param srv
	 *            service diagram for which instantiate
	 * @param object2oid
	 *            relates abstract names in service annotations to concrete
	 *            instances
	 * @return
	 * @throws InvalidServiceObjectAssignmentException
	 */
	public List<Precondition> parseServicePreconditions(ServiceDiagram srv,
			Map<String, String> object2oid)
			throws InvalidServiceObjectAssignmentException {
		List<Precondition> precs = new ArrayList<Precondition>();
		String serviceXml = repositoryHome
				+ System.getProperty("file.separator") + "fragments"
				+ System.getProperty("file.separator") + "" + srv.getType()
				+ ".xml";
		Fragment serviceJAXB = null;
		Unmarshaller un;
		try {
			JAXBContext context;
			context = JAXBContext.newInstance(Fragment.class);
			un = context.createUnmarshaller();
			serviceJAXB = (Fragment) un.unmarshal(new File(serviceXml));
		} catch (JAXBException e) {
			logger.error(e.getMessage(), e);
			return null;
		}

		if (serviceJAXB.getAction() != null) {
			for (Action act : serviceJAXB.getAction()) {
				if (act.getPrecondition() != null
						&& act.getPrecondition().getPoint() != null) {
					List<Map<String, List<String>>> listO2s = new ArrayList<Map<String, List<String>>>();
					for (Point point : act.getPrecondition().getPoint()) {
						Map<String, List<String>> o2s = new HashMap<String, List<String>>();
						for (eu.fbk.das.process.engine.api.jaxb.ClauseType.Point.DomainProperty obj : point
								.getDomainProperty()) {
							if (object2oid.get(obj.getDpName()) == null) {
								logger.debug(obj.getDpName());
								logger.debug(srv.getType());
								throw new InvalidServiceObjectAssignmentException();
							}
							List<String> states = new ArrayList<String>(
									obj.getState());
							o2s.put(object2oid.get(obj.getDpName()), states);
						}
						listO2s.add(o2s);
					}
					precs.add(new Precondition(srv.getSid(), act.getName(),
							listO2s));
				}
			}
		}

		return precs;
	}

	public List<AbstractGoal> parseServiceAbstractGoals(ServiceDiagram srv,
			Map<String, String> object2oid)
			throws InvalidServiceObjectAssignmentException {
		List<AbstractGoal> aGoals = new ArrayList<AbstractGoal>();
		String serviceXml = repositoryHome
				+ System.getProperty("file.separator") + "fragments"
				+ System.getProperty("file.separator") + "" + srv.getType()
				+ ".xml";

		Fragment serviceJAXB = null;
		Unmarshaller un;
		try {
			JAXBContext context;
			context = JAXBContext.newInstance(Fragment.class);
			un = context.createUnmarshaller();
			serviceJAXB = (Fragment) un.unmarshal(new File(serviceXml));
		} catch (JAXBException e) {
			logger.error(e.getMessage(), e);
			return null;
		}

		// Here we instantiate goals
		if (serviceJAXB.getAction() != null) {
			for (Action act : serviceJAXB.getAction()) {
				if (act.getGoal() != null) {
					List<Map<String, List<String>>> listO2s = new ArrayList<Map<String, List<String>>>();
					for (Point point : act.getGoal().getPoint()) {
						Map<String, List<String>> o2s = new HashMap<String, List<String>>();
						for (eu.fbk.das.process.engine.api.jaxb.ClauseType.Point.DomainProperty obj : point
								.getDomainProperty()) {
							if (object2oid.get(obj.getDpName()) == null) {
								logger.debug(obj.getDpName());
								logger.debug(srv.getType());
								throw new InvalidServiceObjectAssignmentException();
							}
							List<String> states = new ArrayList<String>(
									obj.getState());
							o2s.put(object2oid.get(obj.getDpName()), states);
						}
						listO2s.add(o2s);
					}

					aGoals.add(new AbstractGoal(srv.getSid(), act.getName(),
							listO2s));
				}
			}
		}
		return aGoals;
	}

	/**
	 * This method reads object list used in service annotation
	 * 
	 * @param srv
	 *            service diagram for which instantiate
	 * @param object2oid
	 *            relates abstract names in service annotations to concrete
	 *            instances
	 * @return
	 * @throws InvalidServiceObjectAssignmentException
	 */
	public Map<String, String> parseServiceAbstractObjects(String srvType) {
		Map<String, String> oname2type = new HashMap<String, String>();
		String serviceXml = repositoryHome
				+ System.getProperty("file.separator") + "fragments"
				+ System.getProperty("file.separator") + "" + srvType + ".xml";

		Fragment serviceJAXB = null;
		Unmarshaller un;
		try {
			JAXBContext context;
			context = JAXBContext.newInstance(Fragment.class);
			un = context.createUnmarshaller();
			serviceJAXB = (Fragment) un.unmarshal(new File(serviceXml));
		} catch (JAXBException e) {
			logger.error(e.getMessage(), e);
			return null;
		}

		for (Action act : serviceJAXB.getAction()) {
			if (act.getEffect() != null) {
				for (Event event : act.getEffect().getEvent()) {
					oname2type.put(event.getDpName(), event.getDpName());
				}
			}
			if (act.getPrecondition() != null) {
				for (Point point : act.getPrecondition().getPoint()) {
					for (eu.fbk.das.process.engine.api.jaxb.ClauseType.Point.DomainProperty dp : point
							.getDomainProperty()) {
						oname2type.put(dp.getDpName(), dp.getDpName());
					}
				}
			}
			if (act.getGoal() != null) {
				for (Point p : act.getGoal().getPoint()) {
					for (eu.fbk.das.process.engine.api.jaxb.ClauseType.Point.DomainProperty dp : p
							.getDomainProperty()) {
						oname2type.put(dp.getDpName(), dp.getDpName());
					}
				}
			}
		}

		return oname2type;
	}

	/**
	 * parses a control-flow clause
	 * 
	 * @param clause
	 * @return instance of CFClause corresponding to a cluase
	 */
	public CFClause parseCFClause(Object clause) {
		String className = clause.getClass().getCanonicalName();

		// SITUATION

		if (className.equals(ControlFlowType.Result.Situation.class
				.getCanonicalName())) {
			ControlFlowType.Result.Situation situation = (ControlFlowType.Result.Situation) clause;
			Map<String, List<String>> oid2states;
			oid2states = new HashMap<String, List<String>>();
			for (ControlFlowType.Result.Situation.DomainProperty obj : situation
					.getDomainProperty())
				oid2states.put(obj.getOid(), obj.getState());
			return new CFClauseSituation(oid2states);
		}

		// ACTION

		if (className.equals(ControlFlowType.Result.Action.class
				.getCanonicalName())) {
			ControlFlowType.Result.Action action = (ControlFlowType.Result.Action) clause;
			Map<String, List<String>> oid2states;
			if (action.getCondition() != null) {
				oid2states = new HashMap<String, List<String>>();
				for (ControlFlowType.Result.Action.Condition.DomainProperty obj : action
						.getCondition().getDomainProperty())
					oid2states.put(obj.getOid(), obj.getState());
			} else {
				oid2states = null;
			}
			return new CFClauseAction(action.getSid(), action.getName(),
					oid2states);
		}

		// EVENT

		if (className.equals(ControlFlowType.Result.Event.class
				.getCanonicalName())) {
			ControlFlowType.Result.Event event = (ControlFlowType.Result.Event) clause;
			Map<String, List<String>> oid2states;
			if (event.getCondition() != null) {
				oid2states = new HashMap<String, List<String>>();
				for (ControlFlowType.Result.Event.Condition.DomainProperty obj : event
						.getCondition().getDomainProperty())
					oid2states.put(obj.getOid(), obj.getState());
			} else {
				oid2states = null;
			}
			return new CFClauseEvent(event.getOid(), event.getName(),
					oid2states);
		}

		// THEN

		if (className.equals(ControlFlowType.Result.Then.class
				.getCanonicalName())) {
			ControlFlowType.Result.Then then = (ControlFlowType.Result.Then) clause;
			List<CFClause> clauses = new ArrayList<CFClause>();
			for (Object cl : then.getClause()) {
				clauses.add(parseCFClause(cl));
			}

			return new CFClauseThen(clauses);
		}

		// AND

		if (className.equals(ControlFlowType.Result.And.class
				.getCanonicalName())) {
			ControlFlowType.Result.And and = (ControlFlowType.Result.And) clause;
			List<CFClause> clauses = new ArrayList<CFClause>();
			for (Object cl : and.getClause()) {
				clauses.add(parseCFClause(cl));
			}

			return new CFClauseAnd(clauses);
		}

		// OR

		if (className
				.equals(ControlFlowType.Result.Or.class.getCanonicalName())) {
			ControlFlowType.Result.Or or = (ControlFlowType.Result.Or) clause;
			List<CFClause> clauses = new ArrayList<CFClause>();
			for (Object cl : or.getClause()) {
				clauses.add(parseCFClause(cl));
			}

			return new CFClauseOr(clauses);
		}

		// TRYCATCH

		if (className.equals(ControlFlowType.Result.Trycatch.class
				.getCanonicalName())) {
			ControlFlowType.Result.Trycatch tc = (ControlFlowType.Result.Trycatch) clause;

			// although tc.getTry().getClause() returns a list, we know that
			// only one element is allowed in try
			// the same is in catch
			CFClause tryCl = parseCFClause(tc.getTry().getClause().get(0));
			Map<CFClause, CFClause> catchClauses = new HashMap<CFClause, CFClause>();

			for (ControlFlowType.Result.Trycatch.Catch cat : tc.getCatch()) {
				catchClauses.put(parseCFClause(cat.getCondition().getClause()
						.get(0)), parseCFClause(cat.getReaction().getClause()
						.get(0)));
			}

			return new CFClauseTryCatch(tryCl, catchClauses);
		}

		return null;
	}

	public CompositionProblem parseCompositionProblem(String compositionName)
			throws CompositionDuplicateOidException,
			CompositionDuplicateSidException,
			InvalidCompositionPreconditionException,
			InvalidCompositionEffectException,
			InvalidObjectCurrentStateException,
			InvalidServiceCurrentStateException,
			InvalidServiceObjectAssignmentException,
			ServiceGroundingTypeMismatchException {
		String compositionXml = repositoryHome
				+ System.getProperty("file.separator") + "Compositions"
				+ System.getProperty("file.separator") + compositionName
				+ System.getProperty("file.separator") + compositionName
				+ ".xml";

		Composition compositionJAXB = (Composition) UnmarshalHelper
				.doUnmarshal("Composition",
						"eu.fbk.das.process.engine.api.jaxb", compositionXsd,
						compositionXml);

		// Here we instansiate a new composition problem

		// object diagrams parsing
		List<ObjectDiagram> objectDiagrams = new ArrayList<ObjectDiagram>();
		for (Composition.DomainProperties.DomainProperty obj : compositionJAXB
				.getDomainProperties().getDomainProperty()) {
			objectDiagrams.add(parseObjectDiagram(obj.getOid(), obj.getType()));
		}
		// services, preconditions and effects parsing
		List<ServiceDiagram> serviceDiagrams = new ArrayList<ServiceDiagram>();
		List<Precondition> preconditions = new ArrayList<Precondition>();
		List<AbstractGoal> aGoals = new ArrayList<AbstractGoal>();
		List<Precondition> objectPreconditions = new ArrayList<Precondition>();
		List<Effect> effects = new ArrayList<Effect>();
		for (Composition.Fragments.Fragment srv : compositionJAXB
				.getFragments().getFragment()) {
			ServiceDiagram srvDiag = parseServiceDiagram(srv.getSid(),
					srv.getType(), false);
			serviceDiagrams.add(srvDiag);

			Map<String, String> object2oid = new HashMap<String, String>();
			for (Composition.Fragments.Fragment.Assignment assign : srv
					.getAssignment()) {
				object2oid.put(assign.getOName(), assign.getOid());
			}

			Map<String, String> oName2type = parseServiceGrounding(srv
					.getType());
			// grounding type verification
			for (String oName : object2oid.keySet()) {
				String type = oName2type.get(oName);
				String oid = object2oid.get(oName);
				if (type == null)
					throw new ServiceGroundingTypeMismatchException();
				boolean isReal = false;
				for (ObjectDiagram obj : objectDiagrams) {
					if (obj.getOid().equals(oid) && obj.getType().equals(type)) {
						isReal = true;
						break;
					}
				}
				if (!isReal) {
					logger.debug(srv.getSid());
					logger.debug(type);
					logger.debug(oid);
					throw new ServiceGroundingTypeMismatchException();
				}
			}

			effects.addAll(parseServiceEffects(srvDiag, object2oid));
			preconditions
					.addAll(parseServicePreconditions(srvDiag, object2oid));
			aGoals.addAll(parseServiceAbstractGoals(srvDiag, object2oid));
		}
		// sync points parsing
		Map<SyncPoint, Integer> syncPoints = new HashMap<SyncPoint, Integer>();
		if (compositionJAXB.getSynchronization() != null)
			for (Composition.Synchronization.Point point : compositionJAXB
					.getSynchronization().getPoint()) {
				Map<String, List<String>> oid2state = new HashMap<String, List<String>>();
				for (Composition.Synchronization.Point.DomainProperty obj : point
						.getDomainProperty()) {
					oid2state.put(obj.getOid(), obj.getState());
				}
				syncPoints.put(new SyncPoint(oid2state), point.getPriority());
			}

		// next actions parsing
		List<ServiceAction> nextActions = new ArrayList<ServiceAction>();
		if (compositionJAXB.getNextActions() != null)
			for (Composition.NextActions.Action action : compositionJAXB
					.getNextActions().getAction()) {
				nextActions.add(new ServiceAction(action.getSid(), action
						.getValue()));
			}

		// runtime parsing
		for (Composition.Runtime.DomainProperties.DomainProperty obj : compositionJAXB
				.getRuntime().getDomainProperties().getDomainProperty()) {
			for (ObjectDiagram od : objectDiagrams)
				if (od.getOid().equals(obj.getOid()))
					od.setCurrentState(obj.getValue());
		}
		for (Composition.Runtime.Fragments.Fragment srv : compositionJAXB
				.getRuntime().getFragments().getFragment()) {
			for (ServiceDiagram sd : serviceDiagrams)
				if (sd.getSid().equals(srv.getSid()))
					sd.setCurrentState(srv.getValue());
		}

		List<ServiceAction> faults = new ArrayList<ServiceAction>();
		// We parse object fault events into fault actions according to effect
		// annotations
		// for(Composition.Objects.Object obj:
		// compositionJAXB.getObjects().getObject()){
		// for(String event: obj.getFaultEvent()){
		// for(Effect eff: effects){
		// if(eff.getOid().equals(obj.getOid()) && eff.getEvent().equals(event))
		// faults.add(new ServiceAction(eff.getSid(), eff.getAction()));
		// }
		// }
		// }

		//
		// for(Composition.Services.Service srv:
		// compositionJAXB.getServices().getService()){
		// for(String action: srv.getFaultAction()){
		// faults.add(new ServiceAction(srv.getSid(), action));
		// }
		// }

		// We parse control-flow requirements
		List<CFExpression> exprs = new ArrayList<CFExpression>();

		if (compositionJAXB.getControlFlowRequirements() != null)
			for (ControlFlowType expr : compositionJAXB
					.getControlFlowRequirements().getRequirement()) {

				// We know that every premise and result can have only ONE
				// clause
				// (though the List is returned)

				CFClause premise = parseCFClause(expr.getPremise().getClause()
						.get(0));
				Map<CFClause, Integer> result = new HashMap<CFClause, Integer>();
				for (ControlFlowType.Result res : expr.getResult()) {
					result.put(parseCFClause(res.getClause().get(0)),
							res.getPriority());
				}

				CFExpression ex = new CFExpression(premise, result);
				logger.debug(ex);

				exprs.add(ex);
			}

		try {
			CompositionProblem cp = new CompositionProblem(compositionName,
					objectDiagrams, serviceDiagrams, preconditions,
					objectPreconditions, aGoals, effects, syncPoints,
					nextActions, faults, exprs);
			return cp;
		} catch (InvalidCompositionSyncPointException
				| InvalidCompositionNextActionException
				| InvalidCompositionPriorityException
				| ObjectCurrentStateNullException
				| ServiceCurrentStateNullException
				| InvalidCompositionFaultException
				| InvalidCompositionAbstractGoalException
				| ObjectWithNoStatesException e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}

}
