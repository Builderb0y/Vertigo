import org.junit.jupiter.api.Test;

import builderb0y.vertigo.Vertigo;

import static org.junit.jupiter.api.Assertions.*;

public class ReleaseChecks {

	@Test
	public void testReleaseFlags() {
		assertFalse(Vertigo.AUDIT);
		assertFalse(Vertigo.PRINT_EVENTS);
	}
}