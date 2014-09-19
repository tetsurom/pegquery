package pegquery;

import java.util.List;

public interface QueryFunction {
	public Object invoke(List<Object> argList);
}
