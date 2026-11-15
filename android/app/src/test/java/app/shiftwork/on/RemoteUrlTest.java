package app.shiftwork.on;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.net.Uri;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** 화면 자동 업데이트: 앱 안 화면 파일만 웹의 같은 파일로 바뀌는지 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class RemoteUrlTest {
    private static final String B = "https://example.github.io/app/";

    private static String r(String url) {
        return MainActivity.remoteUrl(Uri.parse(url), B);
    }

    @Test
    public void mapsAppFiles() {
        assertEquals(B + "index.html", r("https://appassets.androidplatform.net/assets/web/index.html"));
        assertEquals(B + "sync.js", r("https://appassets.androidplatform.net/assets/web/sync.js"));
        assertEquals(B + "icons/icon-192.png", r("https://appassets.androidplatform.net/assets/web/icons/icon-192.png"));
        assertEquals("text/javascript", MainActivity.mime("sync.js"));
    }

    @Test
    public void leavesOthersAlone() {
        assertNull("다른 사이트", r("https://cdn.jsdelivr.net/npm/x.js"));
        assertNull("앱 화면 폴더 밖", r("https://appassets.androidplatform.net/assets/other/index.html"));
        assertNull("상위 폴더로 나가기", r("https://appassets.androidplatform.net/assets/web/../secret.html"));
        assertNull("모르는 파일 종류", r("https://appassets.androidplatform.net/assets/web/data.bin"));
        assertNull("주소가 비면 끔", MainActivity.remoteUrl(Uri.parse("https://appassets.androidplatform.net/assets/web/index.html"), ""));
    }
}
