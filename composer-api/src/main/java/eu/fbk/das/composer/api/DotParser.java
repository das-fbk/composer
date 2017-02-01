package eu.fbk.das.composer.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.fbk.das.process.engine.api.domain.ServiceDiagram;
import eu.fbk.das.process.engine.api.domain.ServiceDiagramActionType;
import eu.fbk.das.process.engine.api.domain.ServiceTransitionGlobal;

/**
 * Contains methods for parsing dot file with subprocesses (for a version of
 * wsynth that plans for a number of initial states at a time and outputs a set
 * of subprocesses)
 */
public class DotParser {

    private static final Logger logger = LogManager.getLogger(DotParser.class);

    /**
     * builds a proces from a set of unconnected subprocesses
     * 
     * @param inFile
     *            path of a raw file with subprocesses
     * @param outFile
     *            path to store the process built
     */
    public static void buildProcess(String inFile) {
	File f = new File(inFile);
	if (!f.exists()) {
	    return;
	}
	String main = "";
	String main2 = "";
	FileReader fr;
	BufferedReader br;
	try {
	    fr = new FileReader(inFile);
	    br = new BufferedReader(fr);

	    // Here we read the file

	    String str = br.readLine();
	    while (!str.equals("digraph plan_fsm {")) {
		main2 += str + "\n";
		str = br.readLine();
	    }

	    while (str != null) {
		main += str + "\n";
		main2 += str + "\n";
		str = br.readLine();
	    }

	} catch (FileNotFoundException e) {
	    logger.error(e.getMessage(), e);
	} catch (IOException e) {
	    logger.error(e.getMessage(), e);
	}

	// creating a command file
	FileWriter writer;
	try {
	    writer = new FileWriter(inFile);
	    writer.write(main);
	    writer.close();
	} catch (IOException e) {
	    logger.error(e.getMessage(), e);
	}

	try {
	    writer = new FileWriter(inFile.substring(0, inFile.length() - 4)
		    + "_bk.dot");
	    writer.write(main2);
	    writer.close();
	} catch (IOException e) {
	    logger.error(e.getMessage(), e);
	}

    }

    /**
     * @param dot
     *            the path to the dot.exe
     * @param inFile
     * @param outFile
     */
    public static void dot2ps(String dot, String inFile, String outFile) {

	// creating a command file
	FileWriter writer;
	try {
	    writer = new FileWriter("dot2ps.cmd");
	    writer.write("\"" + dot + "\" -Tps " + "\"" + inFile + "\"" + " -o"
		    + "\"" + outFile + "\"");
	    writer.close();
	} catch (IOException e) {
	    logger.error(e.getMessage(), e);
	}

	// generating ps from dot
	try {
	    Process p = Runtime.getRuntime().exec("dot2ps.cmd");
	    InputStream stderr = p.getErrorStream();
	    boolean running = true;
	    while (running) {
		try {
		    int c = stderr.read();
		    if (c != -1)
			System.out.print((char) c);
		    else
			running = false;
		} catch (IOException e) {
		    logger.error(e.getMessage(), e);
		}
	    }
	} catch (IOException e) {
	    logger.error(e.getMessage(), e);
	}
	// deleting the command file
	// File f = new File("dot2ps.cmd");
	// f.delete();
    }

    /**
     * parses a set of string and returns corresponding service transitions
     * 
     * @param strs
     *            list of strings to parse
     * @return list of service transitions parsed
     */
    public static List<ServiceTransitionGlobal> parseDotProcess(
	    List<String> strs, CompositionProblem cp) {
	List<ServiceTransitionGlobal> trs = new ArrayList<ServiceTransitionGlobal>();
	for (String str : strs) {
	    if (str.contains("\" -> \""))
		trs.add(parseDotTransition(str, cp));
	}
	return trs;
    }

    /**
     * parses a file and returns corresponding a list of service transitions
     * 
     * @param inFile
     *            path of the file to parse
     * @return list of service transitions parsed
     */
    public static List<ServiceTransitionGlobal> parseDotProcess(String inFile,
	    CompositionProblem cp) {
	FileReader fr = null;
	BufferedReader br = null;
	List<String> strs = new ArrayList<String>();
	try {
	    fr = new FileReader(inFile);
	    br = new BufferedReader(fr);

	    // Here we read the file

	    String str = br.readLine();

	    while (str != null) {
		strs.add(str);
		str = br.readLine();
	    }

	} catch (FileNotFoundException e) {
	    logger.error(e.getMessage(), e);
	} catch (IOException e) {
	    logger.error(e.getMessage(), e);
	} finally {
	    if (br != null) {
		try {
		    br.close();
		} catch (IOException e) {
		    logger.error(e.getMessage(), e);
		}
	    }
	}

	return parseDotProcess(strs, cp);
    }

    private static ServiceTransitionGlobal parseDotTransition(String str,
	    CompositionProblem cp) {
	Pattern p = Pattern.compile("\"[\\d]+\"");
	Matcher m = p.matcher(str);
	m.find();
	String from = m.group().replaceAll("\"", "");
	m.find(1);
	String to = m.group().replaceAll("\"", "");

	p = Pattern.compile("(dn_main.input)|(dn_main.output)");
	m = p.matcher(str);
	m.find();
	ServiceDiagramActionType type = (m.group().equals("dn_main.input") ? ServiceDiagramActionType.IN
		: ServiceDiagramActionType.OUT);

	p = Pattern.compile("( = )\\S+[\\s\"]");
	m = p.matcher(str);
	m.find();
	String s = m.group().substring(3, m.group().length() - 1);
	String sid = s.substring(0, s.indexOf("_"));
	String action = s.substring(s.indexOf("_") + 1, s.length());

	// understanding serviceType

	String sType = "";
	for (ServiceDiagram sd : cp.getServiceDiagrams()) {
	    if (sd.getSid().equals(sid)) {
		sType = sd.getType();
		break;
	    }
	}

	return new ServiceTransitionGlobal(from, to, action, type, sid, sType);
    }
}
