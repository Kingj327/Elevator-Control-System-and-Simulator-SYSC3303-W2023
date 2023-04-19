package C2G8_Project;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * The dispatcher is designed to be a component for each program within the system and handles all in and out-bound
 * communications over the DatagramSockets. It implements a custom Publish/Subscribe framework that registers
 * destinations over a network and allows any DispatchConsumers to send and receive messages over the network.
 * 
 * Adding Destinations:
 * This should ideally be done prior to starting the Dispatcher after creating it.
 * 
 * DispatchConsumer's and Subscriptions:
 * DispatchConsumer Subscribers can receive messages by subscribing to a topic which is a string. Note that topics are not owned
 * by any individual subscriber and multiple DispatchConsumers may be subscribed to the same topic. When a topic has been 
 * received a function implemented by DispatchConsumers is called containing the topic and JSON data of the packet.
 * 
 * Converting binary data from JSON should be done using the Jackson ObjectMapper. The provided JSON can be converted into the desired type
 * by using the object mapper's <T> readValue(String topic,Class<T> class) method that will return an object of the specified class type T. 
 * Do not forget to handle the possibility that data could be corrupted at this point. Read the javadoc for the ObjectMapper for more details.
 * 
 * DispatchConsumer's should NOT do any significant work in the DispatchConsumer.receiveDispach function because it will
 * tie down the Dispatcher's execution context during that process. Instead DispatchConsumers should implement the asynchronous processing of
 * messages after converting it so that work happens within another thread's execution context.
 * 
 * DispatchConsumers can send messages to the dispatcher and do not need to convert their message. They simply provide the topic that will be used
 * and the data to be sent and it will be automatically converted into JSON and sent via UDP.
 * 
 * @author Jayson Mendoza
 *
 */
public class Dispatcher implements Runnable {
	final public static int NO_PORT = -1;
	final private static String CONNECT_TOPIC = "CONNECT_REQUEST";
	final private static Logger LOG =LogManager.getFormatterLogger(Dispatcher.class);
	final  private int TIMEOUT_MILLS;
	private ObjectMapper objMap;
	private final String name;
	private int port;
	private DatagramSocket socket;
	private volatile boolean isRunning = false;
	private int packetsDropped = 0;
	private int packetsProcessed = 0;
	private final Destinations destinationID;
	private volatile HashMap<Enum<?>,HashSet<InetSocketAddress>> destinations = new HashMap<Enum<?>,HashSet<InetSocketAddress>>();
	private volatile HashMap<DispatchConsumer,HashSet<String>> subscribers = new HashMap<DispatchConsumer,HashSet<String>>();
	private volatile HashMap<String,HashSet<DispatchConsumer>> topicSubscribers = new HashMap<String,HashSet<DispatchConsumer>>();
	private volatile HashSet<Destinations> pendingDestinations = new HashSet<Destinations>();
	
	/**
	 * Creates Dispatcher bound to a specific port.
	 * 
	 * The dispatcher does not deal with any errors and passes them on
	 * for the class using it to handle.
	 * 
	 * @param name The name of the dispatcher that will show in logs
	 * @param port The port on the local machine where the socket will be bound and listen
	 * @throws SocketException 
	 */
	public Dispatcher(final Destinations destinationIdentifier,final String name,final int port,final int timeOutMills) throws SocketException {
		this.destinationID = destinationIdentifier;
		this.port =  port;
		
		if(destinationID!=null) {
			this.name=String.format("%s_DISPATCHER", destinationID);
		}
		else {
			this.name = name;			
		}
		
		this.TIMEOUT_MILLS = timeOutMills;
		objMap = new ObjectMapper();
		LOG.info("%s: Dispatcher setup complete.",this.name);
		init();
	}
	
	public Dispatcher(final String name,final int port) throws SocketException {
		this(null,name,port,500);
	}
	
	/**
	 * Creates dispatcher bound to a random port
	 * @param destinationIdentifier The Destination of the dispatcher to be represented
	 * @throws SocketException 
	 */
	public Dispatcher(final Destinations destinationIdentifier) throws SocketException {
		this(destinationIdentifier,null,NO_PORT,500);
	}
	
