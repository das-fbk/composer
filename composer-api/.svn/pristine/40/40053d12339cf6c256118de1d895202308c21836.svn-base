package eu.fbk.das.composer.api.elements;

import java.util.List;

/**
 * class for ACTION-clause in control-flow requirements
 */
public class CFClauseThen extends CFClause {
    final private List<CFClause> clauses;

    public CFClauseThen(List<CFClause> clauses) {
	super(CFClauseType.THEN);
	this.clauses = clauses;
    }

    public List<CFClause> getClauses() {
	return clauses;
    }

    @Override
    public String toString() {
	String str = "THEN<<\n";
	for (CFClause cl : clauses) {
	    str += "(" + cl + ")\n";
	}
	str += ">>";
	return str;
    }

}
