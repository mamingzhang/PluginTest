package com.horsege.plugintest;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

public class MainActivity extends AppCompatActivity {

    private static final String PluginPath = "/mnt/sdcard/plugin1.apk";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        copyAssetPlugin();

        String[] pluginInfo = getUninstallApkInfo(this, PluginPath);
        if (pluginInfo != null) {
            dynamicLoadApk(pluginInfo[1]);
        }
    }

    /**
     * 获取未安装apk的信息
     *
     * @param context
     * @param archiveFilePath apk文件的path
     * @return
     */
    private String[] getUninstallApkInfo(Context context, String archiveFilePath) {
        String[] info = new String[2];
        PackageManager pm = context.getPackageManager();
        PackageInfo pkgInfo = pm.getPackageArchiveInfo(archiveFilePath, PackageManager.GET_ACTIVITIES);
        if (pkgInfo != null) {
            ApplicationInfo appInfo = pkgInfo.applicationInfo;
            String appName = pm.getApplicationLabel(appInfo).toString();//app名称
            String pkgName = appInfo.packageName;//包名
            info[0] = appName;
            info[1] = pkgName;
        }
        return info;
    }

    /**
     * @return 得到对应插件的Resource对象
     */
    private Resources getPluginResources() {
        try {
            AssetManager assetManager = AssetManager.class.newInstance();
            Method addAssetPath = assetManager.getClass().getMethod("addAssetPath", String.class);
            addAssetPath.invoke(assetManager, PluginPath);
            Resources superRes = this.getResources();
            Resources mResources = new Resources(assetManager, superRes.getDisplayMetrics(),
                    superRes.getConfiguration());
            return mResources;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 加载apk获得内部资源
     *
     * @param apkPackageName
     */
    private void dynamicLoadApk(String apkPackageName) {
        File optimizedDirectoryFile = getDir("dex", Context.MODE_PRIVATE);
        DexClassLoader dexClassLoader = new DexClassLoader(PluginPath, optimizedDirectoryFile.getPath(), null, ClassLoader.getSystemClassLoader());
        try {
            Class<?> clazz = dexClassLoader.loadClass(apkPackageName + ".R$string");
            Field field = clazz.getDeclaredField("plugin_str");
            int resId = field.getInt(R.id.class);
            Resources mResources = getPluginResources();
            if (mResources != null) {
                ((TextView) findViewById(R.id.txtview)).setText(mResources.getString(resId));
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

    }

    private void copyAssetPlugin() {
        try {
            InputStream inputStream = getAssets().open("plugin1.apk");
            if (inputStream != null) {
                File pluginFile = new File(PluginPath);
                if (pluginFile.exists()) {
                    pluginFile.delete();
                }
                pluginFile.createNewFile();
                FileOutputStream fileOutputStream = new FileOutputStream(pluginFile);

                int length = 0;
                byte[] buffer = new byte[1024];
                while ((length = inputStream.read(buffer)) > 0) {
                    fileOutputStream.write(buffer, 0, length);
                }
                fileOutputStream.flush();
                fileOutputStream.close();
                inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
