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
package poke.server;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.bootstrap.Bootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.AdaptiveReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.monitor.HeartMonitor;
import poke.server.conf.JsonUtil;
import poke.server.conf.ServerConf;
import poke.server.conf.ServerConf.GeneralConf;
import poke.server.management.ManagementDecoderPipeline;
import poke.server.management.ManagementQueue;
import poke.server.management.ServerHeartbeat;
import poke.server.resources.ResourceFactory;
import poke.server.routing.graph.OverlayNetwork;
import poke.server.storage.Storage;
import poke.server.storage.StorageFactory;

/**
 * Note high surges of messages can close down the channel if the handler cannot
 * process the messages fast enough. This design supports message surges that
 * exceed the processing capacity of the server through a second thread pool
 * (per connection or per server) that performs the work. Netty's boss and
 * worker threads only processes new connections and forwarding requests.
 * <p>
 * Reference Proactor pattern for additional information.
 * 
 * @author gash
 * 
 */
public class Server {
	protected static Logger logger = LoggerFactory.getLogger("server");

	protected static final ChannelGroup allChannels = new DefaultChannelGroup("server");
	protected static HashMap<Integer, Bootstrap> bootstrap = new HashMap<Integer, Bootstrap>();
	protected ChannelFactory cf, mgmtCF;
	protected ServerConf conf;
	protected ServerHeartbeat heartbeat;
	protected HeartMonitor serverMonitor;
	protected Storage storage;
	
	/**
	 * static because we need to get a handle to the factory from the shutdown
	 * resource
	 */
	public static void shutdown() {
		try {
			ChannelGroupFuture grp = allChannels.close();
			grp.awaitUninterruptibly(5, TimeUnit.SECONDS);
			for (Bootstrap bs : bootstrap.values())
				bs.getFactory().releaseExternalResources();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		logger.info("Server shutdown");
		System.exit(0);
	}

	/**
	 * initialize the server with a configuration of it's resources
	 * 
	 * @param cfg
	 */
	public Server(File cfg, String siteid) {
		/* Since nodeId == siteid in server.conf file we can init the DBStorage using the nodeId
		 * i.e. it will initialize the dbstorage for the shema of the current server (passed as arg) 
		 */
		init(cfg, siteid);
	}

	private void init(File cfg, String siteid) {
		// resource initialization - how message are processed
		BufferedInputStream br = null;
		try {
			byte[] raw = new byte[(int) cfg.length()];
			br = new BufferedInputStream(new FileInputStream(cfg));
			br.read(raw);
			conf = JsonUtil.decode(new String(raw), ServerConf.class);
						
			ResourceFactory.initialize(conf);
		} catch (Exception e) {
		}

		// communication - external (TCP) using asynchronous communication
		cf = new NioServerSocketChannelFactory(Executors.newCachedThreadPool(),
				Executors.newCachedThreadPool());

		// internal using TCP - a better option
		mgmtCF = new NioServerSocketChannelFactory(Executors.newCachedThreadPool(),
				Executors.newFixedThreadPool(5));
	}

	public void release() {
		if (heartbeat != null)
			heartbeat.release();
		if (serverMonitor != null)
			serverMonitor.release();
	}

	private void createPublicBoot(int port, String nodeId, List<GeneralConf> servers) {
		// construct boss and worker threads (num threads = number of cores)
		ServerBootstrap bs = new ServerBootstrap(cf);

		// Set up the pipeline factory.
		bs.setPipelineFactory(new ServerDecoderPipeline(nodeId, servers));

		// tweak for performance
		bs.setOption("child.tcpNoDelay", true);
		bs.setOption("child.keepAlive", true);
		bs.setOption("receiveBufferSizePredictorFactory",
				new AdaptiveReceiveBufferSizePredictorFactory(1024 * 2, 1024 * 4, 1048576));

		bootstrap.put(port, bs);

		// Bind and start to accept incoming connections.
		Channel ch = bs.bind(new InetSocketAddress(port));
		allChannels.add(ch);

		// We can also accept connections from a other ports (e.g., isolate read and writes)
		logger.info("Starting server, listening on port = " + port);
	}

	private void createManagementBoot(int port) {
		// construct boss and worker threads (num threads = number of cores)
		ServerBootstrap bs = new ServerBootstrap(mgmtCF);

		// Set up the pipeline factory.
		bs.setPipelineFactory(new ManagementDecoderPipeline());

		// tweak for performance
		// bs.setOption("tcpNoDelay", true);
		bs.setOption("child.tcpNoDelay", true);
		bs.setOption("child.keepAlive", true);

		bootstrap.put(port, bs);

		// Bind and start to accept incoming connections.
		Channel ch = bs.bind(new InetSocketAddress(port));
		allChannels.add(ch);

		logger.info("Starting management server, listening on port = " + port);
	}

	protected void run(String nodeId) {
		logger.info("Start Server nodeId " + nodeId);
		
		GeneralConf server = conf.findNodeById(nodeId);

		int port =  server.getPort();
		int mport = server.getPortMgmt();
		
		// storage initialization
		StorageFactory.initialize(conf, server);
		
		// start communication
		createPublicBoot(port, nodeId, conf.getServer());
		createManagementBoot(mport);

		// start management
		ManagementQueue.startup();

		// create overlay network graph
		OverlayNetwork.initialize(conf.getServer(), conf.getRoute());
		
		// start heartbeat
		String str = server.getNodeId();
		heartbeat = ServerHeartbeat.getInstance(str);
		ServerHeartbeat.setsHeartRate(conf.getHealthInterval() * 1000);
		heartbeat.start();
		
		serverMonitor = HeartMonitor.getInstance(str, conf.getConnectedNodes(nodeId));
		serverMonitor.start();
		
		logger.info("Server ready with nodeId " + nodeId + " port: " + port + " mgmtPort: " + mport);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 2) {
			System.err.println("Usage: java "
					+ Server.class.getClass().getName() + " conf-file nodeId");
			System.exit(1);
		}

		File cfg = new File(args[0]);
		if (!cfg.exists()) {
			Server.logger.error("configuration file does not exist: " + cfg);
			System.exit(2);
		}

		Server svr = new Server(cfg, args[1]);
		svr.run(args[1]);
	}
}
