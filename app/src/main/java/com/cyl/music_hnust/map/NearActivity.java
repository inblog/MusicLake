package com.cyl.music_hnust.map;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.nearby.NearbySearch;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.cyl.music_hnust.Json.JsonParsing;
import com.cyl.music_hnust.NearPeopleAcivity;
import com.cyl.music_hnust.R;
import com.cyl.music_hnust.UserCenterMainAcivity;
import com.cyl.music_hnust.application.MyApplication;
import com.cyl.music_hnust.bean.Location;
import com.cyl.music_hnust.bean.User;
import com.cyl.music_hnust.bean.UserStatus;
import com.cyl.music_hnust.http.HttpUtil;
import com.cyl.music_hnust.utils.Constants;
import com.cyl.music_hnust.utils.FormatUtil;
import com.cyl.music_hnust.utils.ToastUtil;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cz.msebera.android.httpclient.Header;

/**
 * Created by 永龙 on 2016/3/20.
 */
public class NearActivity extends AppCompatActivity {
    private ImageButton btn_clear;
    private ImageButton back;
    private RecyclerView location_near;
    private NearbySearch mNearbySearch;
    //声明AMapLocationClient类对象
    public AMapLocationClient mLocationClient = null;
    //声明定位回调监听器
    public AMapLocationListener mLocationListener;
    //声明mLocationOption对象
    public AMapLocationClientOption mLocationOption = null;
    private LatLonPoint latLonPoint;
    public List<Location> mydatas;
    private MyApplication application;
    private RequestQueue mRequestQueue;
    private ImageLoader imageLoader;
    private static ProgressDialog progDialog = null;
    private User user;

    private RecyclerView.LayoutManager mLayoutManager;

    private MyLocationAdapter adapter;
    Handler handler = new Handler() {

        @Override
        public void dispatchMessage(Message msg) {
            super.dispatchMessage(msg);
            switch (msg.what) {
                case 0:
                    dissmissProgressDialog();
                    break;
                case 1:
                    dissmissProgressDialog();
                    Bundle bundle = new Bundle();
                    bundle = msg.getData();
                    String response = (String) bundle.get("response");
                    try {
                        JSONObject json = new JSONObject(response);
                        mydatas = JsonParsing.getLocation(json);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (latLonPoint != null) {
                        adapter.latLonPoint = latLonPoint;
                    } else {
                        latLonPoint = new LatLonPoint(0, 0);
                        ToastUtil.show(getApplicationContext(), "定位异常!");
                        adapter.latLonPoint = latLonPoint;
                    }
                    adapter.myDatas = mydatas;
                    adapter.notifyDataSetChanged();

                    break;
                case 2:
                    finish();
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_near);
        mydatas = new ArrayList<>();
        user = UserStatus.getUserInfo(getApplicationContext());
        application =new MyApplication();
//        mRequestQueue = application.getHttpQueues();
//        imageLoader =application.getImageLoader();

        init();
        showProgressDialog("正在确定你的位置");
        initView();


    }

    private void initfunction() {

        if (latLonPoint != null) {
            volley_StringRequest_GET(user.getUser_id(), latLonPoint.getLatitude()
                    , latLonPoint.getLongitude(), 0);
        } else {
            ToastUtil.show(getApplicationContext(), "定位失败!");
        }
        Handler x = new Handler();
        x.postDelayed(new splashhandler(), 500);

    }


    class splashhandler implements Runnable {

        public void run() {
            showProgressDialog("正在查找附近的人");
            if (user.getUser_id() != null) {
                volley_StringRequest_GET(user.getUser_id(), 0, 0
                        , 1);
            } else {
                ToastUtil.show(getApplicationContext(), "请先登录!");
            }
            dissmissProgressDialog();
        }

    }

    private void initView() {
        btn_clear = (ImageButton) findViewById(R.id.btn_clear);
        back = (ImageButton) findViewById(R.id.backImageButton);
        location_near = (RecyclerView) findViewById(R.id.location_near);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();

            }
        });

