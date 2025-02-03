package com.magicianguo.samsungenablemenu;

import android.annotation.SuppressLint;
import android.view.KeyEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;
import io.github.libxposed.api.annotations.AfterInvocation;
import io.github.libxposed.api.annotations.XposedHooker;

@SuppressLint("PrivateApi")
public class SystemUIHook {
    private static XposedModule xposedModule;
    private static ClassLoader classLoader;

    public static void init(XposedModuleInterface.PackageLoadedParam param, XposedModule module) {
        try {
            xposedModule = module;
            classLoader = param.getClassLoader();

            Class<?> clsNavigationBar = classLoader.loadClass("com.android.systemui.navigationbar.NavigationBar");
            Method prepareNavigationBarViewMethod = clsNavigationBar.getDeclaredMethod("prepareNavigationBarView");
            module.hook(prepareNavigationBarViewMethod, LongClickRecentsHooker.class);
        } catch (Exception e) {
            module.log("SystemUIHook - init error! " + e.getMessage());
        }
    }

    @XposedHooker
    private static class LongClickRecentsHooker implements XposedInterface.Hooker {
        @AfterInvocation
        public static void after(XposedInterface.AfterHookCallback callback) {
            xposedModule.log("SystemUIHook - after: ");
            try {
                Class<?> clsViewController = classLoader.loadClass("com.android.systemui.util.ViewController");
                Class<?> clsNavigationBarView = classLoader.loadClass("com.android.systemui.navigationbar.NavigationBarView");
                Class<?> clsBasicRune = classLoader.loadClass("com.android.systemui.BasicRune");
                Class<?> clsButtonDispatcher = classLoader.loadClass("com.android.systemui.navigationbar.buttons.ButtonDispatcher");

                Object navigationBar = callback.getThisObject();
                Field navbarEnabledField = clsBasicRune.getDeclaredField("NAVBAR_ENABLED");
                navbarEnabledField.setAccessible(true);
                boolean navbarEnabled = (boolean) navbarEnabledField.get(null);
                if (navbarEnabled) {
                    Field mViewField = clsViewController.getDeclaredField("mView");
                    mViewField.setAccessible(true);
                    View navigationBarView = (View) mViewField.get(navigationBar);

                    Method getRecentsButtonMethod = clsNavigationBarView.getDeclaredMethod("getRecentsButton");
                    getRecentsButtonMethod.setAccessible(true);
                    Object buttonDispatcher = getRecentsButtonMethod.invoke(navigationBarView);

                    Method setOnLongClickListenerMethod = clsButtonDispatcher.getDeclaredMethod(
                            "setOnLongClickListener",
                            View.OnLongClickListener.class
                    );
                    setOnLongClickListenerMethod.setAccessible(true);
                    setOnLongClickListenerMethod.invoke(buttonDispatcher, (View.OnLongClickListener) view -> {
                        try {
                            Class<?> clsKeyButtonView = classLoader.loadClass("com.android.systemui.navigationbar.buttons.KeyButtonView");

                            Field mCodeField = clsKeyButtonView.getDeclaredField("mCode");
                            mCodeField.setAccessible(true);
                            int originCode = (int) mCodeField.get(view);
                            mCodeField.set(view, KeyEvent.KEYCODE_MENU);

                            Method sendEventMethod = clsKeyButtonView.getDeclaredMethod("sendEvent", int.class, int.class);
                            sendEventMethod.setAccessible(true);
                            sendEventMethod.invoke(view, 0, 128);
                            view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_LONG_CLICKED);

                            mCodeField.set(view, originCode);
                        } catch (Exception e) {
                            xposedModule.log("SystemUIHook - long click error! e = " + e.getMessage());
                        }
                        return true;
                    });
                }
            } catch (Exception e) {
                xposedModule.log("SystemUIHook - Hook error! " + e.getMessage());
            }
        }
    }
}
