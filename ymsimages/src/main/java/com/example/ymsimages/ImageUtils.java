package com.example.ymsimages;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.util.LruCache;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * FileName: ImageUtils
 * Author: king
 * Date: 2019/3/9 9:14
 */
public class ImageUtils {
    /**
     * 图片每次从服务端加载不太好,
     * a.浪费用户流量
     * b.加大服务器的压力
     * c.加载图片耗时,用户体验性差
     *
     * 因此我们把图片缓存到本地 使用三级缓存
     * 最快的是内存
     * 其次是外部存储
     * 最后才是网络存储
     * 首先去内存中加载图片  ->   外部存储中加载图片   ->  网络
     * 不可能把所有图片都缓存到内存中，存储空间不允许这样做。
     * LRU    Least recently used,最近最少使用
     */

    /**
     * 加载图片
     * 这个类只创建一次，单例
     * 怎么样实现单例
     * 2 内部提供一个方法让外部获取实例对象
     *     饿汉式
     *     private static ImageUtil instance = new ImageUtil(null);
     *     懒汉式
     *     枚举方法创建单例
     *     public enum  Sing {
     *     instance;     *
     *     public Bitmap getBitmap(String urlPath) {
     *         return null;
     *     }
     * }
     */

    private static ImageUtils instance;
    private final Handler handler;

    public static ImageUtils getInstance(Context context){
        if (instance == null) {
            synchronized (ImageUtils.class){
                if (instance == null) {
                    instance = new ImageUtils(context);
                }
            }
        }
        return instance;
    }

    private LruCache<String,Bitmap> cache;
    private File cacheDir;
    /**
     * 私有化的构造方法,不等让外部引用
     * @param context
     */
    private ImageUtils(Context context) {
        handler = new Handler();
        //占内存的20%
        int cacheSize = (int) Runtime.getRuntime().freeMemory();
        cacheSize = (int) (cacheSize * 0.2);
        cache = new LruCache<>(cacheSize);

        cacheDir = new File(context.getCacheDir(),"YmsImages");
        //如果目录不存在,创建
        if (!cacheDir.exists()){
            cacheDir.mkdirs();
        }
    }


    @SuppressLint("StaticFieldLeak")
    public void  getBitmap2(final String imageUrl, final ImageLoaderCallback callback){
        new AsyncTask<String, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(String... strings) {
                return getYmsImages(strings[0]);
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                callback.loadBitmapSuccess(bitmap);
            }
        }.execute(imageUrl);
    }

    public interface ImageLoaderCallback {
        void loadBitmapSuccess(Bitmap bitmap);
        //void loadBitmapFail();
    }

    //加载url图片
    public Bitmap getYmsImages(String imageUrl){
        Bitmap bitmap = null;

        //先从内存获取
        bitmap = cache.get(url2Key(imageUrl));
        if (bitmap != null) {
            Log.i("TEST", "从内存中加载图片：" + imageUrl);
            return bitmap;
        }
        
        //从外部存储获取
        bitmap = loadBitmapFromFile(imageUrl);
        if (bitmap != null) {
            Log.i("TEST", "从外部存储中加载图片：" + imageUrl);
            //存入内存
            cache.put(url2Key(imageUrl),bitmap);
            return bitmap;
        }

        //从网络获取

        try {
            URL url = new URL(imageUrl);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(5000);
            int responseCode = urlConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                //获取图片
                bitmap = BitmapFactory.decodeStream(urlConnection.getInputStream());
                //保存到文件
                saveBitmap2File(imageUrl,bitmap);
                //保存到缓存
                cache.put(url2Key(imageUrl),bitmap);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.i("TEST", "从网络中加载图片：" + imageUrl);
        return bitmap;
    }

    /**
     * 将图片保存到文件
     * @param imageUrl
     * @param bitmap
     */
    private void saveBitmap2File(String imageUrl, Bitmap bitmap) throws FileNotFoundException {
        File iamgeFile = new File(cacheDir,url2Key(imageUrl));
        //固定目录
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,
                new FileOutputStream(iamgeFile));
    }

    /**
     * 从文件加载图片
     * @param imageUrl
     * @return
     */
    private Bitmap loadBitmapFromFile(String imageUrl) {
        File imageFile = new File(cacheDir,url2Key(imageUrl));
        if (imageFile.mkdirs()){//判断文件是否存在
            return BitmapFactory.decodeFile(imageFile.getAbsolutePath());
        }
        return null;
    }

    /**
     * 获取文件内存位置
     * @param imageUrl
     * @return
     */
    private String url2Key(String imageUrl) {
        return String.valueOf(imageUrl.hashCode());
    }

}
