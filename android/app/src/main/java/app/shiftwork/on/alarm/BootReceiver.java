package app.shiftwork.on.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** 폰을 켠 뒤, 앱이 업데이트된 뒤, 시간·시간대가 바뀐 뒤 다음 근무 알람을 다시 맞춘다(시스템이 보내는 방송만 받음). */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        AlarmScheduler.reschedule(c);
    }
}
