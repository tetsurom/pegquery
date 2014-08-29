package pegquery;

import java.util.List;
import java.util.Optional;

import org.peg4d.Grammar;
import org.peg4d.GrammarComposer;
import org.peg4d.Helper;
import org.peg4d.ParserContext;
import org.peg4d.ParsingObject;
import org.peg4d.ParsingSource;

import pegquery.Executor.QueryExecutionException;

public class Main {	//TODO: pipe line mode
	private final static String defaultStartPoint = "TopLevel";
	private static Optional<String> pegDef4Data = Optional.empty();
	private static Optional<String> targetData = Optional.empty();
	private final static String pegDef4Query = "query.peg";
	private static Optional<String> queryScript = Optional.empty();

	private static boolean verboseQuery = false;
	private static boolean verboseData = false;

	private static boolean time = false;

	public static void main(String[] args) {
		/**
		 * parse argument
		 */
		final ArgumentsParser argsParser = new ArgumentsParser();
		argsParser.addDefaultAction(s -> argsParser.printHelpBeforeExit(System.err, 1))
		.addHelp("h", "help", false, "show this help message", 
				s -> argsParser.printHelpBeforeExit(System.out, 0))
		.addOption("g", "grammar", true, "peg definition of target data format", true,
				s -> pegDef4Data = Optional.of(s.get()))
		.addOption("t", "target", true, "target data file", 
				s -> targetData = Optional.of(s.get()))
		.addOption("q", "query", true, "query script", 
				s -> queryScript = Optional.of(s.get()))
		.addOption("vd", "verbose:data", false, "display parsed data", 
				s -> verboseData = true)
		.addOption("vq", "verbose:query", false, "display parsed query", 
				s -> verboseQuery = true)
		.addOption(null, "time", false, "display query execution time", 
				s -> time = true);

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

		if(verboseData) {
			System.err.println("parsed data:");
			System.err.println(parsedObject);
			System.err.println("");
		}
		if(parsedObject.isFailure() || parsedObject.is("#error")) {
			System.err.println(parsedObject);
			System.exit(1);
		}

		/**
		 * exec query
		 */
		Grammar queryPeg = Grammar.load(new GrammarComposer(), pegDef4Query);
		if(queryScript.isPresent()) {
			boolean result = parseAndRunQuery(new Executor(), queryPeg, 
					org.peg4d.Main.loadSource(peg, queryScript.get()), parsedObject);
			System.exit(result ? 0 : 1);
		}
		else {
			Shell shell = new Shell();
			Pair<String, Integer> resultPair = null;
			Executor executor = new Executor();
			while((resultPair = shell.readLine()) != null) {	//TODO:
				if(resultPair.getLeft().equals("")) {
					continue;
				}
				ParsingSource source = Helper.loadLine(queryPeg, "(stdin)", resultPair.getRight(), resultPair.getLeft());
				parseAndRunQuery(executor, queryPeg, source, parsedObject);
			}
			System.out.println("");
		}
	}

	private static boolean parseAndRunQuery(Executor executor, Grammar queryPeg, ParsingSource source, ParsingObject target) {
		ParserContext queryParserContext = queryPeg.newParserContext(source);
		ParsingObject queryTree = queryParserContext.parseNode(defaultStartPoint);
		if(verboseQuery) {
			System.err.println("parsed query:");
			System.err.println(queryTree);
		}
		try {
			long start = 0;
			long stop = 0;
			if(time) {
				start = System.nanoTime();
			}

			List<ParsingObject> resultList = executor.execQuery(queryTree, target);

			if(time) {
				stop = System.nanoTime();
				System.err.println("query execution time: " + (stop - start) + "ns");
			}
			resultList.stream().forEach(t -> System.out.println(t.getText()));
			return true;
		}
		catch(QueryExecutionException e) {
			System.err.println(e.getMessage());
		}
		return false;
	}
}
