package eu.fbk.das.composer.api.elements;

import java.util.List;
import java.util.Map;

/**
 * class for EVENT-clause in control-flow requirements
 */
public class CFClauseEvent extends CFClause {
    final private String oid;
    final private String event;
    final private Map<String, List<String>> condition;

    public CFClauseEvent(String oid, String event,
	    Map<String, List<String>> condition) {
	super(CFClauseType.EVENT);
	this.oid = oid;
	this.event = event;
	this.condition = condition;
    }

    public String getOid() {
	return oid;
    }

    public String getEvent() {
	return event;
    }

    public Map<String, List<String>> getCondition() {
	return condition;
    }

    @Override
    public String toString() {
	String str = "EVENT<<(" + oid + "." + event + ")";
	if (condition != null)
	    str += "--->C(" + condition + ")";
	str += ">>";
	return str;
    }

}
