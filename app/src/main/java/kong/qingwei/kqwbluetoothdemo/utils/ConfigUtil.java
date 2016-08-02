package kong.qingwei.kqwbluetoothdemo.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by kqw on 2016/8/2.
 * 配置工具类
 */
public final class ConfigUtil {

    private final SharedPreferences sharedPreferences;
    private final String PAIRING_CONFIRMATION = "pairing_confirmation";

    public ConfigUtil(Context context) {
        sharedPreferences = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
    }

    /**
     * 获取自动配对的配置状态
     *
     * @return 是否自动配对
     */
    public boolean getPairingConfirmation() {
        try {
            return sharedPreferences.getBoolean(PAIRING_CONFIRMATION, false);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 设置是否自动配对
     *
     * @param b 是否自动配对
     */
    public void setPairingConfirmation(boolean b) {
        try {
            sharedPreferences.edit().putBoolean(PAIRING_CONFIRMATION, b).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
