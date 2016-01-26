package bg.nijel.aGrep;

import android.app.Application;

/**
 * Created by Nick on 1/21/2016.
 */
public class MyApp extends Application {

    private static MyApp mMyApp;

    public MyApp getInstance(){
        return mMyApp;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mMyApp = this;
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Runtime.getRuntime().gc();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Runtime.getRuntime().gc();
    }

}
