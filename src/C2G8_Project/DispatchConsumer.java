package C2G8_Project;

/**
 * This interface is used to identify an object that can
 * subscribe to the Dispatcher and forces them to implement
 * a method to receive data from subscribed topics.
 * 
 * DispatchConsumers should NOT do any work other than data
 * conversion in their receiveDispatch method or any method called
 * within. Use Asynchronous design to process the data within another
 * thread's execution context or it will cause delays within the Dispatcher.
 * @author Jayson Mendoza
 *
 */
public interface DispatchConsumer {
	/**
	 * Called by Dispatcher when a subscribed topic has received a message.
	 * This should implement an asynchronous design within the DispatchConsumer
	 * and the consumer should only convert data and process it later. If this
	 * function does anything more or calls methods that do more then it is the 
	 * dispatchers thread doing that work and it will be delay other parts of the
	 * system from functioning during this time.
	 * @param topic The topic under which the data was received
	 * @param data The data contained within the message in JSON format
	 */
	void receiveDispatch(String topic,String data);
	
	/**
	 * This is the name that will be used to identify this DispatchConsumer within the Dispatchers logs
	 * @return The string name that will be used to represent this dispatch consmer in logs
	 */
	String getSubscriberNameIdentifier();
}
