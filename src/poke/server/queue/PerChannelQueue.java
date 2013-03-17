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
package poke.server.queue;

import java.lang.Thread.State;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.monitor.HeartMonitor;
import poke.server.conf.ServerConf.GeneralConf;
import poke.server.resources.Resource;
import poke.server.resources.ResourceFactory;
import poke.server.resources.ResourceUtil;
import poke.server.routing.RoutingConnection;

import com.google.protobuf.GeneratedMessage;

import eye.Comm.Header.ReplyStatus;
import eye.Comm.Request;
import eye.Comm.Response;

/**
 * A server queue exists for each connection (channel).
 * 
 * @author gash
 * 
 */
public class PerChannelQueue implements ChannelQueue {
	protected static Logger logger = LoggerFactory.getLogger("server");

	private Channel channel;
	private LinkedBlockingDeque<com.google.protobuf.GeneratedMessage> inbound;
	private LinkedBlockingDeque<com.google.protobuf.GeneratedMessage> outbound;
	private OutboundWorker oworker;
	private InboundWorker iworker;

	// not the best method to ensure uniqueness
	private ThreadGroup tgroup = new ThreadGroup("ServerQueue-" + System.nanoTime());

	private HashMap<String, RoutingConnection> routingConnections = new HashMap<String, RoutingConnection>();
	private List<GeneralConf> serverMap;
	private String nodeId;
	
	protected PerChannelQueue(Channel channel, String nodeid, List<GeneralConf> servers) {
		this.channel = channel;
		this.nodeId = nodeid;
		this.serverMap = servers;
		
		init();
	}

	protected void init() {
		inbound = new LinkedBlockingDeque<com.google.protobuf.GeneratedMessage>();
		outbound = new LinkedBlockingDeque<com.google.protobuf.GeneratedMessage>();

		iworker = new InboundWorker(tgroup, 1, this);
		iworker.start();

		oworker = new OutboundWorker(tgroup, 1, this);
		oworker.start();

		// let the handler manage the queue's shutdown
		// register listener to receive closing of channel
		// channel.getCloseFuture().addListener(new CloseListener(this));
		
		// above is already handled using QueueFactory
	}

