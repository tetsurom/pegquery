package pegquery;

import java.util.Optional;

import org.peg4d.Grammar;
import org.peg4d.GrammarComposer;
import org.peg4d.Helper;
import org.peg4d.ParserContext;
import org.peg4d.ParsingObject;
import org.peg4d.ParsingSource;

public class Main {	//TODO: pipe line mode
	private final static String defaultStartPoint = "TopLevel";
	private static Optional<String> pegDef4Data = Optional.empty();
	private static Optional<String> targetData = Optional.empty();
	private final static String pegDef4Query = "query.peg";
	private static Optional<String> queryScript = Optional.empty();

	public static void main(String[] args) {
		/**
		 * parse argument
		 */
		final ArgumentsParser argsParser = new ArgumentsParser();
		argsParser.addDefaultAction(s -> argsParser.printHelpBeforeExit(System.err, 1))
		.addOption("h", "help", false, "show this help message", 
				s -> argsParser.printHelpBeforeExit(System.out, 0))
		.addOption("g", "grammar", true, "peg definition of target data format", 
				s -> pegDef4Data = Optional.of(s.get()))
		.addOption("t", "target", true, "target data file", 
				s -> targetData = Optional.of(s.get()))
		.addOption("q", "query", true, "query script", 
				s -> queryScript = Optional.of(s.get()));

		try {
			argsParser.parseAndInvokeAction(args);
		}
		catch(IllegalArgumentException e) {
			System.err.println(e.getMessage());
			argsParser.printHelpBeforeExit(System.err, 1);
		}

		/**
		 * parse data file
		 */
		Grammar peg = Grammar.load(new GrammarComposer(), pegDef4Data.get());
		ParserContext p = peg.newParserContext(org.peg4d.Main.loadSource(peg, targetData.get()));
		ParsingObject parsedObject = p.parseNode(defaultStartPoint);	//FIXME

		/**
		 * exec query
		 */
		Grammar queryPeg = Grammar.load(new GrammarComposer(), pegDef4Query);
		if(queryScript.isPresent()) {
			//TODO: load query script
		}
		else {
			Shell shell = new Shell();
			Pair<String, Integer> resultPair = null;
			while((resultPair = shell.readLine()) != null) {	//TODO:
				if(resultPair.getLeft().equals("")) {
					continue;
				}
				ParsingSource source = Helper.loadLine(queryPeg, "(stdin)", resultPair.getRight(), resultPair.getLeft());
				ParserContext queryParserContext = queryPeg.newParserContext(source);
				ParsingObject tree = queryParserContext.parseNode(defaultStartPoint);
				System.out.println("Parsed: " + tree);
			}
			System.out.println("");
		}
	}
}
