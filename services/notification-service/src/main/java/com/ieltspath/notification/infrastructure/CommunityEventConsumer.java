package com.ieltspath.notification.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.notification.application.NotificationPreferenceService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import com.rabbitmq.client.Channel;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class CommunityEventConsumer {
    private static final Set<String> TYPES = com.ieltspath.notification.application.NotificationEventTypes.ALL;
    private final ObjectMapper mapper; private final JdbcTemplate jdbc; private final TransactionTemplate transaction;
    private final SimpMessagingTemplate socket; private final RabbitTemplate rabbit;
    private final NotificationPreferenceService preferences;
    public CommunityEventConsumer(ObjectMapper mapper, JdbcTemplate jdbc, TransactionTemplate transaction, SimpMessagingTemplate socket, RabbitTemplate rabbit, NotificationPreferenceService preferences) {
        this.mapper=mapper; this.jdbc=jdbc; this.transaction=transaction; this.socket=socket; this.rabbit=rabbit; this.preferences=preferences;
    }
    @RabbitListener(queues = "community.notifications.v1", ackMode = "MANUAL")
    public void receive(Message message, Channel channel) throws Exception {
        long tag=message.getMessageProperties().getDeliveryTag();
        try {
            @SuppressWarnings("unchecked") Map<String,Object> e=mapper.readValue(message.getBody(),Map.class);
            UUID event=UUID.fromString(str(e,"eventId")), actor=UUID.fromString(str(e,"actorId")), recipient=UUID.fromString(str(e,"recipientId"));
            String type=str(e,"eventType"); Instant at=Instant.parse(str(e,"occurredAt"));
            if(!TYPES.contains(type)||!type.equals(message.getMessageProperties().getReceivedRoutingKey())||!event.toString().equals(message.getMessageProperties().getMessageId())||actor.equals(recipient)) throw new IllegalArgumentException("Unsupported community event");
            UUID post=uuid(e.get("postId")), comment=uuid(e.get("commentId"));
            if ((type.contains("commented")||type.contains("replied")||type.contains("mentioned")||type.contains("reacted")) && post==null) throw new IllegalArgumentException("Missing post reference");
            if ((type.contains("commented")||type.contains("replied")||type.contains("mentioned")) && comment==null) throw new IllegalArgumentException("Missing comment reference");
            boolean inserted=Boolean.TRUE.equals(transaction.execute(s -> {
                if (!preferences.isEnabled(recipient, type)) return false;
                return jdbc.update("INSERT INTO notifications(id,source_event_id,recipient_id,actor_id,event_type,post_id,comment_id,created_at) VALUES (?,?,?,?,?,?,?,?) ON CONFLICT DO NOTHING",event,event,recipient,actor,type,post,comment,java.sql.Timestamp.from(at))>0;
            }));
            channel.basicAck(tag,false);
            if(inserted) try { socket.convertAndSendToUser(recipient.toString(),"/queue/notifications",Map.of("id",event,"actorId",actor,"eventType",type,"postId",post==null?"":post.toString(),"createdAt",at.toString())); } catch (RuntimeException ignored) { /* REST reconciliation remains authoritative after push failure. */ }
        } catch(IllegalArgumentException | com.fasterxml.jackson.core.JsonProcessingException bad) {
            publishConfirmed("community.events.dlx","community.notifications.v1.dlq",message);
            channel.basicAck(tag,false);
        } catch(Exception transientFailure) {
            Object attemptsValue=message.getMessageProperties().getHeaders().get("community-retry-count");
            int attempts=attemptsValue instanceof Number n?n.intValue():0;
            if(attempts>=2) publishConfirmed("community.events.dlx","community.notifications.v1.dlq",message);
            else { message.getMessageProperties().setHeader("community-retry-count",attempts+1); publishConfirmed("community.events.retry","retry."+message.getMessageProperties().getReceivedRoutingKey(),message); }
            channel.basicAck(tag,false);
        }
    }
    private static String str(Map<String,Object> e,String k){Object v=e.get(k);if(!(v instanceof String s)||s.isBlank())throw new IllegalArgumentException("Invalid event");return s;}
    private static UUID uuid(Object v){return v==null?null:UUID.fromString(v.toString());}
    private void publishConfirmed(String exchange,String routingKey,Message message) throws Exception {
        var correlation=new CorrelationData(UUID.randomUUID().toString());
        rabbit.send(exchange,routingKey,message,correlation);
        var confirmation=correlation.getFuture().get(5,TimeUnit.SECONDS);
        if(!confirmation.isAck() || correlation.getReturned()!=null) throw new IllegalStateException("RabbitMQ did not confirm event parking");
    }
}
