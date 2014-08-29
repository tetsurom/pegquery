package org.peg4d;

// helper utility. future may be removed
public class Helper {
	public static ParsingSource loadLine(Grammar peg, String fileName, long linenum, String sourceText) {
		return new StringSource(peg, fileName, linenum, sourceText);
	}

	public static ParsingObject dummyRoot(ParsingObject target) {
		ParsingObject dummyRoot = ParsingObject.newSource("#$dummy_root$", target.getSource(), 0);
		dummyRoot.append(target);
		return dummyRoot;
	}
}
