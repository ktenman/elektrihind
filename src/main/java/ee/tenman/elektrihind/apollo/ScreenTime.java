package ee.tenman.elektrihind.apollo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ScreenTime implements Serializable {
	
	private static final long serialVersionUID = -143526534123L;
	private LocalTime time;
	private String url;
	private String hall;

}
