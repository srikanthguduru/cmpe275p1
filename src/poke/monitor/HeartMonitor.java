package poke.monitor;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.server.conf.ServerConf.GeneralConf;
import poke.server.conf.ServerConf.RouteConf;
import poke.server.management.ServerHeartbeat;
import eye.Comm.Heartbeat;
import eye.Comm.Management;
import eye.Comm.Network;
import eye.Comm.Network.Action;

public class HeartMonitor extends Thread {
	protected static Logger logger = LoggerFactory.getLogger("management-monitor");
	protected static AtomicReference<HeartMonitor> instance = new AtomicReference<HeartMonitor>();
	private static int SUCCESS_THRSHOLD = 5;
	private static int FAILURE_THRSHOLD = 3;
	private static int CONNECTION_TIMEOUT = ServerHeartbeat.sHeartRate;
	
	String nodeId;
	List<GeneralConf> servers;
	List<RouteConf> route;

	boolean forever = true;
	HashMap<String, Channel> channels = new HashMap<String, Channel>();
	HashMap<String, ChannelFuture> channelFutures = new HashMap<String, ChannelFuture>();
	HashMap<String, ClientBootstrap> bootstraps = new HashMap<String, ClientBootstrap>();
	
	HashMap<String, MonitorData> serverstatus = new HashMap<String, MonitorData>();
	
	public static HeartMonitor getInstance(String id, List<GeneralConf> servers) {
		instance.compareAndSet(null, new HeartMonitor(id, servers));
		return instance.get();
	}
	
	public static HeartMonitor getInstance() {
		return instance.get();
	}
	
	public synchronized void nodeStatus(Heartbeat beat) {
		if( beat.hasNodeId()) {
			logger.info("got heartbeat message from " + beat.getNodeId());
			
			MonitorData monitor = serverstatus.get(beat.getNodeId());
			if(monitor == null) {
				monitor = new MonitorData(beat.getNodeId());
				serverstatus.put(beat.getNodeId(), monitor);
			}
			monitor.success();
			
			if(beat.hasTimeRef()) 
				monitor.setTimeref(beat.getTimeRef());
		}
		else {
			logger.info("got heartbeat message withtout nodeId");
		}
	}

	/**
	 * Check if server is running 
	 * 
	 * @param nodeId
	 * @return
	 */
	public boolean isServerRunning(String nodeId) {
		MonitorData monitor = serverstatus.get(nodeId);
		if(monitor != null && monitor.successCount >= SUCCESS_THRSHOLD) { 
			return true;
		}
		return false;
	}
	
	protected void closeChannel(Channel channel) {
		if(channels.size() > 0) {
			Iterator<String> itr = channels.keySet().iterator();
			while(itr.hasNext()) {
				String serverId = itr.next();
				if(channels.get(serverId).equals(channel)) {
					logger.info("**********closing channel for nodeId " + serverId);
					channelFutures.remove(serverId); 
					channels.remove(serverId);
				}
			}
		}
	}
	
	protected HeartMonitor(String nodeId, List<GeneralConf> servers) {
		this.nodeId = nodeId;
		this.servers = servers;
	}
	
	protected ClientBootstrap initTCP() {
		ClientBootstrap bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(
				Executors.newCachedThreadPool(),
				Executors.newFixedThreadPool(2)));

		bootstrap.setOption("connectTimeoutMillis", CONNECTION_TIMEOUT);
		bootstrap.setOption("tcpNoDelay", true);
		bootstrap.setOption("keepAlive", true);
		
		bootstrap.setPipelineFactory(new MonitorPipeline());
		
		return bootstrap;
	}
	
	/**
	 * create connection to remote server
	 * 
	 * @return
	 */
	protected Channel connect(String serverId, String host, int port) {
		ClientBootstrap bootstrap = bootstraps.get(serverId);
		// Start the connection attempt.
		if (bootstrap == null) {
			bootstrap = initTCP();
			bootstraps.put(serverId, bootstrap);
		}
		ChannelFuture channel = channelFutures.get(serverId);
		if(channel == null) {
			channel = bootstrap.connect(new InetSocketAddress(host, port));
		}

		// wait for the connection to establish
		channel.awaitUninterruptibly();

		if (channel.isDone() && channel.isSuccess()) {
			channelFutures.put(serverId, channel);
			return channel.getChannel();
		}
		else
			logger.debug("Not able to establish connection to server " + host + ":" + port + " (" + serverId + ")");
		return null;
	}
	
	@Override
	public void run() {
		logger.info("starting server monitoring");

		while (forever) {
			try {
				if(servers != null && servers.size() > 0) {
					for(GeneralConf server: servers) {
						String nodeid = server.getNodeId();
						if(! nodeid.equals(nodeId)) {
							Channel channel = channels.get(nodeid);
							if(!(channel != null && channel.isConnected())) {
								Channel ch = connect(nodeid, server.getHost(), server.getPortMgmt());
								if(ch != null) {
									Network.Builder n = Network.newBuilder();
									n.setNodeId(nodeId);
									n.setAction(Action.NODEJOIN);
									Management.Builder m = Management.newBuilder();
									m.setGraph(n.build());
									ch.write(m.build());	
		
									channels.put(nodeid, ch);
								}
							}
						}
					}
				}
				Thread.sleep(ServerHeartbeat.sHeartRate);
			} catch (InterruptedException ie) {
				break;
			} catch (Exception e) {
				logger.error("Unexpected management communcation failure", e);
				break;
			}
		}

		if (!forever) {
			logger.info("shutting down server monitoring");
		}
	}

	public void release() {
		forever = true;
	}	

	class MonitorData {
		public MonitorData(String nodeId) {
			this.nodeId = nodeId;
			this.successCount = 0;
			this.failCount = 0;
		}

		public String nodeId;
		public int successCount;
		public int failCount;
		public long timeRef;
		
		public void success(){
			if(successCount < SUCCESS_THRSHOLD)
				successCount ++;
			failCount = 0;
		}
		public void fail() {
			failCount = failCount < FAILURE_THRSHOLD ? failCount++ : failCount;
			successCount = 0;
		}
		public void setTimeref(long value) {
			timeRef = value;
		}
	}
}
