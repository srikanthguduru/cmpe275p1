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

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelState;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.server.queue.PerChannelQueue;

public class RoutingHandler extends SimpleChannelUpstreamHandler {
	protected static Logger logger = LoggerFactory.getLogger("routing");

	private volatile Channel channel;
	private volatile PerChannelQueue queue;
	
	public RoutingHandler(PerChannelQueue que) {
		this.queue = que;
	}

	public void handleMessage(eye.Comm.Response msg) {
		queue.enqueueResponse(msg);
	}

	@Override
	public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		channel = e.getChannel();
		super.channelOpen(ctx, e);
	}

	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		if (channel.isConnected())
			channel.write(ChannelBuffers.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
	}

	@Override
	public void channelInterestChanged(ChannelHandlerContext ctx,
			ChannelStateEvent e) throws Exception {
		if (e.getState() == ChannelState.INTEREST_OPS
				&& ((Integer) e.getValue() == Channel.OP_WRITE)
				|| (Integer) e.getValue() == Channel.OP_READ_WRITE)
			logger.warn("channel is not writable! <--------------------------------------------");
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
		handleMessage((eye.Comm.Response) e.getMessage());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
		logger.error("exception: " + e.getCause(), e);
		e.getChannel().close();
	}
}
