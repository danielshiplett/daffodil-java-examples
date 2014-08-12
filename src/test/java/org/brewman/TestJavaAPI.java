package org.brewman;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.jdom2.output.Format;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.illinois.ncsa.daffodil.japi.ConsoleLogWriter;
import edu.illinois.ncsa.daffodil.japi.Daffodil;
import edu.illinois.ncsa.daffodil.japi.DataProcessor;
import edu.illinois.ncsa.daffodil.japi.Diagnostic;
import edu.illinois.ncsa.daffodil.japi.LocationInSchemaFile;
import edu.illinois.ncsa.daffodil.japi.LogLevel;
import edu.illinois.ncsa.daffodil.japi.LogWriter;
import edu.illinois.ncsa.daffodil.japi.ParseResult;
import edu.illinois.ncsa.daffodil.japi.ProcessorFactory;

public class TestJavaAPI {
	private static final Logger log = LoggerFactory
			.getLogger(TestJavaAPI.class);

	private LogWriter lw = new TestLogWriter();

	public java.io.File getResource(String resPath) {
		try {
			return new java.io.File(this.getClass().getResource(resPath)
					.toURI());
		} catch (Exception e) {
			return null;
		}
	}

	private static class TestLogWriter extends LogWriter {
		@Override
		public void write(LogLevel level, String logID, String msg) {
			switch (level) {
			case Error:
				log.error(msg);
				break;
			case Warning:
				log.warn(msg);
				break;
			case Info:
				log.info(msg);
				break;
			case Debug:
				log.debug(msg);
				break;
			default:
				log.error(msg);
			}
		}
	}

	@Test
	@Ignore
	public void testJavaAPI1() throws IOException {
		DebuggerRunnerForJAPITest debugger = new DebuggerRunnerForJAPITest();

		Daffodil.setLogWriter(lw);
		Daffodil.setLoggingLevel(LogLevel.Debug);
		Daffodil.setDebugging(true);

		edu.illinois.ncsa.daffodil.japi.Compiler c = Daffodil.compiler();
		c.setValidateDFDLSchemas(false);
		java.io.File[] schemaFiles = new java.io.File[2];
		schemaFiles[0] = getResource("/mySchema1.dfdl.xsd");
		schemaFiles[1] = getResource("/mySchema2.dfdl.xsd");
		ProcessorFactory pf = c.compile(schemaFiles);
		DataProcessor dp = pf.onPath("/");
		java.io.File file = getResource("/myData.dat");
		java.io.FileInputStream fis = new java.io.FileInputStream(file);
		java.nio.channels.ReadableByteChannel rbc = java.nio.channels.Channels
				.newChannel(fis);
		ParseResult res = dp.parse(rbc, 2 << 3);
		boolean err = res.isError();
		if (!err) {
			org.jdom2.Document doc = res.result();
			org.jdom2.output.XMLOutputter xo = new org.jdom2.output.XMLOutputter();
			xo.setFormat(Format.getPrettyFormat());
			xo.output(doc, System.out);
		}
		java.util.List<Diagnostic> diags = res.getDiagnostics();
		for (Diagnostic d : diags) {
			System.err.println(d.getMessage());
		}
		assertTrue(res.location().isAtEnd());
		assertTrue(debugger.lines.size() > 0);
		assertTrue(debugger.lines
				.contains("----------------------------------------------------------------- 1\n"));

		// reset the global logging and debugger state
		Daffodil.setLogWriter(new ConsoleLogWriter());
		Daffodil.setLoggingLevel(LogLevel.Info);
		Daffodil.setDebugging(false);
		Daffodil.setDebugger(null);
	}

