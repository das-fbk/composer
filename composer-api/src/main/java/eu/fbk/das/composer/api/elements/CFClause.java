package eu.fbk.das.composer.api.elements;

/**
 * superclass fir all control-flow requirement clauses
 */
abstract public class CFClause {
    final protected CFClauseType type;

    public CFClause(CFClauseType type) {
	this.type = type;
    }

    public CFClauseType getType() {
	return type;
    }

}
