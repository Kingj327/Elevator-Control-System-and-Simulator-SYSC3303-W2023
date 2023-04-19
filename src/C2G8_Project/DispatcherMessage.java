package C2G8_Project;

/**
 * A wrapper class for all messages that pass through the Dispatcher.
 * Each message must have a topic used to identify which subscribers
 * will receive it in the target system. It also contains data that
 * may be relevant to the message in JSON format.
 * @author Jayson Mendoza
 *
 */
public record DispatcherMessage(
	String topic,
	String data
) {}