	@Test
	@Ignore
	public void testJavaAPI2() throws IOException {
		Daffodil.setLogWriter(lw);
		Daffodil.setLoggingLevel(LogLevel.Info);

		edu.illinois.ncsa.daffodil.japi.Compiler c = Daffodil.compiler();
		c.setValidateDFDLSchemas(false);
		java.io.File[] schemaFiles = new java.io.File[2];
		schemaFiles[0] = getResource("/test/japi/mySchema1.dfdl.xsd");
		schemaFiles[1] = getResource("/test/japi/mySchema2.dfdl.xsd");
		ProcessorFactory pf = c.compile(schemaFiles);
		DataProcessor dp = pf.onPath("/");
		java.io.File file = getResource("/test/japi/myDataBroken.dat");
		java.io.FileInputStream fis = new java.io.FileInputStream(file);
		java.nio.channels.ReadableByteChannel rbc = java.nio.channels.Channels
				.newChannel(fis);
		ParseResult res = dp.parse(rbc);
		try {
			org.jdom2.Document doc = res.result();
			fail("did not throw");
			org.jdom2.output.XMLOutputter xo = new org.jdom2.output.XMLOutputter();
			xo.setFormat(Format.getPrettyFormat());
			xo.output(doc, System.out);
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("no result"));
		}
		assertTrue(res.isError());
		java.util.List<Diagnostic> diags = res.getDiagnostics();
		assertEquals(1, diags.size());
		Diagnostic d = diags.get(0);
		System.err.println(d.getMessage());
		assertTrue(d.getMessage().contains("int"));
		assertTrue(d.getMessage().contains("Not an int"));
		assertTrue(d.getDataLocations().toString().contains("10"));
		java.util.List<LocationInSchemaFile> locs = d
				.getLocationsInSchemaFiles();
		assertEquals(1, locs.size());
		LocationInSchemaFile loc = locs.get(0);
		assertTrue(loc.toString().contains("mySchema2.dfdl.xsd"));

