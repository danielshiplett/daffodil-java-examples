package org.brewman;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URI;
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
		log.debug("icu version: {}", com.ibm.icu.util.VersionInfo.ICU_VERSION);

		ErrorHandler error = new ErrorHandler() {

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

		DaffodilXMLLoader loader = new DaffodilXMLLoader(error);
		loader.setValidation(true);

		URL resourceUrl = getClass().getResource("/" + TEST_SCHEMA);
		log.debug("url: {}", resourceUrl);
		URI tsURI = resourceUrl.toURI();
		File f = new File(tsURI);
		String enc = determineEncoding(f);
		log.debug("enc: {}", enc);
		Codec codec = new Codec(Charset.forName(enc));
		log.debug("codec: {}", codec);
		BufferedSource input = scala.io.Source.fromURI(tsURI, codec);
		Node origNode = loader.load(tsURI);

		log.debug("origNode: {}", origNode);

		Node someNode = ConstructingParser.fromSource(input, true).document()
				.docElem();

		log.debug("someNode: {}", someNode);

		// val res = (someNode, null, new InputSource(tsURI.toASCIIString()));

		Compiler c = new Compiler(true);
		ProcessorFactory pf = c.compile(someNode);

		log.debug("pf: {}", pf);

		URL dataUrl = getClass().getResource("/" + TEST_DATA);
		log.debug("url: {}", dataUrl);
		URI dataUri = dataUrl.toURI();
		File data = new File(dataUri);

		boolean isError = pf.isError();
		if (isError) {
			throw new Exception("error1");
		}

		DataProcessor p = pf.onPath("/");
		boolean pIsError = p.isError();
		if (pIsError) {
			throw new Exception("error2");
		}

		ParseResult actual = p.parse(data);

		if (actual.isError()) {
			throw new Exception("error3");
		}
	}

	private String determineEncoding(File theFile) throws FileNotFoundException {
		FileInputStream is = new FileInputStream(theFile);
		BufferedInputStream bis = new BufferedInputStream(is);
		String enc = EncodingHeuristics.readEncodingFromStream(bis);
		return enc;
	}
}
