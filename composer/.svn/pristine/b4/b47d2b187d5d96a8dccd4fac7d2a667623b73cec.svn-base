package eu.fbk.das.composer.api;

/**
 * General interface for a Composer
 */
public interface ComposerInterface {

    /**
     * Solve composition problem
     * 
     * @param problem
     *            - specific {@link CompositionProblem} to solve
     * @param outputName
     *            - name of resulting file
     * @param showImprobable
     * @param optimize
     *            - true/false to controlo optimization process
     * @return
     */
    public CompositionStatus compose(CompositionProblem problem,
	    String outputName, boolean showImprobable, boolean optimize,
	    String captevoHome);

    /**
     * Call optimization for composition
     * 
     * @param ocp
     * @return
     */
    public CompositionProblem optimize(CompositionProblem ocp);

    /**
     * Return status message after composition
     * 
     * @param cs
     * @return
     */
    public String getStatusMessage(CompositionStatus cs);

    /**
     * Used only for test
     * 
     * @return where composition result are
     */
    public String getFolderPath();

}
