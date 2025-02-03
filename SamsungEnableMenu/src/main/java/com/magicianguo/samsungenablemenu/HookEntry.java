package com.magicianguo.samsungenablemenu;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;

public class HookEntry extends XposedModule {

    public HookEntry(XposedInterface base, ModuleLoadedParam param) {
        super(base, param);
    }

    @Override
    public void onPackageLoaded(PackageLoadedParam param) {
        super.onPackageLoaded(param);
        if (!param.isFirstPackage()) {
            return;
        }
        if (param.getPackageName().equals("com.android.systemui")) {
            SystemUIHook.init(param, this);
        }
    }

}
