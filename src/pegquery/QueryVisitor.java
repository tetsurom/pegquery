package pegquery;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.peg4d.ParsingObject;

public abstract class QueryVisitor <T> {
	protected final Map<String, Function<ParsingObject, T>> dispatchMap;

	protected QueryVisitor() {
		this.dispatchMap = new HashMap<>();
		this.dispatchMap.put("#select", this::visitSelect);
		this.dispatchMap.put("#path", this::visitPath);
		this.dispatchMap.put("#error", this::visitError);
	}

	protected T dispatch(ParsingObject tree) {
		Function<ParsingObject, T> func = this.dispatchMap.get(tree.getTag());
		if(func == null) {
			throw new RuntimeException("undefined action: " + tree.getTag());
		}
		return func.apply(tree);
	}

	public abstract T visitSelect(ParsingObject queryTree);
	public abstract T visitPath(ParsingObject queryTree);
	public abstract T visitError(ParsingObject queryTree);
}
