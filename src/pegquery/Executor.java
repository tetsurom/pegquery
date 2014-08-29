package pegquery;

import java.util.ArrayList;
import java.util.List;

import org.peg4d.ParsingObject;

public class Executor extends QueryVisitor<Object, ParsingObject> {
	@Override
	public Object visitSelect(ParsingObject queryTree, ParsingObject data) {
		ParsingObject fromTree = this.getChildAt(queryTree, "from");
		ParsingObject whereTree = this.getChildAt(queryTree, "where");

		// select 
		if(fromTree == null && whereTree == null) {
			String[] path = (String[]) this.getPath(queryTree.get(0));
			final List<ParsingObject> foundTreeList = new ArrayList<>();
			this.findSubTree(foundTreeList, path, data);
			final List<String> resultList = new ArrayList<>(foundTreeList.size());
			foundTreeList.stream().forEach(s -> resultList.add(s.getText()));
			return resultList;
		}

		// select [from, (where)]
		if(fromTree != null) {
			@SuppressWarnings("unchecked")
			List<ParsingObject> foundTreeList = (List<ParsingObject>) this.dispatch(fromTree, data);
			List<String> resultList = new ArrayList<>();

			if(whereTree == null) {	// select [from]
				for(ParsingObject tree : foundTreeList) {
					Object result = this.dispatch(queryTree.get(0), tree);
					if(result instanceof String) {
						resultList.add((String) result);
					}
				}
			}
			else {	// select [from, where]
				for(ParsingObject tree : foundTreeList) {
					if((boolean) this.dispatch(whereTree, tree)) {
						Object result = this.dispatch(queryTree.get(0), tree);
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

	/**
	 * 
	 * @param queryTree
	 * not null
	 * @param targetObject
	 * not null
	 * @return
	 * @throws QueryExecutionException
	 */
	@SuppressWarnings("unchecked")
	public List<String> execQuery(ParsingObject queryTree, ParsingObject targetObject) 
			throws QueryExecutionException {
		assert queryTree != null;
		assert targetObject != null;
		List<String> resultList = (List<String>) this.dispatch(queryTree, targetObject);
		if(resultList == null) {
			throw new QueryExecutionException("illegal query:" + System.lineSeparator() + queryTree);
		}
		return resultList;
	}

	@Override
	public Object visitPath(ParsingObject queryTree, ParsingObject data) {	//TODO:
		final String[] path = this.getPath(queryTree);
		ParsingObject target = data;
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
	public Object visitError(ParsingObject queryTree, ParsingObject data) {
		throw new QueryExecutionException("invalid query:" + System.lineSeparator() + queryTree);
	}

	public static class QueryExecutionException extends RuntimeException {
		private static final long serialVersionUID = -5774942100147563362L;

		public QueryExecutionException(String message) {
			super(message);
		}
	}

	@Override
	public Object visitFrom(ParsingObject queryTree, ParsingObject data) {	//TODO:
		String[] path = this.getPath(queryTree.get(0));
		final List<ParsingObject> foundTreeList = new ArrayList<>();
		this.findSubTree(foundTreeList, path, data);
		return foundTreeList;
	}

	@Override
	public Object visitWhere(ParsingObject queryTree, ParsingObject data) {
		return this.dispatch(queryTree.get(0), data);
	}

	@Override
	public Object visitAnd(ParsingObject queryTree, ParsingObject data) {
		Object left = this.dispatch(queryTree.get(0), data);
		if((left instanceof Boolean) && (Boolean) left) {
			Object right = this.dispatch(queryTree.get(1), data);
			return (right instanceof Boolean) && (Boolean) right;
		}
		return false;
	}

	@Override
	public Object visitOr(ParsingObject queryTree, ParsingObject data) {
		Object left = this.dispatch(queryTree.get(0), data);
		if((left instanceof Boolean) && (Boolean) left) {
			return true;
		}
		Object right = this.dispatch(queryTree.get(1), data);
		return (right instanceof Boolean) && (Boolean) right;
	}

	@Override
	public Object visitEQ(ParsingObject queryTree, ParsingObject data) {
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
	public Object visitNEQ(ParsingObject queryTree, ParsingObject data) {
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
	public Object visitLE(ParsingObject queryTree, ParsingObject data) {
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
	public Object visitGE(ParsingObject queryTree, ParsingObject data) {
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
	public Object visitLT(ParsingObject queryTree, ParsingObject data) {
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
	public Object visitGT(ParsingObject queryTree, ParsingObject data) {
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
	public Object visitNum(ParsingObject queryTree, ParsingObject data) {
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

	@Override
	public Object visitTag(ParsingObject queryTree, ParsingObject data) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitName(ParsingObject queryTree, ParsingObject data) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitRange(ParsingObject queryTree, ParsingObject data) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitIndex(ParsingObject queryTree, ParsingObject data) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitCall(ParsingObject queryTree, ParsingObject data) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitArgs(ParsingObject queryTree, ParsingObject data) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitString(ParsingObject queryTree, ParsingObject data) {	//FIXME: escape sequence
		String text = queryTree.getText();
		return text.substring(1, text.length() - 1);
	}
}
