package pegquery;

import java.util.ArrayList;
import java.util.List;

import org.peg4d.Helper;
import org.peg4d.ParsingObject;

import pegquery.Path.Segment;

public class Executor extends QueryVisitor<Object, ParsingObject> {
	/**
	 * 
	 * @param queryTree
	 * not null
	 * @param targetObject
	 * not null
	 * @return
	 * @throws QueryExecutionException
	 */
	public RList execQuery(ParsingObject queryTree, ParsingObject targetObject) 
			throws QueryExecutionException {
		assert queryTree != null;
		assert targetObject != null;
		ParsingObject dummyRoot = Helper.dummyRoot(targetObject);
		try {
			RList resultList = this.dispatchAndCast(queryTree, dummyRoot, RList.class);
			if(resultList == null) {
				throw new QueryExecutionException("illegal query:" + System.lineSeparator() + queryTree);
			}
			return resultList;
		}
		catch(IllegalArgumentException e) {
			QueryExecutionException.propagate(e);
		}
		return null;
	}

	@Override
	public RList visitSelect(ParsingObject queryTree, ParsingObject data) {
		ParsingObject fromTree = this.getChildAt(queryTree, "from");
		ParsingObject whereTree = this.getChildAt(queryTree, "where");

		RList resultList = new RList();
		// select 
		if(fromTree == null && whereTree == null) {
			Object tree = this.dispatch(queryTree.get(0), data);
			resultList.addAndFlat(tree);
			return resultList;
		}

		// select [from, (where)]
		if(fromTree != null) {
			@SuppressWarnings("unchecked")
			List<ParsingObject> foundTreeList = this.dispatchAndCast(fromTree, data, List.class);

			if(whereTree == null) {	// select [from]
				for(ParsingObject tree : foundTreeList) {
					Object treeList = this.dispatch(queryTree.get(0), tree);
					resultList.addAndFlat(treeList);
				}
			}
			else {	// select [from, where]
				for(ParsingObject tree : foundTreeList) {
					if((boolean) this.dispatch(whereTree, tree)) {
						Object treeList = this.dispatch(queryTree.get(0), tree);
						resultList.addAndFlat(treeList);
					}
				}
			}
			return resultList;
		}
		return null;
	}

	/**
	 * create path and evaluate. return List<ParsingObject>
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Object visitPath(ParsingObject queryTree, ParsingObject data) {
		Path path = new Path(data.getParent() == null);
		final int size = queryTree.size();
		for(int i = 0; i < size; i++) {
			ParsingObject child = queryTree.get(i);
			if(!child.is("#tag")) {
				throw new QueryExecutionException("illegal path:" + System.lineSeparator() + queryTree);
			}
			String segName = this.dispatchAndCast(child.get(0), data, String.class);
			Segment segment = null;
			switch(segName) {
			case "/root":
				segment = new Path.RootSegment();
				break;
			case "/decendant": {
				child = queryTree.get(++i);
				String nextSegName = this.dispatchAndCast(child.get(0), data, String.class);
				if(nextSegName.equals("*")) {
					segment = new Path.WildCardDescendantSegment();
				}
				else {
					segment = new Path.DescendantSegment(nextSegName);
				}
				break;
			}
			case "*":
				segment = new Path.WildCardSegment();
				break;
			default:
				segment = new Path.TagNameSegment(segName);
				break;
			}
			if(child.size() == 2) {
				Object index = this.dispatch(child.get(1), data);
				if(index instanceof Pair) {
					segment = new Path.RangeSegment(segment, (Pair<Integer, Integer>) index);
				}
				else if(index instanceof List) {
					segment = new Path.IndexListSegment(segment, (List<Integer>) index);
				}
			}
			path.addSegment(segment);
		}
		return path.apply(data);
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

		private QueryExecutionException(Throwable cause) {
			super(cause);
		}

		public static void propagate(Throwable cause) throws QueryExecutionException {
			if(cause instanceof QueryExecutionException) {
				throw (QueryExecutionException) cause;
			}
			throw new QueryExecutionException(cause);
		}
	}

	@Override
	public Object visitFrom(ParsingObject queryTree, ParsingObject data) {
		return this.dispatch(queryTree.get(0), data);
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
		return left == null && right == null;
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
		return left == null || right == null;
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
		return left == null;
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
		return right == null;
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
		return left == null && right != null;
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
		return left != null && right == null;
	}

	@Override
	public Object visitNum(ParsingObject queryTree, ParsingObject data) {
		return this.asNumber(queryTree.getText());
	}

	@SuppressWarnings("unchecked")
	private Number asNumber(Object value) {
		if(value == null) {
			return null;
		}
		if(value instanceof Number) {
			return (Number) value;
		}
		String str = null;
		if(value instanceof List) {
			str = ((List<ParsingObject>) value).get(0).getText();
		}
		else if(value instanceof ParsingObject) {
			str = ((ParsingObject) value).getText();
		}
		else if(value instanceof String) {
			str = (String) value;
		}
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
	public Object visitName(ParsingObject queryTree, ParsingObject data) {
		return queryTree.getText();
	}

	@Override
	public Object visitRange(ParsingObject queryTree, ParsingObject data) {
		Number left = this.dispatchAndCast(queryTree.get(0), data, Number.class);
		Number right = this.dispatchAndCast(queryTree.get(1), data, Number.class);
		return new Pair<Integer, Integer>(left.intValue(), right.intValue());
	}

	@Override
	public Object visitIndex(ParsingObject queryTree, ParsingObject data) {
		final int size = queryTree.size();
		List<Integer> indexList = new ArrayList<>(size);
		for(int i = 0; i < size; i++) {
			indexList.add(this.dispatchAndCast(queryTree.get(i), data, Number.class).intValue());
		}
		return indexList;
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
	public Object visitString(ParsingObject queryTree, ParsingObject data) {
		StringBuilder sBuilder = new StringBuilder();
		final int size = queryTree.size();
		for(int i = 0; i < size; i++) {
			sBuilder.append(this.dispatch(queryTree.get(i), data));
		}
		return sBuilder.toString();
	}

	@Override
	public Object visitSegment(ParsingObject queryTree, ParsingObject data) {
		return queryTree.getText();
	}
}