	protected Channel getChannel() {
		return channel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see poke.server.ChannelQueue#shutdown(boolean)
	 */
	@Override
	public void shutdown(boolean hard) {
		logger.info("server is shutting down");

		channel = null;

		if (hard) {
			// drain queues, don't allow graceful completion
			inbound.clear();
			outbound.clear();
		}

		if (iworker != null) {
			iworker.forever = false;
			if (iworker.getState() == State.BLOCKED
					|| iworker.getState() == State.WAITING)
				iworker.interrupt();
			iworker = null;
		}

		if (oworker != null) {
			oworker.forever = false;
			if (oworker.getState() == State.BLOCKED
					|| oworker.getState() == State.WAITING)
				oworker.interrupt();
			oworker = null;
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see poke.server.ChannelQueue#enqueueRequest(eye.Comm.Finger)
	 */
	@Override
	public void enqueueRequest(Request req) {
		try {
			inbound.put(req);
		} catch (InterruptedException e) {
			logger.error("message not enqueued for processing", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see poke.server.ChannelQueue#enqueueResponse(eye.Comm.Response)
	 */
	@Override
	public void enqueueResponse(Response reply) {
		try {
			outbound.put(reply);
		} catch (InterruptedException e) {
			logger.error("message not enqueued for reply", e);
		}
	}
	
	public GeneralConf getRouting(byte[] key) {
		if(serverMap != null && serverMap.size() > 0) {
			int index = -1;
			if(key != null)
				index = hash(key, 1) % serverMap.size();
			
			GeneralConf server = null;
			if( index != -1 ) {
				server = serverMap.get(index);
				if(! nodeId.equals(server.getId())) {
					logger.info("*******Route Request to NodeId " + server.getId());
					return server;
				}
			}
		}
		return null;
	}
	
	/* 
     * Ported by Derek Young from the C version (specifically the endian-neutral version) 
     * from: http://murmurhash.googlepages.com/
     * 
     * released to the public domain - dmy999@gmail.com
     */
	public int hash(byte[] data, int seed)
    {
        // 'm' and 'r' are mixing constants generated offline.
        // They're not really 'magic', they just happen to work well.
        int m = 0x5bd1e995;
        int r = 24;

        // Initialize the hash to a 'random' value
        int len = data.length;
        int h = seed ^ len;

        int i = 0;
        while (len >= 4)
        {
            int k = data[i + 0] & 0xFF;
            k |= (data[i + 1] & 0xFF) << 8;
            k |= (data[i + 2] & 0xFF) << 16;
            k |= (data[i + 3] & 0xFF) << 24;

            k *= m;
            k ^= k >>> r;
            k *= m;

            h *= m;
            h ^= k;

            i += 4;
            len -= 4;
        }

        switch (len)
        {
            case 3: h ^= (data[i + 2] & 0xFF) << 16;
            case 2: h ^= (data[i + 1] & 0xFF) << 8;
            case 1: h ^= (data[i + 0] & 0xFF);
                    h *= m;
        }

        h ^= h >>> 13;
        h *= m;
        h ^= h >>> 15;

        return h  & 0x7FFFFFFF; //Return +ve hash
    }	

	protected class OutboundWorker extends Thread {
		int workerId;
		PerChannelQueue sq;
		boolean forever = true;

		public OutboundWorker(ThreadGroup tgrp, int workerId, PerChannelQueue sq) {
			super(tgrp, "outbound-" + workerId);
			this.workerId = workerId;
			this.sq = sq;

			if (outbound == null)
				throw new RuntimeException(
						"connection worker detected null queue");
		}

		@Override
		public void run() {
			Channel conn = sq.channel;
			if (conn == null || !conn.isOpen()) {
				PerChannelQueue.logger
						.error("connection missing, no outbound communication");
				return;
			}

			while (true) {
				if (!forever && sq.outbound.size() == 0)
					break;

				try {
					// block until a message is enqueued
					GeneratedMessage msg = sq.outbound.take();
					if (conn.isWritable()) {
						boolean rtn = false;
						if (channel != null && channel.isOpen()
								&& channel.isWritable()) {
							ChannelFuture cf = channel.write(msg);

							// blocks on write - use listener to be async
							cf.awaitUninterruptibly();
							rtn = cf.isSuccess();
							if (!rtn)
								sq.outbound.putFirst(msg);
						}

					} else
						sq.outbound.putFirst(msg);
				} catch (InterruptedException ie) {
					break;
				} catch (Exception e) {
					PerChannelQueue.logger.error(
							"Unexpected communcation failure", e);
					break;
				}
			}

			if (!forever) {
				PerChannelQueue.logger.info("connection queue closing");
			}
		}
	}

	protected class InboundWorker extends Thread {
		int workerId;
		PerChannelQueue sq;
		boolean forever = true;

		public InboundWorker(ThreadGroup tgrp, int workerId, PerChannelQueue sq) {
			super(tgrp, "inbound-" + workerId);
			this.workerId = workerId;
			this.sq = sq;

			if (outbound == null)
				throw new RuntimeException(
						"connection worker detected null queue");
		}

		@Override
		public void run() {
			Channel conn = sq.channel;
			if (conn == null || !conn.isOpen()) {
				PerChannelQueue.logger.error("connection missing, no inbound communication");
				return;
			}

			while (true) {
				if (!forever && sq.inbound.size() == 0)
					break;

				try {
					// block until a message is enqueued
					GeneratedMessage msg = sq.inbound.take();

					// process request and enqueue response
					if (msg instanceof Request) {
						Request req = ((Request) msg);
						logger.info("Received request on nodeId " + nodeId + " Tag:" + req.getHeader().getTag());
						Resource rsc = ResourceFactory.getInstance().resourceInstance(req.getHeader().getRoutingId());
						if (rsc == null) {
							logger.error("failed to obtain resource for " + req);
							Response reply = ResourceUtil.buildError(req.getHeader(), ReplyStatus.FAILURE, "Request not processed");
							sq.enqueueResponse(reply);
						}
						else {
							// temporarily using tag to identify where to route request, will need to come up with better 
							// mechanism.
							String tag = req.getHeader().getTag();
							GeneralConf server = sq.getRouting(tag.getBytes());
							if(server == null) {
								Response reply = rsc.process(req);
								sq.enqueueResponse(reply);
							}
							else {
								// this request needs to be routed to other server 
								// lets check if we have connection to that server, if we do then simply add this request
								// to its outbound Queue. If we don't then create new RoutingConnection and pass
								// request to it.
								// Use ServiceMonitor to first check if server is available
								if( ! HeartMonitor.getInstance().isServerRunning(server.getId())) {
									logger.error("remote server is not ready to accept request " + req);
									Response reply = ResourceUtil.buildError(req.getHeader(), ReplyStatus.FAILURE, "Request not processed");
									sq.enqueueResponse(reply);
								}
								else {
									RoutingConnection clientConnection = routingConnections.get(server.getId());
									if( clientConnection == null ) {
										clientConnection = RoutingConnection.initConnection(server, sq);
										routingConnections.put(server.getId(), clientConnection);
									}
									clientConnection.enqueueRequest(req);
								}
							}
						}
					}

				} catch (InterruptedException ie) {
					break;
				} catch (Exception e) {
					PerChannelQueue.logger.error(
							"Unexpected processing failure", e);
					break;
				}
			}

			if (!forever) {
				PerChannelQueue.logger.info("connection queue closing");
			}
		}
	}

//	public class CloseListener implements ChannelFutureListener {
//		private ChannelQueue sq;
//
//		public CloseListener(ChannelQueue sq) {
//			this.sq = sq;
//		}
//
//		@Override
//		public void operationComplete(ChannelFuture future) throws Exception {
//			sq.shutdown(true);
//		}
//	}
}
