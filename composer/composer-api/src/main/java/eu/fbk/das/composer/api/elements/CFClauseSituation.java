package eu.fbk.das.composer.api.elements;

import java.util.List;
import java.util.Map;

/**
 * class for SITUATION-clause in control-flow requirements
 */
public class CFClauseSituation extends CFClause {
    final private Map<String, List<String>> oid2states;

    public CFClauseSituation(Map<String, List<String>> oid2states) {
	super(CFClauseType.SITUATION);
	this.oid2states = oid2states;
    }

    public Map<String, List<String>> getOid2states() {
	return oid2states;
    }

    @Override
    public String toString() {
	String str = "SITUATION<<(" + oid2states + ")>>";
	return str;
    }
}
