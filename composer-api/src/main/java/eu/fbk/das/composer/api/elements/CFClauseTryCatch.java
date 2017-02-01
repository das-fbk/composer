package eu.fbk.das.composer.api.elements;

import java.util.Map;

/**
 * class for OR-clause in control-flow requirements
 */
public class CFClauseTryCatch extends CFClause {
    final private CFClause tryCl;
    final private Map<CFClause, CFClause> catchClauses;

    public CFClauseTryCatch(CFClause tryCl, Map<CFClause, CFClause> catchClauses) {
	super(CFClauseType.TRYCATCH);
	this.tryCl = tryCl;
	this.catchClauses = catchClauses;
    }

    public CFClause getTryCl() {
	return tryCl;
    }

    public Map<CFClause, CFClause> getCatchClauses() {
	return catchClauses;
    }

    @Override
    public String toString() {
	String str = "TRYCATCH<<<\nTRY<<\n" + tryCl + "\n>>\n";

	for (CFClause cat : catchClauses.keySet()) {
	    str += "CATCH<<\nCONDITION<\n" + cat + "\n>\nREACTION<\n"
		    + catchClauses.get(cat) + "\n>\n>>\n";
	}

	str += ">>>";
	return str;
    }

}
