/*
 * copyright 2012, gash
 * 
 * Gash licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package poke.server.routing;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.monitor.HeartMonitor;
import poke.server.conf.ServerConf.GeneralConf;
import poke.server.management.ServerHeartbeat;
import poke.server.queue.PerChannelQueue;

import com.google.protobuf.GeneratedMessage;

import eye.Comm.Request;

/**
 * provides an abstraction of the communication to the remote server.
 * 
 * @author gash
 * 
 */
public class RoutingConnection {
	protected static Logger logger = LoggerFactory.getLogger("routing");

	private GeneralConf server;
	private ChannelFuture channel; // do not use directly call connect()!
	private ClientBootstrap bootstrap;
	private LinkedBlockingDeque<com.google.protobuf.GeneratedMessage> outbound;
	private OutboundWorker worker;
	private PerChannelQueue queue;
	private boolean forever = true;
	
	protected RoutingConnection(GeneralConf target, PerChannelQueue que) {
		this.server = target;
		this.queue = que;
		
		init();
	}

	/**
	 * release all resources
	 */
	public void release() {
		forever = false;
		bootstrap.releaseExternalResources();
	}

	public static RoutingConnection initConnection(GeneralConf target, PerChannelQueue que) {
		RoutingConnection rtn = new RoutingConnection(target, que);
		return rtn;
	}
	
	public void enqueueRequest(Request req) {
		try {
			outbound.put(req);
		} catch (InterruptedException e) {
			logger.error("message not enqueued for processing", e);
		}
	}	

	private void init() {
		// the queue to support client-side surging
		outbound = new LinkedBlockingDeque<com.google.protobuf.GeneratedMessage>();

		// Configure the client.
		bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(
				Executors.newCachedThreadPool(),
				Executors.newCachedThreadPool()));

		bootstrap.setOption("connectTimeoutMillis", 10000);
		bootstrap.setOption("tcpNoDelay", true);
		bootstrap.setOption("keepAlive", true);

		// Set up the pipeline factory.
		bootstrap.setPipelineFactory(new RoutingDecoderPipeline(queue));

		// start outbound message processor
		worker = new OutboundWorker(this);
		worker.start();
	}

	/**
	 * create connection to remote server
	 * 
	 * @return
	 */
	protected Channel connect() {
		// Start the connection attempt.
		if (channel == null) {
			channel = bootstrap.connect(new InetSocketAddress(server.getHost(), server.getPort()));
		}

		// wait for the connection to establish
		channel.awaitUninterruptibly();

		if (channel.isDone() && channel.isSuccess()) {
			return channel.getChannel();
		}
		else
			logger.debug("Not able to establish connection to server " + server.getHost() + ":" + server.getPort());
		return null;
	}

	/**
	 * queues outgoing messages - this provides surge protection if the client
	 * creates large numbers of messages.
	 * 
	 * @author gash
	 * 
	 */
	protected class OutboundWorker extends Thread {
		RoutingConnection conn;
		
		public OutboundWorker(RoutingConnection conn) {
			this.conn = conn;

			if (conn.outbound == null)
				throw new RuntimeException("connection worker detected null queue");
		}

		@Override
		public void run() {
			Channel channel = conn.connect();
			
			while (forever) {
				try {
					if(channel == null || !channel.isConnected() || !channel.isOpen()) {
						while( ! HeartMonitor.getInstance().isServerRunning(conn.server.getNodeId())) {
							// remote server is not available sleep and try again after some time 
							Thread.sleep(ServerHeartbeat.getsHeartRate());
						}
						// set previous Channel Future to null
						conn.channel = null;
						channel = conn.connect();
					}
					if(channel != null) {
						// block until a message is enqueued
						GeneratedMessage msg = conn.outbound.take();
						// process request and enqueue response
						if (msg instanceof Request) {
							Request req = ((Request) msg);
							if (channel != null && channel.isOpen() 
									&& channel.isConnected() && channel.isWritable()) {
								boolean rtn = false;
								ChannelFuture cf = channel.write(req);
								
								// blocks on write - use listener to be async
								cf.awaitUninterruptibly();
								rtn = cf.isSuccess();
								if (!rtn)
									conn.outbound.putFirst(req);
							} else {
								conn.outbound.putFirst(req);
							}
						}
					}
				} catch (InterruptedException ie) {
					break;
				} catch (Exception e) {
					RoutingConnection.logger.error("Unexpected communcation failure", e);
					break;
				}
			}

			if (!forever) {
				RoutingConnection.logger.info("connection queue closing");
			}
		}
	}
}
