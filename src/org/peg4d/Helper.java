package org.peg4d;

// helper utility. future may be removed
public class Helper {
	public static ParsingSource loadLine(Grammar peg, String fileName, long linenum, String sourceText) {
		return new StringSource(peg, fileName, linenum, sourceText);
	}

	public static ParsingObject dummyRoot(ParsingObject target) {
		ParsingObjectUtils.newStringSource(target);
		ParsingObject dummyRoot = new ParsingObject(new ParsingTag("#$dummy_root$"), target.getSource(), 0);
		dummyRoot.append(target);
		return dummyRoot;
	}
}
