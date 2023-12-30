package ee.tenman.elektrihind.config.rabbitmq;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class RabbitMQConstants {
    public static final String IMAGE_REQUEST_QUEUE = "picture-request-queue";
    public static final String IMAGE_RESPONSE_QUEUE = "picture-response-queue";
    public static final String TEXT_REQUEST_QUEUE = "text-request-queue";
    public static final String TEXT_RESPONSE_QUEUE = "text-response-queue";
    public static final String ONLINE_CHECK_REQUEST_QUEUE = "online_check_request_queue";
    public static final String ONLINE_CHECK_RESPONSE_QUEUE = "online_check_response_queue";
    public static final String CAPTCHA_REQUEST_QUEUE = "captcha-request-queue";
    public static final String CAPTCHA_RESPONSE_QUEUE = "captcha-response-queue";
}
