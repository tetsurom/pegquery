package pegquery;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.peg4d.ParsingObject;

public class Executor extends QueryVisitor<Object, Object> {
	@Override
	public Object visitSelect(ParsingObject queryTree, Optional<Object> data) {
		ParsingObject fromTree = this.getChildAt(queryTree, "from");
		ParsingObject whereTree = this.getChildAt(queryTree, "where");

		// select 
		if(fromTree == null && whereTree == null) {
			String[] path = (String[]) this.getPath(queryTree.get(0));
			final List<ParsingObject> foundTreeList = new ArrayList<>();
			ParsingObject targetObject = (ParsingObject) data.get();
			this.findSubTree(foundTreeList, path, targetObject);
			final List<String> resultList = new ArrayList<>(foundTreeList.size());
			foundTreeList.stream().forEach(s -> resultList.add(s.getText()));
			return resultList;
		}

		// select [from, (where)]
		if(fromTree != null) {
			String[] path = (String[]) this.dispatch(fromTree, data);
			final List<ParsingObject> foundTreeList = new ArrayList<>();
			ParsingObject targetObject = (ParsingObject) data.get();
			this.findSubTree(foundTreeList, path, targetObject);
			List<String> resultList = new ArrayList<>();

			if(whereTree == null) {	// select [from]
				for(ParsingObject tree : foundTreeList) {
					Object result = this.dispatch(queryTree.get(0), Optional.of(tree));
					if(result instanceof String) {
						resultList.add((String) result);
					}
				}
			}
			else {	// select [from, where]
				for(ParsingObject tree : foundTreeList) {
					if((boolean) this.dispatch(queryTree.get(2), Optional.of(tree))) {
						Object result = this.dispatch(queryTree.get(0), Optional.of(tree));
						if(result instanceof String) {
							resultList.add((String) result);
						}
					}
				}
			}
			return resultList;
		}
		return null;
	}

	private String[] getPath(ParsingObject queryTree) {
		final int size = queryTree.size();
		String[] path = new String[size];
		for(int i = 0; i < size; i++) {
			String tag = queryTree.get(i).getText();
			if(!tag.startsWith("#")) {
				tag = "#" + tag;
			}
			path[i] = tag;
		}
		return path;
	}

	private void findSubTree(final List<ParsingObject> resultList, 
			final String[] path, ParsingObject target) {
		this.findSubTreeAt(resultList, path, 0, target);
	}

