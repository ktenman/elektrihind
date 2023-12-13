package ee.tenman.elektrihind.queue;

import ee.tenman.elektrihind.car.PlateDetectionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class RedisMessageSubscriber implements MessageListener {

    @Resource
    private PlateDetectionService plateDetectionService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String messageBody = new String(message.getBody());
        log.debug("Received message from Redis queue: {}", messageBody);

        try {
            UUID uuid = extractUuidFromMessage(messageBody);
            String plateNumber = extractPlateNumberFromMessage(messageBody);
            log.info("Processing message [UUID: {}, PlateNumber: {}]", uuid, plateNumber);

            plateDetectionService.processDetectionResponse(uuid, plateNumber);
        } catch (Exception e) {
            log.error("Error processing message from Redis queue", e);
        }
    }

    private UUID extractUuidFromMessage(String message) {
        try {
            return UUID.fromString(message.split(":")[0]);
        } catch (Exception e) {
            log.error("Error extracting UUID from message: {}", message, e);
            throw e;
        }
    }

    private String extractPlateNumberFromMessage(String message) {
        try {
            return message.split(":")[1];
        } catch (Exception e) {
            log.error("Error extracting plate number from message: {}", message, e);
            throw e;
        }
    }
}