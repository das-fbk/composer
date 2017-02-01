package eu.fbk.das.composer.api.elements;

import java.util.Map;

/**
 * represents control-flow requirement
 */
public class CFExpression {
    final private CFClause premise;
    final private Map<CFClause, Integer> result;

    public CFExpression(CFClause premise, Map<CFClause, Integer> result) {
	this.premise = premise;
	this.result = result;
    }

    public CFClause getPremise() {
	return premise;
    }

    public Map<CFClause, Integer> getResult() {
	return result;
    }

    @Override
    public String toString() {
	String str = "REQUIREMENT\n<<<\n" + premise + "\n>>>\n->\n<<<\n";
	for (CFClause cl : result.keySet()) {
	    str += result.get(cl) + ":\n" + cl + "\n\n";
	}
	str += ">>>";
	return str;
    }
}
