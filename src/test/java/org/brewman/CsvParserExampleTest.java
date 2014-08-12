package org.brewman;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;

import org.jdom2.Document;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import scala.io.Codec;
import scala.xml.include.sax.EncodingHeuristics;
import edu.illinois.ncsa.daffodil.japi.Compiler;
import edu.illinois.ncsa.daffodil.japi.Daffodil;
import edu.illinois.ncsa.daffodil.japi.DataProcessor;
import edu.illinois.ncsa.daffodil.japi.ParseResult;
import edu.illinois.ncsa.daffodil.japi.ProcessorFactory;

public class CsvParserExampleTest {
	private static final Logger log = LoggerFactory
			.getLogger(CsvParserExampleTest.class);

	private static final String TEST_SCHEMA = "csv.dfdl.xsd";
	private static final String TEST_DATA = "sample.csv";

	@Test
	public void test() throws Exception {
		/**
		 * Get the version of ICU4J.
		 */
		log.debug("icu version: {}", com.ibm.icu.util.VersionInfo.ICU_VERSION);

		/**
		 * Get a URL for the test schema resource. We'll use it later to get the
		 * file or load the XML or whatever...
		 */
		URL schemaUrl = getClass().getResource("/" + TEST_SCHEMA);
		log.debug("url: {}", schemaUrl);

		/**
		 * Or open the File from the URL.
		 */
		File schemaFile = new File(schemaUrl.toURI());

		/**
		 * Create a Daffodil Compiler. Make sure it validates?
		 */
		Compiler c = Daffodil.compiler();
		c.setValidateDFDLSchemas(true);

		/**
		 * And then compile the schema. Why the original vs. the other XML? No
		 * idea...
		 * 
		 * But we get a ProcessorFactory from it.
		 */
		File[] schemaFiles = new File[] { schemaFile };
		ProcessorFactory pf = c.compile(schemaFiles);
		log.debug("pf: {}", pf);

		/**
		 * See if there was an error yet?
		 */
		if (pf.isError()) {
			throw new Exception("error1");
		}

		/**
		 * Get a DataProcessor from the ProcessorFactory. I guess the parameter
		 * tells it where to start processing from? Why XPath here?
		 */
		DataProcessor p = pf.onPath("/");

		/**
		 * Again, check if there is an error with the DataProcessor?
		 */
		if (p.isError()) {
			throw new Exception("error2");
		}

		/**
		 * Get a URL for the test data resource.
		 */
		URL dataUrl = getClass().getResource("/" + TEST_DATA);
		log.debug("url: {}", dataUrl);

		/**
		 * Get a File for the test data.
		 */
		File dataFile = new File(dataUrl.toURI());
		FileInputStream fis = new FileInputStream(dataFile);
		ReadableByteChannel rbc = Channels.newChannel(fis);

		/**
		 * Now finally try to parse the sample data.
		 */
		ParseResult actual = p.parse(rbc);

		/**
		 * And see if there is an error again?
		 */
		if (actual.isError()) {
			throw new Exception("error3");
		}

		Document doc = actual.result();
		XMLOutputter xo = new XMLOutputter();
		xo.setFormat(Format.getPrettyFormat());
		xo.output(doc, System.out);
	}

	private ErrorHandler getSimpleErrorHandler() {
		return new ErrorHandler() {

			@Override
			public void warning(SAXParseException exception)
					throws SAXException {
				log.warn(exception.getMessage(), exception);
			}

			@Override
			public void error(SAXParseException exception) throws SAXException {
				log.error(exception.getMessage(), exception);
			}

			@Override
			public void fatalError(SAXParseException exception)
					throws SAXException {
				log.error(exception.getMessage(), exception);
			}
		};
	}

	private String determineEncoding(File theFile) throws FileNotFoundException {
		FileInputStream is = new FileInputStream(theFile);
		BufferedInputStream bis = new BufferedInputStream(is);
		String enc = EncodingHeuristics.readEncodingFromStream(bis);
		return enc;
	}

	private Codec encodingToCodec(String encoding) {
		return new Codec(Charset.forName(encoding));
	}
}
