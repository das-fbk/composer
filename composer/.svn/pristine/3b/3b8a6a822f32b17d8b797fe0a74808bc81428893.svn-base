package eu.fbk.das.composer.api.elements;

/**
 * effect of a service action
 */
public class Effect {
    private final String sid;
    private final String action;

    private final String oid;
    private final String event;

    public Effect(String sid, String action, String oid, String event) {
	this.sid = sid;
	this.action = action;
	this.oid = oid;
	this.event = event;
    }

    public String getSid() {
	return sid;
    }

    public String getAction() {
	return action;
    }

    public String getOid() {
	return oid;
    }

    public String getEvent() {
	return event;
    }

    @Override
    public String toString() {
	return "E(" + sid + "_" + action + ")=" + oid + "_" + event;
    }

}
