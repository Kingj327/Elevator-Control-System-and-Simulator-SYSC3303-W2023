package C2G8_Project;

import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Dispatch worker thread notifies observers via separate thread to prevent
 * case where calling observer's receiveDispatch function delays or causes a message to be missed.
 * 
 * If we  where to make this production ready these calls should be guarded by an execution time limit
 * that logs an error if an algorithm is greedy. Alternately callback could be implemented.
 * @author Jayson Mendoza
 *
 */
public class DispatchNotificationWorker implements Runnable {
	final private static Logger LOG =LogManager.getFormatterLogger(DispatchNotificationWorker.class);
	private final HashSet<DispatchConsumer> dispatchList;
	private final DispatcherMessage msg;
	
	/**
	 * Creates a DispatchNotificationWorker using a specific context for distribution.
	 * @param dispatchList The distribution list to be used
	 * @param msg The message to be dispatched to the distribution list
	 */
	DispatchNotificationWorker(HashSet<DispatchConsumer> dispatchList,DispatcherMessage msg) {
		this.msg = msg;
		this.dispatchList = dispatchList;
	}
	
	/**
	 * Dispatches the message to all observers then terminates
	 */
	@Override
	public void run() {
		for(DispatchConsumer c : dispatchList) {
			c.receiveDispatch(msg.topic(),msg.data());
			LOG.info("Dispatch to %s with topic %s",c.getSubscriberNameIdentifier(),msg.topic());
		}
	}
}