	public Dispatcher(final Destinations destinationIdentifier,final int port) throws SocketException {
		this(destinationIdentifier,null,port,500);
	}
	
	public String getName() {
		return name;
	}
	
	public int getPort() {
		return port;
	}

	public boolean isRunning() {
		return isRunning;
	}

	public int getPacketsDropped() {
		return packetsDropped;
	}

	public int getPacketsProcessed() {
		return packetsProcessed;
	}
	
	public InetAddress getLocalAddress() {
		
		if(socket==null) {
			return null;
		}
		return socket.getLocalAddress();
	}
	
	public <T extends Enum<?>> boolean isDestinationRegistered(T destination) {
		boolean rc = false;
		synchronized(destinations) {
			rc=destinations.containsKey(destination);
			destinations.notifyAll();
		}
		return rc;
	}
	
	/**
	 * EXPOSED FOR TESTING ONLY.
	 * This method exposes data for testing and should not be used outside a test
	 * environment or it could cause corruption if the data contain is changed.
	 * @return 
	 */
	public HashMap<Enum<?>, HashSet<InetSocketAddress>> getDestinations() {
		return destinations;
	}
	
	/**
	 * EXPOSED FOR TESTING ONLY.
	 * This method exposes data for testing and should not be used outside a test
	 * environment or it could cause corruption if the data contain is changed.
	 * @return 
	 */
	public HashMap<DispatchConsumer, HashSet<String>> getSubscribers() {
		return subscribers;
	}

	/**
	 * EXPOSED FOR TESTING ONLY.
	 * This method exposes data for testing and should not be used outside a test
	 * environment or it could cause corruption if the data contain is changed.
	 * @return 
	 */
	public HashMap<String, HashSet<DispatchConsumer>> getTopicSubscribers() {
		return topicSubscribers;
	}
	
	public void init() throws SocketException {
		try {
			if(port == NO_PORT) {
				socket = new DatagramSocket();
				port = socket.getLocalPort();
			}
			else {
				socket = new DatagramSocket(port);				
			}
			socket.setSoTimeout(TIMEOUT_MILLS); //Timeout interval. This prevents infinite blocking and making it more responsive to shutdown messages
		} catch (SocketException e) {
//			String errMsg = String.format("%s: Dispatcher encountered an error when binding to socket to port %d.\n%s",this.name,port,e.getMessage());
			e.printStackTrace();
			throw e;
		}
		LOG.info("%s listening on port %d",name,socket.getLocalPort());
	}

	/**
	 * Sets up a socket connection using either defaults or the provided port.
	 * Note that destinations should be added BEFORE starting the dispatcher.
	 * 
	 */
	@Override
	public void run() {
		LOG.info("%s: Dispatcher STARTED!",name);
		isRunning = true;
		while(!socket.isClosed() && isRunning) {
			DispatcherMessage msg = receiveData();
			if(msg!=null) {
				dispatchMessageToSubscribers(msg);									
			}
		}
		
		
		if(!socket.isClosed()) {
			socket.close();
		}
	}
	
	/**
	 * Initiates shutdown process that will lead to the eventual 
	 * termination of this runnable.
	 */
	public void shutdown() {
		LOG.info("%s: Dispatcher SHUTDOWN!",name);
		isRunning = false;
	}
	
	
	/**
	 * Subscribes a dispatcher to a topic
	 * 
	 * @param topic The topic to which this subscription applies. It doesn't need to be unique so multiple could receive the data if subscribed to the same topic
	 * @param subscriber The DispatchConsumer that will be called when a message arrives with a subscribed topic
	 */
	public void subscribe(String topic, DispatchConsumer subscriber) {
		if(topic==null) {
			LOG.warn("%s: %s cannot subscribe to a null topic",name,subscriber.getSubscriberNameIdentifier());
			return;
		}
		else if(subscriber == null) {
			LOG.warn("%s: A null subscriber cannot subscribe to the topic \"%s\"",name,topic);
			return;
		}
		
		synchronized(subscribers) {
			HashSet<String> topicList = subscribers.getOrDefault(subscriber, new HashSet<String>());
			topicList.add(topic);
			subscribers.put(subscriber, topicList);		
			subscribers.notifyAll();
		}
		
		
		synchronized(topicSubscribers) {
			HashSet<DispatchConsumer> subList = topicSubscribers.getOrDefault(topic, new HashSet<DispatchConsumer>());
			subList.add(subscriber);
			topicSubscribers.put(topic, subList);			
			topicSubscribers.notifyAll();
		}
		
		LOG.info("%s: Added subscription for subscriber %s to topic \"%s\"",name,subscriber.getSubscriberNameIdentifier(),topic);			
	}
	
