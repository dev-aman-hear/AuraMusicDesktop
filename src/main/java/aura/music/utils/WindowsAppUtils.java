package aura.music.utils;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.WString;

public class WindowsAppUtils {
    private interface Shell32 extends Library {
        Shell32 INSTANCE = Native.load("shell32", Shell32.class);
        int SetCurrentProcessExplicitAppUserModelID(WString appID);
    }
    
    public static void setAppUserModelId(String appId) {
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            try {
                Shell32.INSTANCE.SetCurrentProcessExplicitAppUserModelID(new WString(appId));
            } catch (Throwable t) {
                // Ignore if JNA fails or shell32 is missing
            }
        }
    }
}
