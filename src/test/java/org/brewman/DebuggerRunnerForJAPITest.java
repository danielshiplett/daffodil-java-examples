package org.brewman;

import java.util.ArrayList;

public class DebuggerRunnerForJAPITest {
	ArrayList<String> lines;

	public void init() {
		lines = new ArrayList<String>();
	}

	public void lineOutput(String line) {
		lines.add(line + "\n");
	}
}
