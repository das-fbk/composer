package eu.fbk.das.composer.api;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.fbk.das.composer.api.elements.SyncPoint;
import eu.fbk.das.process.engine.api.DomainObjectInstance;
import eu.fbk.das.process.engine.api.domain.ObjectDiagram;
import eu.fbk.das.process.engine.api.jaxb.Composition;
import eu.fbk.das.process.engine.api.jaxb.Composition.Fragments.Fragment.Assignment;
import eu.fbk.das.process.engine.api.jaxb.ObjectFactory;

/**
 * Builder for composition problems
 */
public class CompositionProblemBuilder {

    private static final Logger logger = LogManager
	    .getLogger(CompositionProblemBuilder.class);

    public String xsdPath;
    public String xmlPath;
    private static int sidCounter = 1;
    private final static String LINE_SEPARATOR = System
	    .getProperty("file.separator");

    private static int getNewSid() {
	return sidCounter++;
    }

    public CompositionProblemBuilder(String xsdPath, String xmlPath) {
	this.xsdPath = xsdPath;
	this.xmlPath = xmlPath;
    }

    /**
     * This method is used to generate a composition specification based on
     * runtime information
     * 
     * @param entities
     * @param service2eids
     * @param goal
     * @param domainObjectInstance
     * @return
     * @throws JAXBException
     * @throws IOException
     */
    public void buildComposition(String name,
	    List<DomainObjectInstance> entities,
	    Map<String, List<String>> service2eids,
	    Map<Integer, List<SyncPoint>> goal, DomainObjectInstance origin)
	    throws JAXBException, IOException {
	logger.debug("BuildComposition - start");
	Parser parser = new Parser(xsdPath, xmlPath);

	ObjectFactory of = new ObjectFactory();

	// ---------------------
	// This is the main part
	// ---------------------

	Composition comp = of.createComposition();
	comp.setDomainProperties(of.createCompositionDomainProperties());
	comp.setFragments(of.createCompositionFragments());
	comp.setSynchronization(of.createCompositionSynchronization());
	comp.setRuntime(of.createCompositionRuntime());
	comp.getRuntime().setDomainProperties(
		of.createCompositionRuntimeDomainProperties());
	comp.getRuntime().setFragments(of.createCompositionRuntimeFragments());

	// allObjects participating in the composition
	Set<String> oidsPart = new HashSet<String>();
	Map<String, String> oid2type = new HashMap<String, String>();
	Map<String, String> oid2state = new HashMap<String, String>();

	// for our convenience
	Map<String, List<ObjectDiagram>> eid2objects = new HashMap<String, List<ObjectDiagram>>();
	for (DomainObjectInstance e : entities) {
	    // eid2objects.put(e.getId(), e.getInternalKnowledge());
	    // todo capire se la interna va dentro o no
	    if (e.getExternalKnowledge() != null) {
		List<ObjectDiagram> kn = eid2objects.get(e.getId());
		if (kn == null) {
		    kn = new ArrayList<ObjectDiagram>();
		}
		// set current knowledge state using origin knowledge (es. user)
		for (ObjectDiagram ek : e.getExternalKnowledge()) {
		    if (!kn.contains(ek)) {
			kn.add(ek);
			// if we are handling origin
			if (e.getId().equals(origin.getId())) {
			    oid2state.put(ek.getOid(), ek.getCurrentState());
			}
		    }
		}
		eid2objects.put(e.getId(), kn);
	    }
	}

	// Services
	for (String srv : service2eids.keySet()) {
	    Map<String, String> oname2type = parser
		    .parseServiceAbstractObjects(srv);
	    Map<String, String> oname2oid = new HashMap<String, String>();
	    for (String oname : oname2type.keySet()) {
		// for(String eid: service2eids.get(srv)){
		for (String eid : eid2objects.keySet()) {
		    boolean isAssigned = false;
		    if (eid2objects.get(eid) != null)
			for (ObjectDiagram objD : eid2objects.get(eid)) {
			    if (objD.getType().equals(oname2type.get(oname))) {
				oname2oid.put(oname, objD.getOid());
				oidsPart.add(objD.getOid());
				oid2type.put(objD.getOid(), objD.getType());
				// oid2state.put(objD.getOid(),
				// objD.getCurrentState());
				isAssigned = true;
				break;
			    }
			}
		    if (isAssigned)
			break;
		}
	    }

	    eu.fbk.das.process.engine.api.jaxb.Composition.Fragments.Fragment service = of
		    .createCompositionFragmentsFragment();
	    String sid = "service" + getNewSid();
	    service.setSid(sid);
	    service.setType(srv);

	    // adding assignments
	    for (String oname : oname2oid.keySet()) {
		Assignment assign = of
			.createCompositionFragmentsFragmentAssignment();
		assign.setOName(oname);
		assign.setOid(oname2oid.get(oname));
		service.getAssignment().add(assign);
	    }

	    eu.fbk.das.process.engine.api.jaxb.Composition.Runtime.Fragments.Fragment rS = of
		    .createCompositionRuntimeFragmentsFragment();
	    rS.setSid(sid);
	    for (String s : parser.parseServiceDiagram(sid, srv, true)
		    .getInitialStates()) {
		rS.setValue(s);
		break;
	    }
	    comp.getRuntime().getFragments().getFragment().add(rS);
	    comp.getFragments().getFragment().add(service);
	}

	for (Integer prior : goal.keySet()) {

	    for (SyncPoint point : goal.get(prior)) {
		Composition.Synchronization.Point p = of
			.createCompositionSynchronizationPoint();
		p.setPriority(prior);

		// WARNING!!!!
		// HERE WE AGREE THAT FOR THE TIME BEING OID contains object
		// type!!!
		// In the FUTURE THIS MUST BE CHANGED!!!

		for (String oid : point.getOid2state().keySet()) {
		    eu.fbk.das.process.engine.api.jaxb.Composition.Synchronization.Point.DomainProperty tmpO = of
			    .createCompositionSynchronizationPointDomainProperty();
		    for (String eid : eid2objects.keySet()) {
			boolean isAssigned = false;
			if (eid2objects.get(eid) != null)
			    for (ObjectDiagram objD : eid2objects.get(eid)) {
				if (objD.getType().equals(oid)) {
				    tmpO.setOid(objD.getOid());
				    oidsPart.add(objD.getOid());
				    oid2type.put(objD.getOid(), objD.getType());
				    // oid2state.put(objD.getOid(),
				    // objD.getCurrentState());
				    isAssigned = true;
				    break;
				}
			    }
			if (isAssigned)
			    break;
		    }

		    tmpO.getState().addAll(point.getOid2state().get(oid));
		    p.getDomainProperty().add(tmpO);
		}
		comp.getSynchronization().getPoint().add(p);
	    }

	}

	// adding objects
	for (String oid : oidsPart) {
	    Composition.DomainProperties.DomainProperty object = of
		    .createCompositionDomainPropertiesDomainProperty();
	    object.setOid(oid);
	    object.setType(oid2type.get(oid));
	    comp.getDomainProperties().getDomainProperty().add(object);

	    Composition.Runtime.DomainProperties.DomainProperty rO = of
		    .createCompositionRuntimeDomainPropertiesDomainProperty();
	    rO.setOid(oid);
	    rO.setValue(oid2state.get(oid));
	    comp.getRuntime().getDomainProperties().getDomainProperty().add(rO);
	}

	// save composition to disk as xml
	String filename = writeCompositionToDisk(name, comp);
	logger.debug("BuildComposition - end - written in " + filename);

    }

    private String writeCompositionToDisk(String name, Composition comp)
	    throws JAXBException, PropertyException, IOException {
	if (name == null || xmlPath == null) {
	    throw new NullPointerException("xmlPath and name must be not null");
	}
	File dir = new File(xmlPath + LINE_SEPARATOR + "Compositions"
		+ LINE_SEPARATOR + name);
	if (!dir.exists()) {
	    // dir.mkdir();
	    FileUtils.forceMkdir(dir);
	}

	JAXBContext context = JAXBContext
		.newInstance("eu.fbk.das.process.engine.api.jaxb");
	Marshaller m = context.createMarshaller();

	m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
	m.setProperty(
		Marshaller.JAXB_SCHEMA_LOCATION,
		"http://soa.fbk.eu/Composition "
			+ System.getenv("CAPTEVO_HOME") + LINE_SEPARATOR
			+ "Schemata" + LINE_SEPARATOR + "Composition.xsd");
	m.marshal(comp, System.out);
	String filename = xmlPath + LINE_SEPARATOR + "Compositions"
		+ LINE_SEPARATOR + name + LINE_SEPARATOR + name + ".xml";
	m.marshal(comp, new FileOutputStream(filename));
	return filename;
    }

}