	/**
	 * Removes a subscribers subscriptions for a given topic.
	 * 
	 * This method does not offer any indication if the subscription existed
	 * prior to the call.
	 * @param topic The topic that will trigger a dispatch to the DispatchConsumer
	 * @param subscriber The DispatchConsumer to be called when the topic has been received
	 */
	public void unSubscribe(String topic, DispatchConsumer subscriber) {
		if(topic=="") {
			LOG.warn("%s: %s cannot unsubscribe from a null topic",name,subscriber.getSubscriberNameIdentifier());
			return;
		}
		else if(subscriber == null) {
			LOG.warn("%s: Cannot unsubscribe a null subscriber from topic \"%s\"",name,topic);
			return;
		}
		
		synchronized(subscribers) {
			HashSet<String> topicList = subscribers.getOrDefault(subscriber, new HashSet<String>());
			topicList.remove(topic);			
			subscribers.notifyAll();
		}
		
		synchronized(topicSubscribers) {
			HashSet<DispatchConsumer> subList = topicSubscribers.getOrDefault(topic, new HashSet<DispatchConsumer>());
			subList.remove(subscriber);			
			topicSubscribers.notifyAll();
		}
		
		LOG.info("%s: Removed any subscriptions for subscriber %s to topic \"%s\"",name,subscriber.getSubscriberNameIdentifier(),topic);			
		
	}
	
	/**
	 * Registers a  with the dispatcher by communicating and verifying the destination. This is a synchronous call and
	 * will block the calling thread until the new destination is resolved or has failed.
	 * 
	 * The destination will only be registered if it is successful. Otherwise an error is thrown.
	 * 
	 * @param destinationEnum The destination identifier. This must be unique. If it already exists it will have its details overwritten.
	 * @param addr The address targeted by messages sent to this destination
	 * @param port The port targeted by messages sent to this destination
	 * @throws UnknownHostException 
	 */
	public void connectNewDestination(final Destinations destinationEnum,final  String addr,final int port) throws UnknownHostException,UnregisteredDispatcherDestination {
		if(this.destinationID==null) {
			String errMsg = String.format("%s: Dispatcher cannot use connectNewDestination without a destinationID being set in constructor.", name);
			LOG.error(errMsg);
			throw new UnregisteredDispatcherDestination(errMsg);
		}
		
		final int maxAttempts = 240; //If TIMEOUT_MILLS = 500 this will be 30 seconds
		int attempt = 0;
		
		final InetSocketAddress dest = new InetSocketAddress(addr, port);
		DispatcherConnectRequest data = new DispatcherConnectRequest(destinationID,destinationEnum);	
				
		/**
		 * This will send a request to the destination then wait until on  a timeout interval
		 * until the request is no longer pending, or the maximum attempts are reached.
		 */
		synchronized(pendingDestinations) {
			pendingDestinations.add(destinationEnum);
			
			while(pendingDestinations.contains(destinationEnum) && attempt < maxAttempts) {
				++attempt;
				HashSet<InetSocketAddress> destList = new HashSet<InetSocketAddress>();
				destList.add(dest);
				sendData(destinationEnum,CONNECT_TOPIC,data,destList);
				LOG.warn("%s: Attempting to connect to new destination %s@%s:%d from origin %s,attempt %d/%d", name, destinationEnum,addr,port,data.origin(),attempt,maxAttempts);
				try {
					pendingDestinations.wait(TIMEOUT_MILLS);
				} catch (InterruptedException e) {
					LOG.error("%s",e.getMessage());
				}
			}
			pendingDestinations.notifyAll();
		}
		
		boolean hasFailed = false;
		synchronized(destinations) {
			if(!destinations.containsKey(destinationEnum)) {
				hasFailed=true;
			}			
			destinations.notifyAll();
		}
		
		if(hasFailed) {
			String failMsg = String.format("%s: Failed to connect to new destination %s@%s:%d. Destination not added.",name,destinationEnum,addr,port);
			LOG.error(failMsg);
			throw new UnknownHostException(failMsg);
		}
		
		LOG.info("%s: Destination %s was successfully connected.", name,destinationEnum);
	}
	
