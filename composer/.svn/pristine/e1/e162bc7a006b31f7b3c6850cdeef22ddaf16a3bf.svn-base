package eu.fbk.das.composer.api.elements;

import java.util.List;

/**
 * class for OR-clause in control-flow requirements
 */
public class CFClauseOr extends CFClause {
    final private List<CFClause> clauses;

    public CFClauseOr(List<CFClause> clauses) {
	super(CFClauseType.AND);
	this.clauses = clauses;
    }

    public List<CFClause> getClauses() {
	return clauses;
    }

    @Override
    public String toString() {
	String str = "OR<<\n";
	for (CFClause cl : clauses) {
	    str += "(" + cl + ")\n";
	}
	str += ">>";
	return str;
    }

}
