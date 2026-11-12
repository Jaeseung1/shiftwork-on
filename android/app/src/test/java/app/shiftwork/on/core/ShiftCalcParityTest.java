package app.shiftwork.on.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Android Studio / ./gradlew test 에서 ShiftCalcTest(웹 앱과 같은 결과인지)를 돌린다. */
public class ShiftCalcParityTest {
    @Test
    public void sameAsWebApp() {
        assertEquals("실패한 시험 수", 0, ShiftCalcTest.run());
    }
}
