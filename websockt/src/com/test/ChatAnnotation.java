package com.test;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/getServer/{code}")
public class ChatAnnotation {
	private static final String GUEST_PREFIX = "Guest";
	private static final AtomicInteger connectionIds = new AtomicInteger(0);
	private static final Set<ChatAnnotation> connections = new CopyOnWriteArraySet<ChatAnnotation>();

	private final String nickname;
	private Session session;
	
	public ChatAnnotation() {
		nickname = GUEST_PREFIX + connectionIds.getAndIncrement();
	}

	@OnOpen
	public void onOpen(Session session, @PathParam("code") String code) {
		session.getUserProperties().put("code", code);
		this.session = session;
		connections.add(this);
		try {
			session.getBasicRemote().sendText(code);
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
	
    /**  
     * 收到客户端消息时触发  
     * @param relationId  
     * @param userCode  
     * @param message  
     * @return  
    */  
    @OnMessage
    public void onMessage(Session session, String message) {
    	broadcast(message);
    }
  
    /**  
     * 异常时触发
     * @param relationId
     * @param userCode
     * @param session
    */
    @OnError
    public void onError(Throwable throwable,Session session) {}
  
    /**  
     * 关闭连接时触发 
     * @param relationId  
     * @param userCode
     * @param session
    */
    @OnClose
    public void onClose(Session session) {
    	connections.remove(this);
    }
    
    public static void broadcast(String msg) {
		for (ChatAnnotation client : connections) {
			try {
				synchronized (client) {
					String code = client.session.getUserProperties().get("code").toString();
		            if (code.equals(msg)) {
		            	client.session.getBasicRemote().sendText("I find you");
		            }
				}
			} catch (IOException e) {
				connections.remove(client);
				try {
					client.session.close();
				} catch (IOException e1) {
				}
			}
		}
	}
    
}
