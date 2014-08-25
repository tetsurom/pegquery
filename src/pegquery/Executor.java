package pegquery;

import java.util.ArrayList;
import java.util.List;

import org.peg4d.ParsingObject;

public class Executor extends QueryVisitor<Object> {
	private ParsingObject targetObject;

	@Override
	public Object visitSelect(ParsingObject queryTree) {
		String[] path = (String[]) this.dispatch(queryTree.get(0));
		final List<String> resultList = new ArrayList<>();
		this.findElement(resultList, path, this.targetObject);
		return resultList;
	}

	private void findElement(final List<String> resultList, 
			final String[] path, ParsingObject target) {
		final int size = target.size();
		for(int i = 0; i < size; i++) {
			ParsingObject subObject = target.get(i);
			if(subObject.is(path[0])) {
				if(path.length == 1) {
					resultList.add(subObject.getText());
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

	private void findElementAt(final List<String> resultList, 
			final String[] path, int curPathIndex, ParsingObject target) {
		final int size = target.size();
		for(int i = 0; i < size; i++) {
			ParsingObject subObject = target.get(i);
			if(subObject.is(path[curPathIndex])) {
				if(path.length == curPathIndex + 1) {
					resultList.add(subObject.getText());
					return;
				}
				this.findElementAt(resultList, path, curPathIndex + 1, subObject);
				break;
			}
		}
	}

	@SuppressWarnings("unchecked")
	public List<String> execQuery(ParsingObject queryTree, ParsingObject targetObject) 
			throws QueryExecutionException {
		this.targetObject = targetObject;
		return (List<String>) this.dispatch(queryTree);
	}

	@Override
	public Object visitPath(ParsingObject queryTree) {
		final int size = queryTree.size();
		String[] path = new String[size];
		for(int i = 0; i < size; i++) {
			path[i] = queryTree.get(i).getText();
		}
		return path;
	}

	@Override
	public Object visitError(ParsingObject queryTree) {
		throw new QueryExecutionException("invalid query:" + System.lineSeparator() + queryTree);
	}

	public static class QueryExecutionException extends RuntimeException {
		private static final long serialVersionUID = -5774942100147563362L;

		public QueryExecutionException(String message) {
			super(message);
		}
	}
}
