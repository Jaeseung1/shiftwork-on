package app.shiftwork.on.alarm;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;

/** 알람 울림 화면을 그림으로 남긴다(app/build/widget-shots/alarm-*.png). 화면이 뜨는지도 확인한다. */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 34)
public class AlarmActivityShotTest {
    private void shot(String name) throws Exception {
        Intent i = new Intent(RuntimeEnvironment.getApplication(), AlarmActivity.class).putExtra(AlarmScheduler.EXTRA_SHIFT, "야");
        AlarmActivity a = Robolectric.buildActivity(AlarmActivity.class, i).setup().get();
        View v = a.getWindow().getDecorView();
        int w = 1080, h = 2340;
        v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        v.layout(0, 0, w, h);
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        v.draw(new Canvas(b));
        File dir = new File("build/widget-shots");
        dir.mkdirs();
        try (FileOutputStream o = new FileOutputStream(new File(dir, name + ".png"))) {
            b.compress(Bitmap.CompressFormat.PNG, 100, o);
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h780dp-xxhdpi")
    public void light() throws Exception {
        shot("alarm-light");
    }

    @Test
    @Config(qualifiers = "w360dp-h780dp-night-xxhdpi")
    public void dark() throws Exception {
        shot("alarm-dark");
    }
}
