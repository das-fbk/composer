package eu.fbk.das.composer;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.junit.Test;

import eu.fbk.das.composer.api.ComposerInterface;
import eu.fbk.das.composer.api.CompositionProblem;
import eu.fbk.das.composer.api.CompositionProblemBuilder;
import eu.fbk.das.composer.api.CompositionStatus;
import eu.fbk.das.composer.api.Parser;
import eu.fbk.das.composer.api.elements.SyncPoint;
import eu.fbk.das.composer.api.exceptions.CompositionDuplicateOidException;
import eu.fbk.das.composer.api.exceptions.CompositionDuplicateSidException;
import eu.fbk.das.composer.api.exceptions.InvalidCompositionEffectException;
import eu.fbk.das.composer.api.exceptions.InvalidCompositionPreconditionException;
import eu.fbk.das.composer.api.exceptions.InvalidServiceCurrentStateException;
import eu.fbk.das.composer.api.exceptions.InvalidServiceObjectAssignmentException;
import eu.fbk.das.composer.api.exceptions.ServiceGroundingTypeMismatchException;
import eu.fbk.das.composer.impl.Composer;
import eu.fbk.das.process.engine.api.DomainObjectInstance;
import eu.fbk.das.process.engine.api.domain.exceptions.InvalidObjectCurrentStateException;

public class ComposerTest {

    private static final String LINE_SEPARATOR = System
	    .getProperty("file.separator");

    @Test
    public void composizioneDuePassiTest()
	    throws CompositionDuplicateOidException,
	    CompositionDuplicateSidException,
	    InvalidCompositionPreconditionException,
	    InvalidCompositionEffectException,
	    InvalidObjectCurrentStateException,
	    InvalidServiceCurrentStateException,
	    InvalidServiceObjectAssignmentException,
	    ServiceGroundingTypeMismatchException {

	String captevoHome = "C:\\Lavoro\\workspace\\soa\\composer\\composer-impl\\src\\test\\resources\\eu\\fbk\\das\\composer\\composizioneduepassi";

	String projectName = "collectiveUrbanMobility";
	String compositionName = "composition";

	Parser parser = new Parser(captevoHome + LINE_SEPARATOR + "Schemata"
		+ LINE_SEPARATOR, captevoHome + LINE_SEPARATOR + projectName
		+ LINE_SEPARATOR);

	CompositionProblem cp = parser.parseCompositionProblem(compositionName);

	ComposerInterface composer = new Composer(captevoHome + LINE_SEPARATOR
		+ projectName + LINE_SEPARATOR);

	CompositionProblem ocp = cp.clone();

	System.out.println("Service composition started...\n");
	long startTime = System.currentTimeMillis();
	CompositionStatus cs = composer.compose(ocp, compositionName, false,
		false, captevoHome);
	long endTime = System.currentTimeMillis();
	System.out.println("Composition Status: "
		+ composer.getStatusMessage(cs));
	System.out.println("Full Composition time: "
		+ ((float) (endTime - startTime)) / 1000 + " seconds\n");

	System.out.println("Result in " + composer.getFolderPath());

	assertTrue(outputFilesExist(composer.getFolderPath(), compositionName));
    }

    private boolean outputFilesExist(String dir, String compositionName) {
	return Files.exists(Paths.get(dir + LINE_SEPARATOR + "Compositions"
		+ LINE_SEPARATOR + compositionName + LINE_SEPARATOR
		+ compositionName + ".smv"))
		&& Files.exists(Paths.get(dir + LINE_SEPARATOR + "Compositions"
			+ LINE_SEPARATOR + compositionName + LINE_SEPARATOR
			+ compositionName + ".dot"));
    }

    @Test(expected = NullPointerException.class)
    public void testNullCompositionProblemBuilder() throws JAXBException,
	    IOException {
	CompositionProblemBuilder cpb = new CompositionProblemBuilder(null,
		null);
	Map<Integer, List<SyncPoint>> goal = null;
	cpb.buildComposition(null, null, null, goal, null);
    }

    @Test()
    public void testSimpleCompositionProblemBuilder() throws JAXBException,
	    IOException {

	CompositionProblemBuilder cpb = new CompositionProblemBuilder(null,
		"target");
	cpb.buildComposition("hg", new ArrayList<DomainObjectInstance>(),
		new HashMap<String, List<String>>(),
		new HashMap<Integer, List<SyncPoint>>(), null);
    }

    @Test
    public void composerDueFrammentiTest()
	    throws CompositionDuplicateOidException,
	    CompositionDuplicateSidException,
	    InvalidCompositionPreconditionException,
	    InvalidCompositionEffectException,
	    InvalidObjectCurrentStateException,
	    InvalidServiceCurrentStateException,
	    InvalidServiceObjectAssignmentException,
	    ServiceGroundingTypeMismatchException {

	String captevoHome = "C:\\Lavoro\\workspace\\soa\\composer\\composer-impl\\src\\test\\resources\\eu\\fbk\\das\\composer\\composizionedueframmenti";

	String projectName = "collectiveUrbanMobility";
	String compositionName = "composition";

	Parser parser = new Parser(captevoHome + LINE_SEPARATOR + "Schemata"
		+ LINE_SEPARATOR, captevoHome + LINE_SEPARATOR + projectName
		+ LINE_SEPARATOR);

	CompositionProblem cp = parser.parseCompositionProblem(compositionName);

	ComposerInterface composer = new Composer(captevoHome + LINE_SEPARATOR
		+ projectName + LINE_SEPARATOR);

	CompositionProblem ocp = cp.clone();

	System.out.println("Service composition started...\n");
	long startTime = System.currentTimeMillis();
	CompositionStatus cs = composer.compose(ocp, compositionName, false,
		false, captevoHome);
	long endTime = System.currentTimeMillis();
	System.out.println("Composition Status: "
		+ composer.getStatusMessage(cs));
	System.out.println("Full Composition time: "
		+ ((float) (endTime - startTime)) / 1000 + " seconds\n");

	System.out.println("Result in " + composer.getFolderPath());

	assertTrue(outputFilesExist(composer.getFolderPath(), compositionName));
    }
}
