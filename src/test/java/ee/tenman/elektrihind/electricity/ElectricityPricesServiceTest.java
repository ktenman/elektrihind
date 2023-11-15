package ee.tenman.elektrihind.electricity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ElectricityPricesServiceTest {

    @Mock
    ElectricityPricesClient electricityPricesClient;

    @Mock
    Clock clock;

    @InjectMocks
    ElectricityPricesService electricityPricesService;

    @BeforeEach
    void setUp() {
        lenient().when(clock.instant()).thenReturn(Instant.parse("2023-10-27T10:00:00.00Z"));
        lenient().when(clock.getZone()).thenReturn(ZoneId.systemDefault());
    }
}
