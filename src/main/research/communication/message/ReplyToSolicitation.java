package main.research.communication.message;

import static main.research.SetParam.ReplyType;

import main.research.agent.Agent;

public class ReplyToSolicitation extends Message {
	static private int replies = 0;

	ReplyType replyType;

	public ReplyToSolicitation( Agent from, Agent to, ReplyType replyType ) {
		super( from, to );
		replies++;
		this.replyType = replyType;
	}

	public ReplyType getReplyType() {
		return replyType;
	}

	public static int getReplies() {
		return replies;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append( "From: " + from.id ).append( "(" + from.role + ") , " );
		sb.append( "To: " + to.id ).append( "(" + to.role + ") , " );
		sb.append( "Reply: " + replyType );
		return sb.toString();
	}

	@Override
	public void clear() {
		replies = 0;
	}
}
