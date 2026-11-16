package app.shiftwork.on.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** 알람이 울릴 때(RING)와 끄기·5분 뒤 버튼을 받는다(앱 안에서만, 다른 앱은 보낼 수 없음). */
public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        String a = i.getAction();
        if (a == null) return;
        switch (a) {
            case AlarmScheduler.ACTION_RING:
                boolean test = i.getBooleanExtra(AlarmScheduler.EXTRA_TEST, false);
                if (test || AlarmStore.owner(c)) AlarmRinger.ring(c, i.getStringExtra(AlarmScheduler.EXTRA_SHIFT), test);
                if (!test) AlarmScheduler.reschedule(c); // 다음 근무 알람
                break;
            case AlarmScheduler.ACTION_SNOOZE:
                AlarmRinger.stop(c);
                AlarmScheduler.snooze(c, i.getStringExtra(AlarmScheduler.EXTRA_SHIFT), 5);
                break;
            case AlarmScheduler.ACTION_DISMISS:
                AlarmRinger.stop(c);
                break;
            default:
                break;
        }
    }
}
