package C2G8_Project;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Queue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import C2G8_Project.DispatchConsumer;
import C2G8_Project.Dispatcher;
import C2G8_Project.DispatcherMessage;

/**
 * Dispatcher Mock class designed to test if Dispatcher has correctly
 * dispatched messages to subscribers
 * 
 * @author Jayson Mendoza
 *
 */
public class DispatchSubscriberTester  extends Thread implements Runnable, DispatchConsumer,Iterable<DispatcherMessage> {
	private final static Logger LOG =LogManager.getFormatterLogger(DispatchSubscriberTester.class);
	private static final String namePrefix = "Dispatcher";
	private static int nextDispatcherNum=1;
	private final String idName;
	private volatile boolean isRunning = false;
	private volatile int queueSize = 0;
	private final Queue<DispatcherMessage> messageQueue = new ArrayDeque<DispatcherMessage>();
	
	private DispatchSubscriberTester(Dispatcher dis) {
		idName = String.format("%s: [%s] %d",namePrefix,dis.getName(),nextDispatcherNum++);
		this.setName(idName);
	}
	
	public void receiveDispatch(String topic, String data) {
		// TODO Auto-generated method stub
		DispatcherMessage msg = new DispatcherMessage(topic,data);
		messageQueue.add(msg);
		queueSize=messageQueue.size();
		LOG.info("[%s] Message Received: queuesize=%d topic: %s, data: %s",idName,queueSize,topic,data);
	}

	@Override
	public String getSubscriberNameIdentifier() {
		return idName;
	}

	@Override
	public void run() {
		isRunning = true;
		while(isRunning) {
			
		}
		
	}

	public boolean isRunning(){
		return isRunning;
	}
	
	public void shutdown() {
		isRunning=false;
	}

	@Override
	public Iterator<DispatcherMessage> iterator() {
		return messageQueue.iterator();
	}
	public boolean isEmpty() {
		return messageQueue.size() == 0;
	}
	
	public int size() {
		return queueSize;
	}
	
	public void clear() {
		messageQueue.clear();
	}
	
	public boolean hasReceivedTopic(String topic) {
		ArrayList<DispatcherMessage> list = new ArrayList<DispatcherMessage>(messageQueue);
		for(DispatcherMessage msg : list) {
			if(msg.topic()==topic) {
				return true;
			}
		}
		return false;
	}

	public DispatcherMessage receiveData() {
		Runnable run = new Runnable() {

			@Override
			public void run() {
				while(queueSize==0) {}
				System.out.println("Done!");
			}
		};
		Thread td = new Thread(run);
		td.setName("Data Runner");
		td.start();
		while(td.isAlive()) {}
		return messageQueue.remove();
	}
	
	static DispatchSubscriberTester createSubscriberTester(Dispatcher dispatcher) {
		DispatchSubscriberTester tester = new DispatchSubscriberTester(dispatcher);
		tester.start();
		while(!tester.isRunning) {}
		System.out.println("Tester Setup complete");
		return tester;
	}

}
