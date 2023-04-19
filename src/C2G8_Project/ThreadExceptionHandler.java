package C2G8_Project;

import java.lang.Thread.UncaughtExceptionHandler;

public class ThreadExceptionHandler implements UncaughtExceptionHandler {

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		if(e instanceof Error) {
			throw new Error(e.toString());
		}
	}

}
