package eu.fbk.das.composer.api.elements;

/**
 * represents object events of a composition problem (includes event_name and
 * object_id)
 */
public class ObjectEvent {
    private final String oid;
    private final String event;

    public ObjectEvent(String oid, String event) {
	this.oid = oid;
	this.event = event;
    }

    public String getOid() {
	return oid;
    }

    public String getEvent() {
	return event;
    }

    @Override
    public String toString() {
	return oid + "_" + event;
    }

}