	private void findSubTreeAt(final List<ParsingObject> resultList, 
			final String[] path, final int curPathIndex, ParsingObject target) {
		final int size = target.size();
		for(int i = 0; i < size; i++) {
			ParsingObject subObject = target.get(i);
			if(subObject.is(path[curPathIndex])) {
				if(path.length == curPathIndex + 1) {
					resultList.add(subObject);
					this.findSubTree(resultList, path, subObject);
				}
				else {
					this.findSubTreeAt(resultList, path, curPathIndex + 1, subObject);
				}
			}
			else {
				this.findSubTree(resultList, path, subObject);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public List<String> execQuery(ParsingObject queryTree, ParsingObject targetObject) 
			throws QueryExecutionException {
		List<String> resultList = (List<String>) this.dispatch(queryTree, Optional.of(targetObject));
		if(resultList == null) {
			throw new QueryExecutionException("illegal query:" + System.lineSeparator() + queryTree);
		}
		return resultList;
	}

	// FIXME: support duplicated tag
	@Override
	public Object visitPath(ParsingObject queryTree, Optional<Object> data) {	// get matched value
		final String[] path = this.getPath(queryTree);
		ParsingObject target = (ParsingObject) data.get();
		for(String tag : path) {
			final int childSize = target.size();
			boolean found = false;
			for(int i = 0; i < childSize; i++) {
				if(target.get(i).is(tag)) {
					found = true;
					target = target.get(i);
					break;
				}
			}
			if(!found) {
				return null;
			}
		}
		return target.getText();
	}

	@Override
	public Object visitError(ParsingObject queryTree, Optional<Object> data) {
		throw new QueryExecutionException("invalid query:" + System.lineSeparator() + queryTree);
	}

	public static class QueryExecutionException extends RuntimeException {
		private static final long serialVersionUID = -5774942100147563362L;

		public QueryExecutionException(String message) {
			super(message);
		}
	}

	@Override
	public Object visitFrom(ParsingObject queryTree, Optional<Object> data) {
		return this.getPath(queryTree.get(0));
	}

	@Override
	public Object visitWhere(ParsingObject queryTree, Optional<Object> data) {
		return this.dispatch(queryTree.get(0), data);
	}

	@Override
	public Object visitAnd(ParsingObject queryTree, Optional<Object> data) {
		Object left = this.dispatch(queryTree.get(0), data);
		if((left instanceof Boolean) && (Boolean) left) {
			Object right = this.dispatch(queryTree.get(1), data);
			return (right instanceof Boolean) && (Boolean) right;
		}
		return false;
	}

	@Override
	public Object visitOr(ParsingObject queryTree, Optional<Object> data) {
		Object left = this.dispatch(queryTree.get(0), data);
		if((left instanceof Boolean) && (Boolean) left) {
			return true;
		}
		Object right = this.dispatch(queryTree.get(1), data);
		return (right instanceof Boolean) && (Boolean) right;
	}

	@Override
	public Object visitEQ(ParsingObject queryTree, Optional<Object> data) {
		Number left = this.asNumber(this.dispatch(queryTree.get(0), data));
		Number right = this.asNumber(this.dispatch(queryTree.get(1), data));
		if((left instanceof Long) && (right instanceof Long)) {
			return (long) left == (long) right;
		}
		else if((left instanceof Long) && (right instanceof Double)) {
			return (long) left == (double) right;
		}
		else if((left instanceof Double) && (right instanceof Double)) {
			return (double) left == (double) right;
		}
		else if((left instanceof Double) && (right instanceof Long)) {
			return (double) left == (long) right;
		}
		return false;
	}

	@Override
	public Object visitNEQ(ParsingObject queryTree, Optional<Object> data) {
		Number left = this.asNumber(this.dispatch(queryTree.get(0), data));
		Number right = this.asNumber(this.dispatch(queryTree.get(1), data));
		if((left instanceof Long) && (right instanceof Long)) {
			return (long) left != (long) right;
		}
		else if((left instanceof Long) && (right instanceof Double)) {
			return (long) left != (double) right;
		}
		else if((left instanceof Double) && (right instanceof Double)) {
			return (double) left != (double) right;
		}
		else if((left instanceof Double) && (right instanceof Long)) {
			return (double) left != (long) right;
		}
		return false;
	}

	@Override
	public Object visitLE(ParsingObject queryTree, Optional<Object> data) {
		Number left = this.asNumber(this.dispatch(queryTree.get(0), data));
		Number right = this.asNumber(this.dispatch(queryTree.get(1), data));
		if((left instanceof Long) && (right instanceof Long)) {
			return (long) left <= (long) right;
		}
		else if((left instanceof Long) && (right instanceof Double)) {
			return (long) left <= (double) right;
		}
		else if((left instanceof Double) && (right instanceof Double)) {
			return (double) left <= (double) right;
		}
		else if((left instanceof Double) && (right instanceof Long)) {
			return (double) left <= (long) right;
		}
		return false;
	}

	@Override
	public Object visitGE(ParsingObject queryTree, Optional<Object> data) {
		Number left = this.asNumber(this.dispatch(queryTree.get(0), data));
		Number right = this.asNumber(this.dispatch(queryTree.get(1), data));
		if((left instanceof Long) && (right instanceof Long)) {
			return (long) left >= (long) right;
		}
		else if((left instanceof Long) && (right instanceof Double)) {
			return (long) left >= (double) right;
		}
		else if((left instanceof Double) && (right instanceof Double)) {
			return (double) left >= (double) right;
		}
		else if((left instanceof Double) && (right instanceof Long)) {
			return (double) left >= (long) right;
		}
		return false;
	}

	@Override
	public Object visitLT(ParsingObject queryTree, Optional<Object> data) {
		Number left = this.asNumber(this.dispatch(queryTree.get(0), data));
		Number right = this.asNumber(this.dispatch(queryTree.get(1), data));
		if((left instanceof Long) && (right instanceof Long)) {
			return (long) left < (long) right;
		}
		else if((left instanceof Long) && (right instanceof Double)) {
			return (long) left < (double) right;
		}
		else if((left instanceof Double) && (right instanceof Double)) {
			return (double) left < (double) right;
		}
		else if((left instanceof Double) && (right instanceof Long)) {
			return (double) left < (long) right;
		}
		return false;
	}

	@Override
	public Object visitGT(ParsingObject queryTree, Optional<Object> data) {
		Number left = this.asNumber(this.dispatch(queryTree.get(0), data));
		Number right = this.asNumber(this.dispatch(queryTree.get(1), data));
		if((left instanceof Long) && (right instanceof Long)) {
			return (long) left > (long) right;
		}
		else if((left instanceof Long) && (right instanceof Double)) {
			return (long) left > (double) right;
		}
		else if((left instanceof Double) && (right instanceof Double)) {
			return (double) left > (double) right;
		}
		else if((left instanceof Double) && (right instanceof Long)) {
			return (double) left > (long) right;
		}
		return false;
	}

	@Override
	public Object visitNum(ParsingObject queryTree, Optional<Object> data) {
		return this.asNumber(queryTree.getText());
	}

	private Number asNumber(Object value) {
		if(value == null) {
			return null;
		}
		if(value instanceof Number) {
			return (Number) value;
		}
		String str = (String) value;
		try {
			if(str.indexOf(".") == -1) {
				return Long.parseLong(str);	// as long
			}
			return Double.parseDouble(str);	// as double
		}
		catch(NumberFormatException e) {
		}
		return null;
	}
}
