package ee.tenman.elektrihind.queue;


import ee.tenman.elektrihind.car.PlateDetectionService;
import jakarta.annotation.Resource;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RedisMessageSubscriber implements MessageListener {

    @Resource
    private PlateDetectionService plateDetectionService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String messageBody = new String(message.getBody());

        UUID uuid = extractUuidFromMessage(messageBody);
        String plateNumber = extractPlateNumberFromMessage(messageBody);

        plateDetectionService.processDetectionResponse(uuid, plateNumber);
    }

    private UUID extractUuidFromMessage(String message) {
        return UUID.fromString(message.split(":")[0]);
    }

    private String extractPlateNumberFromMessage(String message) {
        return message.split(":")[1];
    }
}