        btn_clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                User user = UserStatus.getUserInfo(getApplicationContext());
                if (user.getUser_id() != null) {
                    volley_StringRequest_GET(user.getUser_id(), 0, 0
                            , 2);
                } else {
                    ToastUtil.show(getApplicationContext(), "请先登录!");
                }

            }
        });


        mLayoutManager = new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.VERTICAL, false);


        adapter = new MyLocationAdapter(getApplicationContext(), mydatas, imageLoader);
        location_near.setAdapter(adapter);
        location_near.setLayoutManager(mLayoutManager);

    }


    private void init() {
        mNearbySearch = NearbySearch.getInstance(getApplicationContext());

        mLocationListener = new AMapLocationListener() {
            @Override
            public void onLocationChanged(AMapLocation amapLocation) {
                if (amapLocation != null) {
                    if (amapLocation.getErrorCode() == 0) {
                        //定位成功回调信息，设置相关消息
                        amapLocation.getLocationType();//获取当前定位结果来源，如网络定位结果，详见定位类型表
                        amapLocation.getLatitude();//获取纬度
                        amapLocation.getLongitude();//获取经度
                        amapLocation.getAccuracy();//获取精度信息
                        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        Date date = new Date(amapLocation.getTime());
                        df.format(date);//定位时间
                        amapLocation.getAddress();//地址，如果option中设置isNeedAddress为false，则没有此结果，网络定位结果中会有地址信息，GPS定位不返回地址信息。
                        amapLocation.getCountry();//国家信息
                        amapLocation.getProvince();//省信息
                        amapLocation.getCity();//城市信息
                        amapLocation.getDistrict();//城区信息
                        amapLocation.getStreet();//街道信息
                        amapLocation.getStreetNum();//街道门牌号信息
                        amapLocation.getCityCode();//城市编码
                        amapLocation.getAdCode();//地区编码
                        amapLocation.getAoiName();//获取当前定位点的AOI信息

                        latLonPoint = new LatLonPoint(amapLocation.getLongitude(), amapLocation.getLatitude());
                        Log.e("amapLocation", "Location amapLocation, getLongitude:"
                                + amapLocation.getLongitude() + ", getLatitude:"
                                +  amapLocation.getLatitude());
                        initfunction();
                    } else {
                        dissmissProgressDialog();
                        initfunction();
                        //显示错误信息ErrCode是错误码，errInfo是错误信息，详见错误码表。
                        Log.e("AmapError", "Location Error, ErrCode:"
                                + amapLocation.getErrorCode() + ", errInfo:"
                                + amapLocation.getErrorInfo());
                    }
                }

            }
        };

        mLocationClient = new AMapLocationClient(getApplicationContext());
        mLocationClient.setLocationListener(mLocationListener);
        //初始化定位参数
        mLocationOption = new AMapLocationClientOption();
//设置定位模式为高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
//设置是否返回地址信息（默认返回地址信息）
        mLocationOption.setNeedAddress(true);
//设置是否只定位一次,默认为false
        mLocationOption.setOnceLocation(true);
//设置是否强制刷新WIFI，默认为强制刷新
        mLocationOption.setWifiActiveScan(true);
//设置是否允许模拟位置,默认为false，不允许模拟位置
        mLocationOption.setMockEnable(false);
//设置定位间隔,单位毫秒,默认为2000ms
        mLocationOption.setInterval(10000);
//给定位客户端对象设置定位参数
        mLocationClient.setLocationOption(mLocationOption);
//启动定位
        mLocationClient.startLocation();

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocationClient.onDestroy();//销毁定位客户端。
        //调用销毁功能，在应用的合适生命周期需要销毁附近功能
    }


    //适配器
    public class MyLocationAdapter extends RecyclerView.Adapter<MyLocationAdapter.MyLocationViewHolder> {


        public Context mContext;
        public List<Location> myDatas = new ArrayList<>();
        public LayoutInflater mLayoutInflater;
        public ImageLoader imageLoader;
        public LatLonPoint latLonPoint;

        public MyLocationAdapter(Context mContext, List<Location> myDatas, ImageLoader imageLoader) {
            this.mContext = mContext;
            mLayoutInflater = LayoutInflater.from(mContext);
            this.myDatas = myDatas;
            this.imageLoader = imageLoader;
        }

        @Override
        public MyLocationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View mView = mLayoutInflater.inflate(R.layout.item_near, parent, false);
            MyLocationViewHolder mViewHolder = new MyLocationViewHolder(mView);
            return mViewHolder;
        }

        @Override
        public void onBindViewHolder(final MyLocationViewHolder holder, final int position) {

            holder.user_name.setText(myDatas.get(position).getUser().getNick());
            String distance = "";
            String distime = "";

            holder.id_cardview_foot.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent;
                    User user = UserStatus.getUserInfo(getApplicationContext());
                    if (myDatas.get(position).getUser().getUser_id().equals(user.getUser_id())) {
                        intent = new Intent(getApplicationContext(), UserCenterMainAcivity.class);
                    } else {
                        intent = new Intent(getApplicationContext(), NearPeopleAcivity.class);
                    }
                    if (myDatas.get(position).getUser().isSecret()) {
                        //保密
                        intent.putExtra("flag", 1);

                        intent.putExtra("nick", mydatas.get(position).getUser().getNick());
                    } else {
                        intent.putExtra("flag", 0);

                        intent.putExtra("name", mydatas.get(position).getUser().getUser_name());
                        intent.putExtra("num", mydatas.get(position).getUser().getUser_id());
                        intent.putExtra("class", mydatas.get(position).getUser().getUser_class());
                        intent.putExtra("college", mydatas.get(position).getUser().getUser_college());
                        intent.putExtra("major", mydatas.get(position).getUser().getUser_major());
                        intent.putExtra("img", mydatas.get(position).getUser().getUser_img());


                        intent.putExtra("sex", mydatas.get(position).getUser().getUser_sex());
                        intent.putExtra("phone", mydatas.get(position).getUser().getPhone());
                        intent.putExtra("email", mydatas.get(position).getUser().getUser_email());
                        intent.putExtra("nick", mydatas.get(position).getUser().getNick());


                    }
                    startActivity(intent);
                }
            });
            holder.location_time.setText(distime);
            holder.user_distance.setText("不超过" + distance);
            //  holder1.user_logo.setDefaultImageResId(R.mipmap.user_icon_default_main);
            holder.user_img.setErrorImageResId(R.drawable.ic_account_circle_black_24dp);
            holder.user_img.setImageUrl(myDatas.get(position).getUser().getUser_img(), imageLoader);

        }

        @Override
        public int getItemCount() {
            return myDatas.size();
        }

        public class MyLocationViewHolder extends RecyclerView.ViewHolder {
            public NetworkImageView user_img;
            public TextView user_name;
            public TextView user_signature;
            public TextView user_distance;
            public TextView location_time;
            public CardView id_cardview_foot;

            public MyLocationViewHolder(View itemView) {
                super(itemView);
                id_cardview_foot = (CardView) itemView.findViewById(R.id.id_cardview_foot);
                user_name = (TextView) itemView.findViewById(R.id.user_name);
                user_img = (NetworkImageView) itemView.findViewById(R.id.user_img);
                user_signature = (TextView) itemView.findViewById(R.id.user_signature);
                user_distance = (TextView) itemView.findViewById(R.id.user_distance);
                location_time = (TextView) itemView.findViewById(R.id.location_time);
            }
        }
    }


    /**
     * 利用StringRequest实现Get请求
     */
    private void volley_StringRequest_GET(String user_id, double latitude, double longitude, final int requestcode) {
        String url = "";
        if (requestcode == 0) {
            //上传位置
            url = "http://119.29.27.116/hcyl/music_BBS/operate.php?user_id=" + user_id +
                    "&&newLocation&location_latitude=" + latitude +
                    "&location_longitude=" + longitude +
                    "&secretTime=" + FormatUtil.getTime();

        } else if (requestcode == 1) {
            //附近的人
            url = Constants.DEFAULT_URL+"user_id=" + user_id + "&nearLocation";
        } else if (requestcode == 2) {
            //清空用户定位信息
            url = Constants.DEFAULT_URL+"user_id=" + user_id + "&clearLocation";
        }

        HttpUtil.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

                try {
                    String response = new String(responseBody, "utf-8");
                    Log.i("log", response);

                    Message message = new Message();
                    message.what = requestcode;
                    if (requestcode == 1) {

                        //ToastUtil.show(getApplicationContext(),response.toString()+"");

                        Bundle bundle = new Bundle();
                        bundle.putString("response", response);
                        message.setData(bundle);

                    }
                    handler.sendMessage(message);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                dissmissProgressDialog();
                ToastUtil.show(getApplicationContext(), "网络异常，请检查网络！");
            }
        });

    }

    /**
     * 显示进度框
     */
    private void showProgressDialog(String msg) {
        progDialog = new ProgressDialog(NearActivity.this);
        progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progDialog.setIndeterminate(false);
        progDialog.setCancelable(true);
        progDialog.setMessage(msg);
        progDialog.show();

    }

    /**
     * 隐藏进度框
     */
    private static void dissmissProgressDialog() {
        if (progDialog != null) {
            progDialog.dismiss();
        }
    }


}
