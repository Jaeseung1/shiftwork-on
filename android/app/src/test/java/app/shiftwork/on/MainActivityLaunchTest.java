package app.shiftwork.on;

import static org.junit.Assert.assertNotNull;

import androidx.work.Configuration;
import androidx.work.WorkManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

/** 앱이 켜지고(onCreate·onResume) 꺼질 때(onPause·onDestroy) 죽지 않는지 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class MainActivityLaunchTest {
    @Before
    public void work() {
        // 실제 폰에서는 앱이 시작될 때 WorkManager가 저절로 준비된다. Robolectric에서는 직접 준비한다.
        WorkManager.initialize(org.robolectric.RuntimeEnvironment.getApplication(), new Configuration.Builder().setExecutor(Runnable::run).build());
    }

    @Test
    public void launches() throws Exception {
        ActivityController<MainActivity> c = Robolectric.buildActivity(MainActivity.class).setup();
        assertNotNull(c.get());
        Thread.sleep(500);
        ShadowLooper.idleMainLooper();
        c.pause().stop().destroy();
    }
}
