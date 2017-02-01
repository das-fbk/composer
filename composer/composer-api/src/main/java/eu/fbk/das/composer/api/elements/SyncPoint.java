package eu.fbk.das.composer.api.elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * a single configuration (state) of a system of objects
 */
public class SyncPoint {
    private final Map<String, List<String>> oid2state;

    public SyncPoint(Map<String, List<String>> oid2state) {
	this.oid2state = oid2state;
    }

    public Map<String, List<String>> getOid2state() {
	return oid2state;
    }

    @Override
    public String toString() {
	return oid2state.toString();
    }

    @Override
    public SyncPoint clone() {
	Map<String, List<String>> o2s = new HashMap<String, List<String>>();
	for (String oid : oid2state.keySet()) {
	    o2s.put(oid, new ArrayList<String>(oid2state.get(oid)));
	}

	return new SyncPoint(o2s);
    }

}