		// reset the global logging state
		Daffodil.setLogWriter(new ConsoleLogWriter());
		Daffodil.setLoggingLevel(LogLevel.Info);
	}

	/**
	 * Verify that we can detect when the parse did not consume all the data.
	 * 
	 * @throws IOException
	 */
	@Test
	@Ignore
	public void testJavaAPI3() throws IOException {
		edu.illinois.ncsa.daffodil.japi.Compiler c = Daffodil.compiler();
		c.setValidateDFDLSchemas(false);
		java.io.File[] schemaFiles = new java.io.File[1];
		schemaFiles[0] = getResource("/test/japi/mySchema3.dfdl.xsd");
		ProcessorFactory pf = c.compile(schemaFiles);
		pf.setDistinguishedRootNode("e3", null);
		DataProcessor dp = pf.onPath("/");
		java.io.File file = getResource("/test/japi/myData16.dat");
		java.io.FileInputStream fis = new java.io.FileInputStream(file);
		java.nio.channels.ReadableByteChannel rbc = java.nio.channels.Channels
				.newChannel(fis);
		ParseResult res = dp.parse(rbc, 16 << 3);
		boolean err = res.isError();
		org.jdom2.output.XMLOutputter xo = new org.jdom2.output.XMLOutputter();
		xo.setFormat(Format.getPrettyFormat());
		java.util.List<Diagnostic> diags = res.getDiagnostics();
		for (Diagnostic d : diags) {
			System.err.println(d.getMessage());
		}
		if (!err) {
			org.jdom2.Document doc = res.result();
			xo.output(doc, System.out);
		}
		assertFalse(err);
		assertFalse(res.location().isAtEnd());
	}

	@Test
	@Ignore
	public void testJavaAPI4b() throws IOException {
		edu.illinois.ncsa.daffodil.japi.Compiler c = Daffodil.compiler();
		c.setValidateDFDLSchemas(false);
		File[] schemaFileNames = new File[1];
		schemaFileNames[0] = getResource("/test/japi/mySchema3.dfdl.xsd");
		c.setDistinguishedRootNode("e4", null);
		ProcessorFactory pf = c.compile(schemaFileNames);
		DataProcessor dp = pf.onPath("/");
		java.io.File file = getResource("/test/japi/myData2.dat");
		java.io.FileInputStream fis = new java.io.FileInputStream(file);
		java.nio.channels.ReadableByteChannel rbc = java.nio.channels.Channels
				.newChannel(fis);
		ParseResult res = dp.parse(rbc, 64 << 3);
		boolean err = res.isError();
		org.jdom2.output.XMLOutputter xo = new org.jdom2.output.XMLOutputter();
		xo.setFormat(Format.getPrettyFormat());
		java.util.List<Diagnostic> diags = res.getDiagnostics();
		for (Diagnostic d : diags) {
			System.err.println(d.getMessage());
		}
		if (!err) {
			org.jdom2.Document doc = res.result();
			xo.output(doc, System.out);
		}
		assertFalse(err);
		assertFalse(res.location().isAtEnd());
	}

	@Test
	@Ignore
	public void testJavaAPI5() throws IOException {
		edu.illinois.ncsa.daffodil.japi.Compiler c = Daffodil.compiler();
		c.setValidateDFDLSchemas(false);
		File[] schemaFileNames = new File[1];
		schemaFileNames[0] = getResource("/test/japi/mySchema3.dfdl.xsd");
		c.setDistinguishedRootNode("e4", null); // e4 is a 4-byte long string
												// element
		ProcessorFactory pf = c.compile(schemaFileNames);
		DataProcessor dp = pf.onPath("/");
		java.io.File file = getResource("/test/japi/myData3.dat"); // contains 5
																	// bytes
		java.io.FileInputStream fis = new java.io.FileInputStream(file);
		java.nio.channels.ReadableByteChannel rbc = java.nio.channels.Channels
				.newChannel(fis);
		ParseResult res = dp.parse(rbc, 4 << 3);
		boolean err = res.isError();
		org.jdom2.output.XMLOutputter xo = new org.jdom2.output.XMLOutputter();
		xo.setFormat(Format.getPrettyFormat());
		java.util.List<Diagnostic> diags = res.getDiagnostics();
		for (Diagnostic d : diags) {
			System.err.println(d.getMessage());
		}
		if (!err) {
			org.jdom2.Document doc = res.result();
			xo.output(doc, System.out);
		}
		assertFalse(err);
		assertTrue("Assertion failed: End of data not reached.", res.location()
				.isAtEnd());
	}

	/***
	 * Verify that the compiler throws a FileNotFound exception when fed a list
	 * of schema files that do not exist.
	 * 
	 * @throws IOException
	 */
	@Test
	@Ignore
	public void testJavaAPI6() throws IOException {
		Daffodil.setLogWriter(lw);
		Daffodil.setLoggingLevel(LogLevel.Debug);

		edu.illinois.ncsa.daffodil.japi.Compiler c = Daffodil.compiler();
		c.setValidateDFDLSchemas(false);
		java.io.File[] schemaFiles = new java.io.File[4];// String[]
															// schemaFileNames =
															// new String[2];
		schemaFiles[0] = getResource("/test/japi/mySchema1.dfdl.xsd");
		schemaFiles[1] = new java.io.File("/test/japi/notHere1.dfdl.xsd");
		schemaFiles[2] = getResource("/test/japi/mySchema2.dfdl.xsd");
		schemaFiles[3] = new java.io.File("/test/japi/notHere2.dfdl.xsd");
		try {
			c.compile(schemaFiles);
			fail("Expected a FileNotFoundException and didn't get one");
		} catch (FileNotFoundException fnf) {
			String msg = fnf.getMessage();
			assertTrue(msg.contains("notHere1"));
			assertTrue(msg.contains("notHere2"));
		}

		// reset the global logging state
		Daffodil.setLogWriter(new ConsoleLogWriter());
		Daffodil.setLoggingLevel(LogLevel.Info);
	}

	/**
	 * Tests a user submitted case where the XML appears to be serializing odd
	 * xml entities into the output.
	 * 
	 * @throws IOException
	 */
	@Test
	@Ignore
	public void testJavaAPI7() throws IOException {
		// TODO: This is due to the fact that we are doing several conversions
		// back and forth between Scala.xml.Node and JDOM. And the conversions
		// both use XMLOutputter to format the result (which escapes the
		// entities).

		Daffodil.setLogWriter(lw);
		Daffodil.setLoggingLevel(LogLevel.Debug);

		edu.illinois.ncsa.daffodil.japi.Compiler c = Daffodil.compiler();
		c.setValidateDFDLSchemas(false);
		java.io.File[] schemaFiles = new java.io.File[1];
		schemaFiles[0] = getResource("/test/japi/TopLevel.xsd");
		c.setDistinguishedRootNode("TopLevel", null);
		ProcessorFactory pf = c.compile(schemaFiles);
		DataProcessor dp = pf.onPath("/");
		java.io.File file = getResource("/test/japi/01very_simple.txt");
		java.io.FileInputStream fis = new java.io.FileInputStream(file);
		java.nio.channels.ReadableByteChannel rbc = java.nio.channels.Channels
				.newChannel(fis);
		ParseResult res = dp.parse(rbc);
		boolean err = res.isError();
		if (!err) {
			org.jdom2.Document doc = res.result();
			org.jdom2.output.XMLOutputter xo = new org.jdom2.output.XMLOutputter();
			// xo.setFormat(Format.getPrettyFormat());
			xo.setFormat(Format.getRawFormat().setTextMode(
					Format.TextMode.PRESERVE));
			xo.output(doc, System.out);
		}
		java.util.List<Diagnostic> diags = res.getDiagnostics();
		for (Diagnostic d : diags) {
			System.err.println(d.getMessage());
		}
		assertTrue(res.location().isAtEnd());

		// reset the global logging state
		Daffodil.setLogWriter(new ConsoleLogWriter());
		Daffodil.setLoggingLevel(LogLevel.Info);
	}

	/**
	 * This test is nearly identical to testJavaAPI7. The only difference is
	 * that this test uses double newline as a terminator for the first element
	 * in the sequence rather than double newline as a separator for the
	 * sequence
	 * 
	 * @throws IOException
	 */
	@Test
	@Ignore
	public void testJavaAPI8() throws IOException {

		Daffodil.setLogWriter(lw);
		Daffodil.setLoggingLevel(LogLevel.Debug);

		edu.illinois.ncsa.daffodil.japi.Compiler c = Daffodil.compiler();
		c.setValidateDFDLSchemas(false);
		java.io.File[] schemaFiles = new java.io.File[1];
		schemaFiles[0] = getResource("/test/japi/TopLevel.xsd");
		c.setDistinguishedRootNode("TopLevel2", null);
		ProcessorFactory pf = c.compile(schemaFiles);
		DataProcessor dp = pf.onPath("/");
		java.io.File file = getResource("/test/japi/01very_simple.txt");
		java.io.FileInputStream fis = new java.io.FileInputStream(file);
		java.nio.channels.ReadableByteChannel rbc = java.nio.channels.Channels
				.newChannel(fis);
		ParseResult res = dp.parse(rbc);
		boolean err = res.isError();
		if (!err) {
			org.jdom2.Document doc = res.result();
			org.jdom2.output.XMLOutputter xo = new org.jdom2.output.XMLOutputter();
			// xo.setFormat(Format.getPrettyFormat());
			xo.setFormat(Format.getRawFormat());
			xo.output(doc, System.out);
		}
		java.util.List<Diagnostic> diags = res.getDiagnostics();
		for (Diagnostic d : diags) {
			System.err.println(d.getMessage());
		}
		assertTrue(res.location().isAtEnd());

		// reset the global logging state
		Daffodil.setLogWriter(new ConsoleLogWriter());
		Daffodil.setLoggingLevel(LogLevel.Info);
	}

	/**
	 * Verify that calling result() on the ParseResult mutiple times does not
	 * error.
	 */
	@Test
	@Ignore
	public void testJavaAPI9() throws IOException {

		Daffodil.setLogWriter(lw);
		Daffodil.setLoggingLevel(LogLevel.Debug);

		edu.illinois.ncsa.daffodil.japi.Compiler c = Daffodil.compiler();
		c.setValidateDFDLSchemas(false);
		java.io.File[] schemaFiles = new java.io.File[1];
		schemaFiles[0] = getResource("/test/japi/TopLevel.xsd");
		c.setDistinguishedRootNode("TopLevel2", null);
		ProcessorFactory pf = c.compile(schemaFiles);
		DataProcessor dp = pf.onPath("/");
		java.io.File file = getResource("/test/japi/01very_simple.txt");
		java.io.FileInputStream fis = new java.io.FileInputStream(file);
		java.nio.channels.ReadableByteChannel rbc = java.nio.channels.Channels
				.newChannel(fis);
		ParseResult res = dp.parse(rbc);
		boolean err = res.isError();
		if (!err) {
			org.jdom2.Document doc = res.result();
			// org.jdom2.Document doc2 = res.result();
			// org.jdom2.Document doc3 = res.result();
			org.jdom2.output.XMLOutputter xo = new org.jdom2.output.XMLOutputter();
			xo.setFormat(Format.getRawFormat());
			xo.output(doc, System.out);
		}
		java.util.List<Diagnostic> diags = res.getDiagnostics();
		for (Diagnostic d : diags) {
			System.err.println(d.getMessage());
		}
		assertTrue(res.location().isAtEnd());

		// reset the global logging state
		Daffodil.setLogWriter(new ConsoleLogWriter());
		Daffodil.setLoggingLevel(LogLevel.Info);
	}

	/**
	 * Verify that hidden elements do not appear in the resulting infoset
	 */
	@Test
	@Ignore
	public void testJavaAPI10() throws IOException {

		edu.illinois.ncsa.daffodil.japi.Compiler c = Daffodil.compiler();
		c.setValidateDFDLSchemas(false);
		java.io.File[] schemaFiles = new java.io.File[1];
		schemaFiles[0] = getResource("/test/japi/mySchema4.dfdl.xsd");
		ProcessorFactory pf = c.compile(schemaFiles);
		DataProcessor dp = pf.onPath("/");
		java.io.File file = getResource("/test/japi/myData4.dat");
		java.io.FileInputStream fis = new java.io.FileInputStream(file);
		java.nio.channels.ReadableByteChannel rbc = java.nio.channels.Channels
				.newChannel(fis);
		ParseResult res = dp.parse(rbc);
		boolean err = res.isError();
		if (!err) {
			org.jdom2.Document doc = res.result();
			org.jdom2.output.XMLOutputter xo = new org.jdom2.output.XMLOutputter();
			xo.setFormat(Format.getPrettyFormat());
			xo.output(doc, System.out);
			org.jdom2.Element rootNode = doc.getRootElement();
			org.jdom2.Element hidden = rootNode.getChild("hiddenElement",
					rootNode.getNamespace());
			assertTrue(null == hidden);
		}
		java.util.List<Diagnostic> diags = res.getDiagnostics();
		for (Diagnostic d : diags) {
			System.err.println(d.getMessage());
		}
		assertTrue(res.location().isAtEnd());
	}

	/**
	 * Verify that nested elements do not appear as duplicates
	 */
	@Test
	@Ignore
	public void testJavaAPI11() throws IOException {

		edu.illinois.ncsa.daffodil.japi.Compiler c = Daffodil.compiler();
		c.setValidateDFDLSchemas(false);
		java.io.File[] schemaFiles = new java.io.File[1];
		schemaFiles[0] = getResource("/test/japi/mySchema5.dfdl.xsd");
		ProcessorFactory pf = c.compile(schemaFiles);
		DataProcessor dp = pf.onPath("/");
		java.io.File file = getResource("/test/japi/myData5.dat");
		java.io.FileInputStream fis = new java.io.FileInputStream(file);
		java.nio.channels.ReadableByteChannel rbc = java.nio.channels.Channels
				.newChannel(fis);
		ParseResult res = dp.parse(rbc);
		boolean err = res.isError();
		if (!err) {
			org.jdom2.Document doc = res.result();
			org.jdom2.output.XMLOutputter xo = new org.jdom2.output.XMLOutputter();
			xo.setFormat(Format.getPrettyFormat());
			xo.output(doc, System.out);
			org.jdom2.Element rootNode = doc.getRootElement();
			org.jdom2.Element elementGroup = rootNode.getChild("elementGroup",
					rootNode.getNamespace());
			assertTrue(null != elementGroup);
			org.jdom2.Element groupE2 = elementGroup.getChild("e2",
					rootNode.getNamespace());
			assertTrue(null != groupE2);
			org.jdom2.Element groupE3 = elementGroup.getChild("e3",
					rootNode.getNamespace());
			assertTrue(null != groupE3);
			org.jdom2.Element rootE2 = rootNode.getChild("e2",
					rootNode.getNamespace());
			assertTrue(null == rootE2);
			org.jdom2.Element rootE3 = rootNode.getChild("e3",
					rootNode.getNamespace());
			assertTrue(null == rootE3);
		}
		java.util.List<Diagnostic> diags = res.getDiagnostics();
		for (Diagnostic d : diags) {
			System.err.println(d.getMessage());
		}
		assertTrue(res.location().isAtEnd());
	}

	@Test
	@Ignore
	public void testJavaAPI12() throws IOException {
		DebuggerRunnerForJAPITest debugger = new DebuggerRunnerForJAPITest();

		Daffodil.setLogWriter(lw);
		Daffodil.setLoggingLevel(LogLevel.Debug);
		Daffodil.setDebugging(true);
		// Daffodil.setDebugger(debugger);

		edu.illinois.ncsa.daffodil.japi.Compiler c = Daffodil.compiler();
		c.setValidateDFDLSchemas(false);

		java.io.File[] schemaFiles = new java.io.File[2];
		schemaFiles[0] = getResource("/test/japi/mySchema1.dfdl.xsd");
		schemaFiles[1] = getResource("/test/japi/mySchema2.dfdl.xsd");
		ProcessorFactory pf = c.compile(schemaFiles);
		DataProcessor dp = pf.onPath("/");
		java.io.File file = getResource("/test/japi/myData.dat");
		java.io.FileInputStream fis = new java.io.FileInputStream(file);
		java.nio.channels.ReadableByteChannel rbc = java.nio.channels.Channels
				.newChannel(fis);
		ParseResult res = dp.parse(rbc, 2 << 3);
		boolean err = res.isError();
		if (!err) {
			org.jdom2.Document doc = res.result();
			org.jdom2.output.XMLOutputter xo = new org.jdom2.output.XMLOutputter();
			xo.setFormat(Format.getPrettyFormat());
			xo.output(doc, System.out);
		}
		java.util.List<Diagnostic> diags = res.getDiagnostics();
		for (Diagnostic d : diags) {
			System.err.println(d.getMessage());
		}
		assertTrue(res.location().isAtEnd());

		assertTrue(debugger.lines.size() > 0);
		assertTrue(debugger.lines
				.contains("----------------------------------------------------------------- 1\n"));

		// reset the global logging and debugger state
		Daffodil.setLogWriter(new ConsoleLogWriter());
		Daffodil.setLoggingLevel(LogLevel.Info);
		Daffodil.setDebugging(false);
		Daffodil.setDebugger(null);
	}

	@Test
	@Ignore
	public void testJavaAPI13() throws IOException {
		// Demonstrates here that we can set external variables
		// after compilation but before parsing via Compiler.
		DebuggerRunnerForJAPITest debugger = new DebuggerRunnerForJAPITest();

		Daffodil.setLogWriter(lw);
		Daffodil.setLoggingLevel(LogLevel.Debug);
		Daffodil.setDebugging(true);
		// Daffodil.setDebugger(debugger);

		edu.illinois.ncsa.daffodil.japi.Compiler c = Daffodil.compiler();
		c.setValidateDFDLSchemas(false);
		java.io.File extVarsFile = getResource("/test/japi/external_vars_1.xml");
		java.io.File[] schemaFiles = new java.io.File[2];
		schemaFiles[0] = getResource("/test/japi/mySchemaWithVars.dfdl.xsd");
		schemaFiles[1] = getResource("/test/japi/mySchema2.dfdl.xsd");
		c.setExternalDFDLVariables(extVarsFile);
		ProcessorFactory pf = c.compile(schemaFiles);

		DataProcessor dp = pf.onPath("/");

		java.io.File file = getResource("/test/japi/myData.dat");
		java.io.FileInputStream fis = new java.io.FileInputStream(file);
		java.nio.channels.ReadableByteChannel rbc = java.nio.channels.Channels
				.newChannel(fis);
		ParseResult res = dp.parse(rbc, 2 << 3);
		boolean err = res.isError();
		if (!err) {
			org.jdom2.Document doc = res.result();
			org.jdom2.output.XMLOutputter xo = new org.jdom2.output.XMLOutputter();
			xo.setFormat(Format.getPrettyFormat());
			xo.output(doc, System.out);
			String docString = xo.outputString(doc);
			boolean containsVar1 = docString.contains("var1Value");
			boolean containsVar1Value = docString.contains("externallySet");
			assertTrue(containsVar1);
			assertTrue(containsVar1Value);
		}

		// reset the global logging and debugger state
		Daffodil.setLogWriter(new ConsoleLogWriter());
		Daffodil.setLoggingLevel(LogLevel.Info);
		Daffodil.setDebugging(false);
		Daffodil.setDebugger(null);
	}

	@Test
	@Ignore
	public void testJavaAPI14() throws IOException {
		// Demonstrates here that we can set external variables
		// after compilation but before parsing via DataProcessor.
		DebuggerRunnerForJAPITest debugger = new DebuggerRunnerForJAPITest();

		Daffodil.setLogWriter(lw);
		Daffodil.setLoggingLevel(LogLevel.Debug);
		Daffodil.setDebugging(true);
		// Daffodil.setDebugger(debugger);

		edu.illinois.ncsa.daffodil.japi.Compiler c = Daffodil.compiler();
		c.setValidateDFDLSchemas(false);
		java.io.File extVarFile = getResource("/test/japi/external_vars_1.xml");
		java.io.File[] schemaFiles = new java.io.File[2];
		schemaFiles[0] = getResource("/test/japi/mySchemaWithVars.dfdl.xsd");
		schemaFiles[1] = getResource("/test/japi/mySchema2.dfdl.xsd");
		ProcessorFactory pf = c.compile(schemaFiles);
		DataProcessor dp = pf.onPath("/");
		dp.setExternalVariables(extVarFile);

		java.io.File file = getResource("/test/japi/myData.dat");
		java.io.FileInputStream fis = new java.io.FileInputStream(file);
		java.nio.channels.ReadableByteChannel rbc = java.nio.channels.Channels
				.newChannel(fis);
		ParseResult res = dp.parse(rbc, 2 << 3);
		boolean err = res.isError();
		if (!err) {
			org.jdom2.Document doc = res.result();
			org.jdom2.output.XMLOutputter xo = new org.jdom2.output.XMLOutputter();
			xo.setFormat(Format.getPrettyFormat());
			xo.output(doc, System.out);
			String docString = xo.outputString(doc);
			boolean containsVar1 = docString.contains("var1Value");
			boolean containsVar1Value = docString.contains("externallySet");
			assertTrue(containsVar1);
			assertTrue(containsVar1Value);
		}
		java.util.List<Diagnostic> diags = res.getDiagnostics();
		for (Diagnostic d : diags) {
			System.err.println(d.getMessage());
		}
		assertTrue(res.location().isAtEnd());

		assertTrue(debugger.lines.size() > 0);
		assertTrue(debugger.lines
				.contains("----------------------------------------------------------------- 1\n"));

		// reset the global logging and debugger state
		Daffodil.setLogWriter(new ConsoleLogWriter());
		Daffodil.setLoggingLevel(LogLevel.Info);
		Daffodil.setDebugging(false);
		Daffodil.setDebugger(null);
	}

}
