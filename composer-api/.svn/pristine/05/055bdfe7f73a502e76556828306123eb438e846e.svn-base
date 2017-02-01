package eu.fbk.das.composer.api.elements;

import java.util.ArrayList;
import java.util.List;

/**
 * object-based precondition of a service action
 */
public class Compensation {
    private final String sid;
    private final String action;

    private final List<SyncPoint> goalPoints;

    public Compensation(String sid, String action, List<SyncPoint> goalPoints) {
	this.sid = sid;
	this.action = action;
	this.goalPoints = goalPoints;
    }

    public String getSid() {
	return sid;
    }

    public String getAction() {
	return action;
    }

    public List<SyncPoint> getGoalPoints() {
	return goalPoints;
    }

    @Override
    public String toString() {
	return "C(" + sid + "_" + action + ")=" + goalPoints;
    }

    @Override
    public Compensation clone() {
	Compensation pr = null;

	List<SyncPoint> list = new ArrayList<SyncPoint>();
	for (SyncPoint sp : goalPoints) {
	    list.add(sp.clone());
	}
	return new Compensation(sid, action, list);

    }

}
