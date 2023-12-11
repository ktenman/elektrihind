package ee.tenman.elektrihind.car.openai;

public class NoOpenAiResponseException extends RuntimeException {
    public NoOpenAiResponseException(String message) {
        super(message);
    }
}
