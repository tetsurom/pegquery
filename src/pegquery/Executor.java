package pegquery;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.peg4d.ParsingObject;

public class Executor extends QueryVisitor<Object, Object> {
	@Override
	public Object visitSelect(ParsingObject queryTree, Optional<Object> data) {
		int size = queryTree.size();
		if(size == 1) {
			String[] path = (String[]) this.getPath(queryTree.get(0));
			final List<ParsingObject> foundTreeList = new ArrayList<>();
			ParsingObject targetObject = (ParsingObject) data.get();
			this.findElement(foundTreeList, path, targetObject);
			final List<String> resultList = new ArrayList<>(foundTreeList.size());
			foundTreeList.stream().forEach(s -> resultList.add(s.getText()));
			return resultList;
		}
		else if(size == 3) {
			String[] path = (String[]) this.dispatch(queryTree.get(1), data);
			final List<ParsingObject> foundTreeList = new ArrayList<>();
			ParsingObject targetObject = (ParsingObject) data.get();
			this.findElement(foundTreeList, path, targetObject);
			List<String> resultList = new ArrayList<>();
			for(ParsingObject tree : foundTreeList) {
				if((boolean) this.dispatch(queryTree.get(2), Optional.of(tree))) {
					Object result = this.dispatch(queryTree.get(0), Optional.of(tree));
					if(result instanceof String) {
						resultList.add((String) result);
					}
				}
			}
			return resultList;
		}
		throw new QueryExecutionException("illegal query: " + queryTree);
	}

	private String[] getPath(ParsingObject queryTree) {
		final int size = queryTree.size();
		String[] path = new String[size];
		for(int i = 0; i < size; i++) {
			path[i] = queryTree.get(i).getText();
		}
		return path;
	}

	private void findElement(final List<ParsingObject> resultList, 
			final String[] path, ParsingObject target) {
		final int size = target.size();
		for(int i = 0; i < size; i++) {
			ParsingObject subObject = target.get(i);
			if(subObject.is(path[0])) {
				if(path.length == 1) {
					resultList.add(subObject);
				}
				else {
					this.findElementAt(resultList, path, 1, subObject);
				}
			}
			else {
				this.findElement(resultList, path, subObject);
			}
		}
	}

	private void findElementAt(final List<ParsingObject> resultList, 
			final String[] path, int curPathIndex, ParsingObject target) {
		final int size = target.size();
		for(int i = 0; i < size; i++) {
			ParsingObject subObject = target.get(i);
			if(subObject.is(path[curPathIndex])) {
				if(path.length == curPathIndex + 1) {
					resultList.add(subObject);
					return;
				}
				this.findElementAt(resultList, path, curPathIndex + 1, subObject);
			}
			else {
				this.findElement(resultList, path, subObject);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public List<String> execQuery(ParsingObject queryTree, ParsingObject targetObject) 
			throws QueryExecutionException {
		return (List<String>) this.dispatch(queryTree, Optional.of(targetObject));
	}

	@Override
	public Object visitPath(ParsingObject queryTree, Optional<Object> data) {	// get matched value
		final int size = queryTree.size();
		String[] path = new String[size];
		for(int i = 0; i < size; i++) {
			path[i] = queryTree.get(i).getText();
		}
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
		String text = queryTree.getText();
		try {
			if(text.indexOf(".") == -1) {	// as long
				return Long.parseLong(text);
			}
			return Double.parseDouble(text);	// as double
		}
		catch(NumberFormatException e) {
		}
		return null;
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
				return Long.parseLong(str);
			}
			return Double.parseDouble(str);
		}
		catch(NumberFormatException e) {
		}
		return null;
	}
}
