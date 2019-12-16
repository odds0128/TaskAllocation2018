package main.research.communication.message;

import static main.research.Parameter.ReplyType;

import main.research.agent.Agent;
import main.research.task.Subtask;

public class ReplyToSolicitation extends Message {
	static private int replies = 0;

	ReplyType replyType;
	Subtask   subtask;

	public ReplyToSolicitation( Agent from, Agent to, ReplyType replyType, Subtask subtask ) {
		super( from, to );
		replies++;
		this.replyType = replyType;
		this.subtask   = subtask;
	}

	public ReplyType getReplyType() {
		return replyType;
	}

	public Subtask getSubtask() {
		return subtask;
	}

	public static int getReplies() {
		return replies;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append( "Reply        - " );
		sb.append( "From: " + from );
		sb.append( "To: " + to );
		sb.append( "Reply: " + replyType );
		return sb.toString();
	}

	@Override
	public void clear() {
		replies = 0;
	}
}
