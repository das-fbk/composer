package eu.fbk.das.composer.api.elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * object-based precondition of a service action
 */
public class Precondition {
    private final String sid;
    private final String action;

    private final List<Map<String, List<String>>> listOid2states;

    public Precondition(String sid, String action,
	    List<Map<String, List<String>>> oid2states) {
	this.sid = sid;
	this.action = action;
	this.listOid2states = oid2states;
    }

    public String getSid() {
	return sid;
    }

    public String getAction() {
	return action;
    }

    public boolean addOid2states(Map<String, List<String>> o2s) {
	return listOid2states.add(o2s);
    }

    public List<Map<String, List<String>>> getOid2states() {
	return listOid2states;
    }

    @Override
    public String toString() {
	return "P(" + sid + "_" + action + ")=" + listOid2states;
    }

    @Override
    protected Precondition clone() {
	Precondition pr = null;

	List<Map<String, List<String>>> list = new ArrayList<Map<String, List<String>>>();
	for (Map<String, List<String>> m : listOid2states) {
	    Map<String, List<String>> mNew = new HashMap<String, List<String>>();
	    for (String oid : m.keySet()) {
		mNew.put(oid, new ArrayList<String>(m.get(oid)));
	    }
	    list.add(mNew);
	}
	pr = new Precondition(this.sid, this.action, list);
	return pr;
    }

}