	private void handleConnectTopicMessage(DispatcherConnectRequest  request,final InetAddress requestAddr, final int requestPort) {	
		//If the source of the request is this dispatcher then it was successful
		
		Destinations destToAdd =  null;

		if(destinationID != null && request.origin()==destinationID) {
			destToAdd = request.destination();
		}
		else {
			destToAdd = request.origin();
		}
			
			
		synchronized(destinations) {
			HashSet<InetSocketAddress> destinationList = destinations.getOrDefault(destToAdd, new HashSet<InetSocketAddress>());
			destinationList.add(new InetSocketAddress(requestAddr,requestPort));
			destinations.put(destToAdd,destinationList);
			destinations.notifyAll();
		}

		synchronized(pendingDestinations) {
			pendingDestinations.remove(request.destination()); //This must happen AFTER the destination is registered successfully				
			pendingDestinations.notifyAll();
		}
		
		if(destinationID != null && request.origin()==destinationID) {
			LOG.info("%s: Destination %s@%s:%d has responded and was successfully registered as a destination.", name,request.destination(),requestAddr.toString(), requestPort);			
		}
		else {
			try {
				sendData(request.origin(),CONNECT_TOPIC,request);
				LOG.info("%s: Received a %s request from source %s@%s:%d. %s was successfully registered as a destination.", name,CONNECT_TOPIC,request.origin(),requestAddr.toString(), requestPort,request.origin());
			} catch (UnregisteredDispatcherDestination e) {
				LOG.error("%s: Failed to register %s as a destination. Received a %s request from source %s@%s:%d. ", name,request.origin(),CONNECT_TOPIC,request.origin(),requestAddr.toString(), requestPort);
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Registers a destination to the Dispatcher. Ideally do this before the Dispatcher has been started for best results.
	 * 
	 * Destinations are used by DispatchConsumers and uniquely identify the target of a message. If the destination already
	 * exists it will be overwritten with the new destination address and port for future messages.
	 * @param <T> The Enumeration subtype that is used to identify the destination
	 * @param destinationEnum The destination identifier. This must be unique. If it already exists it will have its details overwritten.
	 * @param addr The address targeted by messages sent to this destination
	 * @param port The port targeted by messages sent to this destination
	 * @throws UnknownHostException If a destination address cannot be resolved
	 */
	public <T extends Enum<?>>void registerDestination(final T destinationEnum,final String addr, final int port) throws UnknownHostException {
		InetSocketAddress destAddr = new InetSocketAddress(addr,port);
		if(destAddr.isUnresolved()) {
			LOG.error("%s: Provided destination %s cannot be resolved at time of registration. %s:%d",name,destinationEnum,addr,port);
			throw new UnknownHostException(String.format("%s: Provided destination %s cannot be resolved at time of registration. %s:%d",name,destinationEnum,addr,port));
		}
		else if(destinationEnum.toString().equals(CONNECT_TOPIC)) {
			LOG.warn("%s: Provided destinationEnum %s is reserved and cannot be used. Please specify a valid destination enum. This request has been ignored.",name,CONNECT_TOPIC);
			return;
		}
		
		synchronized(destinations) {
			HashSet<InetSocketAddress> addressList = destinations.getOrDefault(destinationEnum, new HashSet<InetSocketAddress>());
			addressList.add(destAddr);
			HashSet<InetSocketAddress> old = destinations.put(destinationEnum,addressList);

			LOG.info("%s: Provided destination %s added registered to new endpoint %s:%d, resolved=%s",name,destinationEnum,addr,port,!destAddr.isUnresolved());
			
			destinations.notifyAll();
		}
	}
	
	/**
	 * Gets a destination from the mappings in a thread safe manner and then releases the lock
	 * @param <E> The enum type of the destination
	 * @param destination The destination identifier that has previously been registered
	 * @return The destination found or null if it was not found
	 */
	private <E extends Enum<?>> HashSet<InetSocketAddress> getDestination(E destination) {
		HashSet<InetSocketAddress> dest = null;
		synchronized(destinations) {
			dest = destinations.get(destination);
			
			destinations.notifyAll();
		}
		return dest;
	}
	
	/**
	 * Sends data without a topic to an established destination registered to the dispatcher.
	 * This method handles all the data conversion.
	 * 
	 * @param <E> The destination enum type used to identify a registered to the destination.
	 * @param <T> The type of the data to be sent
	 * @param destination The destination enum identifying the message target that's registered to the dispatcher.
	 * @param data The data to be sent
	 * @throws UnregisteredDispatcherDestination If the destination provided is not registered to this dispatcher
	 */
	public <E extends Enum<?>,T> void sendData(E destination,T data) throws UnregisteredDispatcherDestination {
		sendData(destination,"",data);
	}
	
	/**
	 * Sends data with a specific topic without any associated data to an established destination registered to the dispatcher.
	 * 
	 * @param <E> The destination enum type used to identify a registered to the destination.
	 * @param destination The destination enum identifying the message target that's registered to the dispatcher.
	 * @param topic The topic used to identify the message, its data, and data type to the receiving system
	 * @throws UnregisteredDispatcherDestination If the destination provided is not registered to this dispatcher
	 */
	public <E extends Enum<?>> void sendData(E destination,String topic) throws UnregisteredDispatcherDestination {
		sendData(destination,topic,null);
	}
	
	/**
	 * 
	 * Sends data with a specific topic to an established destination registered to the dispatcher.
	 * This method handles all the data conversion.
	 * 
	 * @param <E> The destination enum type used to identify a registered to the destination.
	 * @param <T> The type of the data to be sent
	 * @param destination destination The destination enum identifying the message target that's registered to the dispatcher.
	 * @param topic The topic used to identify the message, its data, and data type to the receiving system
	 * @param data The data to be sent
	 * @throws UnregisteredDispatcherDestination If the destination provided is not registered to this dispatcher
	 */
	public <E extends Enum<?>,T> void sendData(final E destination,final String topic,final T data) throws UnregisteredDispatcherDestination {
		if(!destinations.containsKey(destination)) {
			LOG.warn("%s: Provided destination %s was not found.",name,destination);
			throw new UnregisteredDispatcherDestination(String.format("%s: Provided destination %s was not found.",name,destination));
		}
		
		HashSet<InetSocketAddress> destAddr = new HashSet<InetSocketAddress>(getDestination(destination));
		sendData(destination,topic,data,destAddr);
	}
	
	/**
	 * 
	 * Sends data with a specific topic to an established destination registered to the dispatcher.
	 * This method handles all the data conversion.
	 * 
	 * @param <E> The destination enum type used to identify a registered to the destination.
	 * @param <T> The type of the data to be sent
	 * @param destination destination The destination enum identifying the message target that's registered to the dispatcher.
	 * @param topic The topic used to identify the message, its data, and data type to the receiving system
	 * @param data The data to be sent
	 * @param  destAddr The address where the message should be sent
	 */
	public <E extends Enum<?>,T> void sendData(final E destination,final String topic,final T data, final HashSet<InetSocketAddress> destAddr) {
		if(!isRunning) {
			return;
		}
		else if(topic == "") {
			LOG.warn("%s: Provided topic %s was null. Make sure this was intentional and the receiver has subscribed to an empty topic.",name);
		}
		
		String jsonData = "";
		String stringData = "";
		try {
			//This is to avoid the case where a string is passed and given an extra set of quotation marks
			if(data instanceof String) {
				stringData = (String)data;
			}
			else if(data!=null) {
				stringData = objMap.writeValueAsString(data);
			}
			DispatcherMessage msg = new DispatcherMessage(topic,stringData);
			jsonData = objMap.writeValueAsString(msg);			
		}
		catch (JsonProcessingException e) {
			LOG.error("%s: Failed to convert data into JSON. Send failed",name);
			e.printStackTrace();
			return;
		}
		
		byte[] buffer = jsonData.getBytes();
		
		for(InetSocketAddress addr : destAddr) {
			try {
				DatagramPacket packet = new DatagramPacket(buffer,buffer.length,addr);
				socket.send(packet);
				LOG.info("Sent packet with topic %s to %s:%d. Bytes: %d, message Data: %s",topic,packet.getAddress().toString(),packet.getPort(),packet.getLength(),stringData);					
				
			} catch (IOException e) {
				LOG.error("%s: Failed to send message to destination %s (%s:%d). Send failed",name,destination,addr.getAddress().toString(),addr.getPort());
				e.printStackTrace();
				return;
			}
			catch (IllegalArgumentException e) {
				LOG.error("%s: Failed to send message to destination because address cannot be resolved. %s (%s:%d).",name,destination,addr.getAddress().toString(),addr.getPort());
				return;
			}
		}
	}

	/**
	 * Receives data from the DatagramSocket and converts it into a DispatcherMessage
	 * @return The dispatcher message received from the socket
	 */
	private DispatcherMessage receiveData() {
		
		byte[] buffer = new byte[CONFIG.MAX_MESSAGE_BYTES];
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		try {
			socket.receive(packet);
			DispatcherMessage msg = objMap.readValue(packet.getData(), DispatcherMessage.class);
			LOG.info("Packet received from %s:%d with %d bytes and topic %s",packet.getAddress().toString(),packet.getPort(),packet.getLength(),msg.topic());
			
			if(msg.topic().equals(CONNECT_TOPIC)) {
				DispatcherConnectRequest request = objMap.readValue(msg.data(), DispatcherConnectRequest.class);
				handleConnectTopicMessage(request,packet.getAddress(),packet.getPort());
				return null;
			}
			else {
				return msg;
			}
		}
		catch (SocketTimeoutException e) {
			return null;
		}
		catch (IOException e) {
			LOG.error("%s: Dispatcher encountered an error when receiving a packet and has dropped it.\n%s",this.name,e.getMessage());
			e.printStackTrace();
			++packetsDropped;
			return null;
		}			
	}
	
	/**
	 * Retrieves all subscribers of a topic in a thread safe manner.
	 * Separated so that lock is released before list processing starts.
	 * @param topic The topic to query for subscribers
	 * @return A list of DispatchConsumers subscribed to the topic. Will return an empty list if none are subscribed.
	 */
	private HashSet<DispatchConsumer> getDispatchList(String topic) {

		HashSet<DispatchConsumer> dispatchList = null;
		
		synchronized(topicSubscribers) {
			dispatchList = new HashSet<DispatchConsumer>(topicSubscribers.getOrDefault(topic, new HashSet<DispatchConsumer>()));			
		}

		return dispatchList;
	}
	
	/**
	 * Sends Message to all subscribers of a topic specified in DispatcherMessage
	 * @param msg The DispatcherMessage containing a topic and data.
	 */
	private void dispatchMessageToSubscribers(DispatcherMessage msg) {
		HashSet<DispatchConsumer> dispatchList = null;
		++packetsProcessed;
		
		dispatchList = getDispatchList(msg.topic());
		DispatchNotificationWorker worker = new DispatchNotificationWorker(dispatchList,msg);
		Thread workerThread = new Thread(worker);
		workerThread.setName(String.format("DispatchNotificationWorkerThread_%d", packetsProcessed));
//		for(DispatchConsumer c : dispatchList) {
//			c.receiveDispatch(msg.topic(),msg.data());
//			LOG.info("Dispatch to %s with topic %s",c.getSubscriberNameIdentifier(),msg.topic());
//		}
		workerThread.start();
	}

}
