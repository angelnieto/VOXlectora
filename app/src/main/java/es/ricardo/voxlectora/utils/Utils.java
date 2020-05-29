package es.ricardo.voxlectora.utils;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;

import java.util.List;

public class Utils {

    public static boolean isHomeButtonPressed(Context context){
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
        if (!taskInfo.isEmpty()) {
            ComponentName topActivity = taskInfo.get(0).topActivity;
            if (!topActivity.getPackageName().equals(context.getPackageName()))
                return true;
        }
        return false;
    }
}
