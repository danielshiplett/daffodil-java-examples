package org.brewman;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.nio.charset.Charset;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import scala.io.BufferedSource;
import scala.io.Codec;
import scala.xml.Node;
import scala.xml.include.sax.EncodingHeuristics;
import scala.xml.parsing.ConstructingParser;
import edu.illinois.ncsa.daffodil.api.DFDL.DataProcessor;
import edu.illinois.ncsa.daffodil.api.DFDL.ParseResult;
import edu.illinois.ncsa.daffodil.compiler.Compiler;
import edu.illinois.ncsa.daffodil.compiler.ProcessorFactory;
import edu.illinois.ncsa.daffodil.xml.DaffodilXMLLoader;

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
		 * Get a Daffofil XML Loader. It loads the schema XML differently?
		 */
		DaffodilXMLLoader loader = new DaffodilXMLLoader(
				getSimpleErrorHandler());
		loader.setValidation(true);

		/**
		 * Use the loader to get an XML 'Node'.
		 */
		Node origNode = loader.load(schemaUrl.toURI());
		log.debug("origNode: {}", origNode);

		/**
		 * Or open the File from the URL.
		 */
		File f = new File(schemaUrl.toURI());

		/**
		 * We needed the File to determine the encoding I guess?
		 */
		Codec codec = encodingToCodec(determineEncoding(f));
		log.debug("codec: {}", codec);

		/**
		 * Otherwise, get the file as a Source. This is what needed the
		 * encoding.
		 */
		BufferedSource input = scala.io.Source
				.fromURI(schemaUrl.toURI(), codec);

		/**
		 * This also gets an XML 'Node'. Note that it appears to be way less
		 * verbose than getting it the original way....
		 */
		Node someNode = ConstructingParser.fromSource(input, true).document()
				.docElem();
		log.debug("someNode: {}", someNode);

		/**
		 * Create a Daffodil Compiler. Make sure it validates?
		 */
		Compiler c = new Compiler(true);

		/**
		 * And then compile the schema. Why the original vs. the other XML? No
		 * idea...
		 * 
		 * But we get a ProcessorFactory from it.
		 */
		ProcessorFactory pf = c.compile(origNode);
		log.debug("schema valid: {}", c.validateDFDLSchemas());
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
		File data = new File(dataUrl.toURI());

		/**
		 * Now finally try to parse the sample data.
		 */
		ParseResult actual = p.parse(data);

		/**
		 * And see if there is an error again?
		 */
		if (actual.isError()) {
			throw new Exception("error3");
		}
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
