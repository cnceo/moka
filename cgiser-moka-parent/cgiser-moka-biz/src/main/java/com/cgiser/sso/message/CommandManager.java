package com.cgiser.sso.message;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xsocket.connection.ConnectionUtils;
import org.xsocket.connection.IServer;
import org.xsocket.connection.Server;
import org.xsocket.connection.IConnection.FlushMode;

import com.cgiser.core.common.cache.mem.MemCachedManager;

public class CommandManager {
	protected static IServer srv = null;
	Logger logger = LoggerFactory.getLogger(CommandManager.class);
	private XSocketDataHandler xSocketDataHandler;
    private MemCachedManager memCachedManager;
	public CommandManager() {
		try {
			logger.debug("聊天服务器已开启运行");
			xSocketDataHandler = new XSocketDataHandler();
			xSocketDataHandler.registerCommand(1000, LoginCommand.class);
			logger.debug("注册消息号为1000的Command");
//			xSocketDataHandler.registerCommand(1005, MessageCommand.class);
//			logger.debug("注册消息号为1005的消息处理Command");
			
		    //注册信息号为1001的命令
			xSocketDataHandler.registerCommand(1001, WorldMessageCommand.class);
			logger.debug("注册消息号为1001的Command");
            
            //注册信息号为1002的命令
            xSocketDataHandler.registerCommand(1002, SingleMessageCommand.class);
            logger.info("注册消息号为1002的Command");
            
            //注册信息号为1003的命令
            xSocketDataHandler.registerCommand(1003, GroupMessageCommand.class);
            logger.debug("注册消息号为1003的Command");
            
            xSocketDataHandler.registerCommand(1, GetCardMessageCommand.class);
            //心跳检测command
			xSocketDataHandler.registerCommand(1020, HeartCommand.class);
			srv = new Server(8090, xSocketDataHandler);
			srv.setFlushmode(FlushMode.ASYNC);
			srv.setAutoflush(false);
			srv.setIdleTimeoutMillis(2 * 60 * 1000);
			srv.setWorkerpool(Executors.newFixedThreadPool(20));
			ConnectionUtils.start(srv);
			// srv.run();
			// the call will not return
			// ... or start it by using a dedicated thread
//			srv.start(); // returns after the server has been started
			logger.debug("服务器" + srv.getLocalAddress() + ":" + 8090);
			Map<String, Class> maps = srv.getOptions();
			if (maps != null) {

				for (Entry<String, Class> entry : maps.entrySet()) {
					logger.debug("key= " + entry.getKey()
							+ " value =" + entry.getValue().getName());
				}
			}
			logger.debug("日志: " + srv.getStartUpLogMessage());
		} catch (Exception ex) {
			logger.error("chat error:"+ex.getMessage());
		}
	}
	public XSocketDataHandler getxSocketDataHandler() {
		return xSocketDataHandler;
	}
	public void setxSocketDataHandler(XSocketDataHandler xSocketDataHandler) {
		this.xSocketDataHandler = xSocketDataHandler;
	}
	public MemCachedManager getMemCachedManager() {
		return memCachedManager;
	}
	public void setMemCachedManager(MemCachedManager memCachedManager) {
		this.memCachedManager = memCachedManager;
		this.xSocketDataHandler.setMemCachedManager(memCachedManager);
	}
}
