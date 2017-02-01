package eu.fbk.das.composer.api.elements;

import eu.fbk.das.composer.api.exceptions.CFPolicyInvalidConstraintException;
import eu.fbk.das.composer.api.exceptions.CFPolicyInvalidPremiseException;

/**
 * represents business policies. In fact, it is very similar to CFExpression,
 * but imposes restrictions on the type of premise and result. Moreover, the
 * result contains only one clause, no preferences. The semantics is different:
 * the premise situation can happen only if the constraint is satisfied.
 * Otherwise the application execution fails.
 */
public class CFPolicy {
    final private CFClause premise;
    final private CFClause constraint;

    public CFPolicy(CFClause premise, CFClause constraint)
	    throws CFPolicyInvalidPremiseException,
	    CFPolicyInvalidConstraintException {
	if (premise.getType() != CFClauseType.ACTION
		&& premise.getType() != CFClauseType.EVENT)
	    throw new CFPolicyInvalidPremiseException();
	if (constraint.getType() != CFClauseType.SITUATION)
	    throw new CFPolicyInvalidConstraintException();
	this.premise = premise;
	this.constraint = constraint;
    }

    public CFClause getPremise() {
	return premise;
    }

    public CFClause getConstraint() {
	return constraint;
    }

    @Override
    public String toString() {
	String str = "POLICY\n<<<\n" + premise + "\n>>>\n->\n<<<\n";
	str += constraint + "\n";
	str += ">>>";
	return str;
    }
}
