package pegquery;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import org.peg4d.ParsingObject;

public abstract class QueryVisitor <R, T> {
	protected final Map<String, BiFunction<ParsingObject, Optional<T>, R>> dispatchMap;

	protected QueryVisitor() {
		this.dispatchMap = new HashMap<>();
		this.dispatchMap.put("#select", this::visitSelect);
		this.dispatchMap.put("#path", this::visitPath);
		this.dispatchMap.put("#error", this::visitError);
		this.dispatchMap.put("#from", this::visitFrom);
		this.dispatchMap.put("#where", this::visitWhere);

		this.dispatchMap.put("#and", this::visitAnd);
		this.dispatchMap.put("#or", this::visitOr);
		this.dispatchMap.put("#eq", this::visitEQ);
		this.dispatchMap.put("#neq", this::visitNEQ);
		this.dispatchMap.put("#le", this::visitLE);
		this.dispatchMap.put("#ge", this::visitGE);
		this.dispatchMap.put("#lt", this::visitLT);
		this.dispatchMap.put("#gt", this::visitGT);

		this.dispatchMap.put("#number", this::visitNum);
	}

	/**
	 * 
	 * @param parent
	 * @param tag
	 * @return
	 * may be null
	 */
	protected ParsingObject getChildAt(final ParsingObject parent, String tag) {
		if(!tag.startsWith("#")) {
			tag = "#" + tag;
		}

		final int size = parent.size();
		for(int i = 0; i < size; i++) {
			ParsingObject chid = parent.get(i);
			if(chid.is(tag)) {
				return chid;
			}
		}
		return null;
	}

	/**
	 * lookup method from dispatchMap and invoke.
	 * @param tree
	 * @param data
	 * not null
	 * @return
	 * return value of looked up method
	 */
	protected R dispatch(ParsingObject tree, Optional<T> data) {
		BiFunction<ParsingObject, Optional<T>, R> func = this.dispatchMap.get(tree.getTag());
		if(func == null) {
			throw new RuntimeException("undefined action: " + tree.getTag());
		}
		return func.apply(tree, data);
	}

	public abstract R visitSelect(ParsingObject queryTree, Optional<T> data);
	public abstract R visitPath  (ParsingObject queryTree, Optional<T> data);
	public abstract R visitError (ParsingObject queryTree, Optional<T> data);
	public abstract R visitFrom  (ParsingObject queryTree, Optional<T> data);
	public abstract R visitWhere (ParsingObject queryTree, Optional<T> data);

	// conditional expression
	public abstract R visitAnd   (ParsingObject queryTree, Optional<T> data);
	public abstract R visitOr    (ParsingObject queryTree, Optional<T> data);
	public abstract R visitEQ    (ParsingObject queryTree, Optional<T> data);
	public abstract R visitNEQ   (ParsingObject queryTree, Optional<T> data);
	public abstract R visitLE    (ParsingObject queryTree, Optional<T> data);
	public abstract R visitGE    (ParsingObject queryTree, Optional<T> data);
	public abstract R visitLT    (ParsingObject queryTree, Optional<T> data);
	public abstract R visitGT    (ParsingObject queryTree, Optional<T> data);

	public abstract R visitNum   (ParsingObject queryTree, Optional<T> data);
}
