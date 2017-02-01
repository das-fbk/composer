package eu.fbk.das.composer.util;

import java.io.File;
import java.util.HashMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * UnmarshallerHelper store in a local cache already performed unmarshalling
 */
public class UnmarshalHelper {

    private static final Logger logger = LogManager
	    .getLogger(UnmarshalHelper.class);

    static private HashMap<String, Unmarshaller> un_hash = new HashMap<String, Unmarshaller>();
    static private HashMap<String, Object> obj_hash = new HashMap<String, Object>();

    synchronized static public Object doUnmarshal(String ucontext, String jaxb,
	    String xsd, String xml) {
	String key = ucontext + "|" + jaxb + "|" + xsd + "|" + xml;
	if (obj_hash.containsKey(key)) {
	    return obj_hash.get(key);
	}
	Unmarshaller un = null;
	if (un_hash.containsKey(jaxb)) {
	    un = un_hash.get(jaxb);
	} else {
	    try {
		JAXBContext context;
		context = JAXBContext.newInstance(jaxb);
		un = context.createUnmarshaller();
	    } catch (JAXBException e) {
		logger.error(e.getMessage(), e);
	    }
	    un_hash.put(jaxb, un);
	}

	Object obj = null;
	try {
	    obj = un.unmarshal(new File(xml));
	} catch (JAXBException e) {
	    logger.error(e.getMessage(), e);
	}

	obj_hash.put(key, obj);
	return obj;
    }

}
