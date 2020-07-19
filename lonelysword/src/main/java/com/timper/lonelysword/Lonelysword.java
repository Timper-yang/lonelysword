package com.timper.lonelysword;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;

import com.timper.lonelysword.context.App;
import com.timper.lonelysword.dagger.MultiModule;
import com.timper.lonelysword.dagger.Warehouse;
import com.timper.lonelysword.logger.DefaultLogger;
import com.timper.lonelysword.logger.ILogger;
import com.timper.lonelysword.utils.ClassUtils;
import com.timper.lonelysword.utils.PackageUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * User: tangpeng.yang
 * Date: 21/05/2018
 * Description:
 * FIXME
 */
public class Lonelysword {

    public static final String TAG = "Lonelysword";
    public static boolean debug = false;
    public static ILogger logger = new DefaultLogger();

    @VisibleForTesting
    static final Map<Class<?>, Constructor<? extends Unbinder>> BINDINGS = new LinkedHashMap<>();


    public static final String SP_CACHE_KEY = "SP_CACHE_KEY";
    public static final String DAGGER_MULTI_MAP = "DAGGER_MULTI_MAP";
    public static final String LAST_VERSION_NAME = "LAST_VERSION_NAME";
    public static final String LAST_VERSION_CODE = "LAST_VERSION_CODE";
    public static final String LONELYSWORD_ROOT_PAKCAGE = "lonelysword.dagger";
    public static final String DAGGERMULTI = "MultiModule$$";
    private static boolean registerByPlugin;


    private static void loadRouterMap() {
        registerByPlugin = false;
        //使用gradle插件会在这个方法里插入代码,如下：
        // register("xxxxmodulejava");

    }

    /**
     * 注册类名
     * @param className class name
     */
    private static void register(String className) {
        if (!TextUtils.isEmpty(className)) {
            try {
                Class<?> clazz = Class.forName(className);
                Object obj = clazz.getConstructor().newInstance();
                if (obj instanceof MultiModule) {
                    registerDaggerModuleRoot((MultiModule) obj);
                } else {
                    logger.info(TAG, "register failed, class name: " + className
                            + " should implements one of MultiModule.");
                }
            } catch (Exception e) {
                logger.error(TAG,"register class error:" + className);
            }
        }
    }

    private static void registerDaggerModuleRoot(MultiModule multiModule) {
        markRegisteredByPlugin();
        if (multiModule != null) {
            try {
                multiModule.saveAndroidModule(Warehouse.daggerMultiModules, App.context());
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * mark already registered by arouter-auto-register plugin
     */
    private static void markRegisteredByPlugin() {
        if (!registerByPlugin) {
            registerByPlugin = true;
        }
    }

    /**
     * Lonelysword init, 加载所有的module至内存
     */
    public synchronized static void init(Application context) {
        try {
            loadRouterMap();
            if (registerByPlugin) {
                logger.info(TAG, "Load router map by arouter-auto-register plugin.");
                return;
            }

            Set<String> routerMap;
            // It will rebuild router map every times when debuggable.
            if (Lonelysword.debug || PackageUtils.isNewVersion(context)) {
                logger.info(TAG, "Run with debug mode or new install, rebuild router map.");
                // These class was generated by lonelysword-compiler.
                routerMap = ClassUtils.getFileNameByPackageName(context, LONELYSWORD_ROOT_PAKCAGE);
                if (!routerMap.isEmpty()) {
                    context.getSharedPreferences(SP_CACHE_KEY, Context.MODE_PRIVATE).edit().putStringSet(DAGGER_MULTI_MAP, routerMap).apply();
                }
                PackageUtils.updateVersion(context);    // Save new version name when router map update finishes.
            } else {
                logger.info(TAG, "Load dagger multi map from cache.");
                routerMap = new HashSet<>(context.getSharedPreferences(SP_CACHE_KEY, Context.MODE_PRIVATE).getStringSet(DAGGER_MULTI_MAP, new HashSet<String>()));
            }

            for (String className : routerMap) {
                if (className.startsWith(LONELYSWORD_ROOT_PAKCAGE + "." + DAGGERMULTI)) {
                    ((MultiModule) (Class.forName(className).getConstructor().newInstance())).saveAndroidModule(Warehouse.daggerMultiModules, context);
                }
            }

            if (Warehouse.daggerMultiModules.size() == 0) {
                logger.error(TAG, "No daggerMultiModules mapping files were found, check your configuration please!");
            }

            if (Lonelysword.debug) {
                logger.debug(TAG, String.format(Locale.getDefault(), "Lonelysword has already been loaded, daggerMultiModules[%d]", Warehouse.daggerMultiModules.size()));
            }
        } catch (Exception e) {
            throw new RuntimeException(TAG + "Lonelysword init logistics center exception! [" + e.getMessage() + "]");
        }
    }

    /**
     * Control whether debug logging is enabled.
     *
     * @param debug true: debug model,false:release model
     */
    public static void setDebug(boolean debug) {
        Lonelysword.debug = debug;
    }

    @NonNull
    @UiThread
    public static Unbinder bind(@NonNull Object target) {
        return createBinding(target);
    }

    private static Unbinder createBinding(@NonNull Object target) {
        Class<?> targetClass = target.getClass();
        if (debug) Log.d(TAG, "Looking up binding for " + targetClass.getName());
        Constructor<? extends Unbinder> constructor = findBindingConstructorForClass(targetClass);

        if (constructor == null) {
            return Unbinder.EMPTY;
        }

        try {
            return constructor.newInstance(target);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Unable to invoke " + constructor, e);
        } catch (InstantiationException e) {
            throw new RuntimeException("Unable to invoke " + constructor, e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new RuntimeException("Unable to create binding instance.", cause);
        }
    }

    @Nullable
    @CheckResult
    @UiThread
    private static Constructor<? extends Unbinder> findBindingConstructorForClass(Class<?> cls) {
        Constructor<? extends Unbinder> bindingCtor = BINDINGS.get(cls);
        if (bindingCtor != null) {
            if (debug) Log.d(TAG, "HIT: Cached in binding map.");
            return bindingCtor;
        }
        String packageName = cls.getPackage().getName();
        String clsName = cls.getSimpleName();
        if (packageName.startsWith("android.") || packageName.startsWith("java.")) {
            if (debug) Log.d(TAG, "MISS: Reached framework class. Abandoning search.");
            return null;
        }
        try {
            Class<?> bindingClass = cls.getClassLoader().loadClass(packageName + ".LonelySword_" + clsName);
            //noinspection unchecked
            bindingCtor = (Constructor<? extends Unbinder>) bindingClass.getConstructor(cls);
            if (debug) Log.d(TAG, "HIT: Loaded binding class and constructor.");
        } catch (ClassNotFoundException e) {
            if (debug) Log.d(TAG, "Not found. Trying superclass " + cls.getSuperclass().getName());
            bindingCtor = findBindingConstructorForClass(cls.getSuperclass());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Unable to find binding constructor for " + clsName, e);
        }
        BINDINGS.put(cls, bindingCtor);
        return bindingCtor;
    }
}
