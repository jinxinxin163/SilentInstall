package com.example.root.install_silent;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "SilentInstall";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ApplicationManager am;
        try {
            am = new ApplicationManager(this);
            am.setOnPackagedObserver(new OnPackagedObserver(){

                @Override
                public void packageInstalled(String packageName, int returnCode) {
                    Log.d(TAG, packageName + "is installed successfully");
                }

                @Override
                public void packageDeleted(String packageName, int returnCode) {
                    Log.d(TAG, packageName + "is removed successfully");
                }

            });

            am.installPackage("/cache/test.apk"); //apk �ļ���·��
            //am.uninstallPackage(""); //apk �İ���
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
