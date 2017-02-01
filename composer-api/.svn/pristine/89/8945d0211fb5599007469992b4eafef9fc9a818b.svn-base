package eu.fbk.das.composer.api.elements;

/**
 * It represents a service action when referred outside a ServiceDiagram. It
 * includes service instance identifier sid and internal action name action. The
 * unique action identifier is generated as [sid]_[action]
 */
public class ServiceAction {
    private final String sid;
    private final String action;

    public ServiceAction(String sid, String action) {
	this.sid = sid;
	this.action = action;
    }

    public String getSid() {
	return sid;
    }

    public String getAction() {
	return action;
    }

    /**
     * returns an action identifier that is uniquie within the composition
     * problem
     * 
     * @return a string [sid]_[action]
     */
    public String getUniqueAction() {
	return sid + "_" + action;
    }

    @Override
    public boolean equals(Object arg0) {
	ServiceAction a = (ServiceAction) arg0;
	if (a != null && a.getUniqueAction().equals(this.getUniqueAction()))
	    return true;
	else
	    return false;
    }

    @Override
    public String toString() {
	return getUniqueAction();
    }

}
