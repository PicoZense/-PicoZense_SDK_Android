package com.picozense.sample;

import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.picozense.sdk.PsCamera.DeviceStatus;
import com.picozense.sdk.IFrameCallback;
import com.picozense.sdk.IUpgradeStatusCallback;
import com.picozense.sdk.PsCamera;
import com.picozense.sdk.PsCamera.OnPicoCameraConnectListener;
import com.picozense.sdk.PsFrame;

import static com.picozense.sdk.PsFrame.PixelFormat.PixelFormatGray16;
public class MainActivity extends AppCompatActivity {
	private static final boolean DEBUG = true;
	private static final String TAG = "MainActivity";
	private final Object mSync = new Object();
	private PsCamera mPicoCamera;
	private FrameCallback mFrameCallback = null;
	private UpgradeStatusCallback mUpgradeStatusCallback = null;
	private GLSurfaceView mGlSurfaceViewColor;
	private GLSurfaceView mGlSurfaceViewDepth;
	private GLSurfaceView mGlSurfaceViewIr;
	private MyRenderer mRenderDepth;
	private MyRenderer mRenderIr;
	private MyRenderer mRenderColor;
	private Spinner mWorkModeSp = null;
	boolean mIsSpWorkModeFirst = true;
	int mWorkMode = PsCamera.FACE_MODE;
	private CheckBox mDoMirrorCk;
	private CheckBox mTofEnabledCk;
	private CheckBox mRgbAecCk;
	private CheckBox mIR16Ck;
	private Button mUpgradeBtn;
	TextView mTextUpgradeStatus;
	private Button bRestartCamera;
	TextView mTextDeviceInfo;
	Bitmap mBmpDepth = Bitmap.createBitmap(480, 640, Bitmap.Config.ARGB_8888);
	Bitmap mBmpIr = Bitmap.createBitmap(480, 640, Bitmap.Config.ARGB_8888);
	Bitmap mBmpRgb = Bitmap.createBitmap(480, 640, Bitmap.Config.ARGB_8888);
	static final String[] WORKMODE = { "FaceMode","CodeMode","StandbyMode"};
	long startTime = 0;
	long endTime = 0;
	int fps = 0;
	boolean bHasDeviceRegister = false;
	String fwVer = "";
	String sdkVersion = "";

	private static final String[] PERMISSIONS_STORAGE = {
			"android.permission.READ_EXTERNAL_STORAGE",
			"android.permission.WRITE_EXTERNAL_STORAGE"
	};
	private static final int REQUEST_EXTERNAL_STORAGE = 1;
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			String android_version = android.os.Build.VERSION.RELEASE;
			int version = Integer.parseInt(android_version.substring(0, 1));
			if (version >= 6) {
				int permission = this.checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE");
				if (permission != PackageManager.PERMISSION_GRANTED) {
					this.requestPermissions(
							PERMISSIONS_STORAGE,
							REQUEST_EXTERNAL_STORAGE
					);
				}
			}
		}catch(Exception e){
			Log.e(TAG,"android_version is invalid");
		}
		setContentView(R.layout.activity_main);

		mGlSurfaceViewDepth = findViewById(R.id.depthGlView);
		mGlSurfaceViewIr = findViewById(R.id.irGlView);
		mGlSurfaceViewColor = findViewById(R.id.rgbGlView);

		mRenderDepth = new MyRenderer();
		mGlSurfaceViewDepth.setEGLContextClientVersion(2);
		mGlSurfaceViewDepth.setRenderer(mRenderDepth);
		mGlSurfaceViewDepth.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

		mRenderIr = new MyRenderer();
		mGlSurfaceViewIr.setEGLContextClientVersion(2);
		mGlSurfaceViewIr.setRenderer(mRenderIr);
		mGlSurfaceViewIr.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

		mRenderColor = new MyRenderer();
		mGlSurfaceViewColor.setEGLContextClientVersion(2);
		mGlSurfaceViewColor.setRenderer(mRenderColor);
		mGlSurfaceViewColor.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);


		mWorkModeSp = (Spinner) findViewById(R.id.spinner_workMode);
		mWorkModeSp.setEnabled(false);
		ArrayAdapter<String> adapterScanMode=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, WORKMODE);
		mWorkModeSp.setAdapter(adapterScanMode);
		mWorkModeSp.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
									   int position, long id) {
				if (mIsSpWorkModeFirst) {
					mIsSpWorkModeFirst = false;
					return;
				}
				int ret = 0;
				String str = parent.getItemAtPosition(position).toString();
				if (str.equals("FaceMode")) {
					if(bHasDeviceRegister && null != mPicoCamera) {
						ret = mPicoCamera.setWorkMode(PsCamera.FACE_MODE);
						if (ret == 0) {
							mWorkMode = 1;
							mTofEnabledCk.setChecked(true);
							mRgbAecCk.setChecked(true);
							mPicoCamera.setRgbAecEnabled(true);
							mWorkModeSp.setEnabled(true);
							mDoMirrorCk.setEnabled(true);
							mTofEnabledCk.setEnabled(true);
							mRgbAecCk.setEnabled(true);
							mIR16Ck.setEnabled(true);
						} else {
							Log.e(TAG, "set FaceMode failed,try again");
							try{
								Thread.sleep(400);
							}catch(Exception e){

							}
							if (null != mPicoCamera) {
								ret = mPicoCamera.setWorkMode(PsCamera.FACE_MODE);
								if (ret == 0) {
									mWorkMode = 1;
									mTofEnabledCk.setChecked(true);
									mRgbAecCk.setChecked(true);
									mPicoCamera.setRgbAecEnabled(true);
									mWorkModeSp.setEnabled(true);
									mDoMirrorCk.setEnabled(true);
									mTofEnabledCk.setEnabled(true);
									mRgbAecCk.setEnabled(true);
									mIR16Ck.setEnabled(true);
								} else {
									Log.e(TAG, "set FaceMode failed");
								}
							}
						}
					}
				} else if (str.equals("CodeMode")) {
					if(bHasDeviceRegister && null != mPicoCamera) {
						ret = mPicoCamera.setWorkMode(PsCamera.CODE_MODE);
						if (ret == 0) {
							mWorkMode = 2;
							mTofEnabledCk.setChecked(false);
							mRgbAecCk.setChecked(true);
							mPicoCamera.setRgbAecEnabled(true);
							mWorkModeSp.setEnabled(true);
							mDoMirrorCk.setEnabled(true);
							mTofEnabledCk.setEnabled(true);
							mRgbAecCk.setEnabled(true);
							mIR16Ck.setEnabled(false);
						} else {
							Log.e(TAG, "set CodeMode failed, try again");
							try{
								Thread.sleep(400);
							}catch(Exception e){

							}
							if(null != mPicoCamera) {
								ret = mPicoCamera.setWorkMode(PsCamera.CODE_MODE);
								if (ret == 0) {
									mWorkMode = 2;
									mTofEnabledCk.setChecked(false);
									mRgbAecCk.setChecked(true);
									mPicoCamera.setRgbAecEnabled(true);
									mWorkModeSp.setEnabled(true);
									mDoMirrorCk.setEnabled(true);
									mTofEnabledCk.setEnabled(true);
									mRgbAecCk.setEnabled(true);
									mIR16Ck.setEnabled(false);
								} else {
									Log.e(TAG, "set CodeMode failed");
								}
							}
						}
					}
				} else if (str.equals("StandbyMode")) {
					if(bHasDeviceRegister && null != mPicoCamera) {
						ret = mPicoCamera.setWorkMode(PsCamera.STANDBY_MODE);
						if (ret == 0) {
							mWorkMode = 3;
							mWorkModeSp.setEnabled(true);
							mDoMirrorCk.setEnabled(false);
							mTofEnabledCk.setEnabled(false);
							mRgbAecCk.setEnabled(false);
							mIR16Ck.setEnabled(false);
						} else {
							Log.e(TAG, "set StandbyMode failed,try again");
							try{
								Thread.sleep(400);
							}catch(Exception e){

							}
							if(null != mPicoCamera) {
								ret = mPicoCamera.setWorkMode(PsCamera.STANDBY_MODE);
								if (ret == 0) {
									mWorkMode = 3;
									mWorkModeSp.setEnabled(true);
									mDoMirrorCk.setEnabled(false);
									mTofEnabledCk.setEnabled(false);
									mRgbAecCk.setEnabled(false);
									mIR16Ck.setEnabled(false);
								} else {
									Log.e(TAG, "set StandbyMode failed");
								}
							}
						}
					}
				}
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});
		mDoMirrorCk = (CheckBox) findViewById(R.id.do_Mirror);
		mDoMirrorCk.setEnabled(false);
		mDoMirrorCk.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(bHasDeviceRegister && null != mPicoCamera) {
					int ret = 0;
					if (isChecked) {
						ret = mPicoCamera.setImageMirror(1);
					} else {
						ret = mPicoCamera.setImageMirror(0);
					}
					if (ret != 0) {
						Log.e(TAG, "setImageMirror failed");
					}
				}else{
					Log.e(TAG, "device has not connect");
				}
			}
		});
		mTofEnabledCk = (CheckBox) findViewById(R.id.tof_enabled);
		mTofEnabledCk.setEnabled(false);
		mTofEnabledCk.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(bHasDeviceRegister && null != mPicoCamera) {
					mPicoCamera.setTofFrameEnabled(isChecked);
					mIR16Ck.setEnabled(isChecked);
				}else{
					Log.e(TAG, "device has not connect");
				}
			}
		});

		mRgbAecCk = (CheckBox) findViewById(R.id.rgb_aec);
		mRgbAecCk.setEnabled(false);
		mRgbAecCk.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(bHasDeviceRegister && null != mPicoCamera) {
					mPicoCamera.setRgbAecEnabled(isChecked);
				}else{
					Log.e(TAG, "device has not connect");
				}
			}
		});

		mIR16Ck = (CheckBox) findViewById(R.id.ir16_enabled);
		mIR16Ck.setEnabled(false);
		mIR16Ck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(bHasDeviceRegister && null != mPicoCamera) {
					mPicoCamera.setFramePixelFormat(PsFrame.FrameType.IRFrame, (isChecked ? PsFrame.PixelFormat.PixelFormatGray16 : PsFrame.PixelFormat.PixelFormatGray8));
				}else{
					Log.e(TAG, "device has not connect");
				}
			}
		});

		mUpgradeBtn = (Button) findViewById(R.id.upgrade);
		mUpgradeBtn.setEnabled(false);
		mUpgradeBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(mPicoCamera != null){
					mTextUpgradeStatus.setText("");
					String imgFilePath = "/storage/emulated/0/Firmware.img";
					int ret = mPicoCamera.StartUpgradeFirmWare(imgFilePath);
					switch (ret)
					{
						case 0:
//							Toast.makeText(MainActivity.this, "Upgrade start.", Toast.LENGTH_SHORT).show();
							mTextUpgradeStatus.setText("Upgrade start.");
							mUpgradeBtn.setEnabled(false);
							break;
						case 1:
//							Toast.makeText(MainActivity.this, "Upgrade start, but bVer is same.", Toast.LENGTH_SHORT).show();
							mUpgradeBtn.setEnabled(false);
							mTextUpgradeStatus.setText("Upgrade start, but bVer is same.");
							break;
						case -1:
//							Toast.makeText(MainActivity.this, "Do not repeat calls during device upgrade.", Toast.LENGTH_SHORT).show();
							mTextUpgradeStatus.setText("Do not repeat calls during device upgrade.");
							break;
						case -2:
//							Toast.makeText(MainActivity.this, "FirmWare check failed.", Toast.LENGTH_SHORT).show();
							mTextUpgradeStatus.setText("FirmWare check failed.");
							break;
						case -3:
//							Toast.makeText(MainActivity.this, "The current version is too low to support upgrading.", Toast.LENGTH_SHORT).show();
							mTextUpgradeStatus.setText("The current version is too low to support upgrading.");
							break;
						default:
//							Toast.makeText(MainActivity.this, "Upgrade failed. Others errors.", Toast.LENGTH_SHORT).show();
							mTextUpgradeStatus.setText("Upgrade failed. Others errors.");
							break;
					}
				}
			}
		});
		mTextUpgradeStatus = (TextView) findViewById(R.id.upgradeStatus);

		bRestartCamera = (Button) findViewById(R.id.restartCamera);
		bRestartCamera.setEnabled(false);
		bRestartCamera.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(mPicoCamera != null){
					int ret = mPicoCamera.restartCamera();
					if (0 == ret)
					{
						mWorkModeSp.setSelection(2, true);
					}
				}
			}
		});

		mTextDeviceInfo = (TextView) findViewById(R.id.id_DeviceInfo);
        final Button bExit = (Button) findViewById(R.id.Exit);
        bExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                synchronized (mSync) {
                    if (mPicoCamera != null) {
                        mPicoCamera.stop();
                    }
                }
                if (mPicoCamera != null) {
                    mPicoCamera.destroy();
                    mPicoCamera = null;
                }
                finish();
            }
        });

		mPicoCamera = new PsCamera();
		if (mPicoCamera != null) {
			mPicoCamera.init(this,mOnPicoCameraConnectListener);
		}
		mFrameCallback = new FrameCallback();
        mUpgradeStatusCallback = new UpgradeStatusCallback();
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (DEBUG) Log.v(TAG, "onStart:");
		if (mPicoCamera != null) {
			mPicoCamera.setFrameCallback(mFrameCallback);
            mPicoCamera.setUpgradeStatusCallback(mUpgradeStatusCallback);
			mPicoCamera.setWorkMode(mWorkMode);
			mPicoCamera.setTofFrameEnabled(mTofEnabledCk.isChecked());
			mPicoCamera.start(this);
		}
	}

	@Override
	protected void onStop() {
		Log.i(TAG, "onStop:");
		synchronized (mSync) {
			if (mPicoCamera != null) {
				mPicoCamera.stop();
			}
		}
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		if (DEBUG) Log.v(TAG, "onDestroy:");
		if (mPicoCamera != null) {
			mPicoCamera.destroy();
			mPicoCamera = null;
		}

		if(mBmpDepth != null){
			mBmpDepth.recycle();
			mBmpDepth = null;
		}
		if(mBmpIr != null){
			mBmpIr.recycle();
			mBmpIr = null;
		}
		if(mBmpRgb != null){
			mBmpRgb.recycle();
			mBmpRgb = null;
		}

		super.onDestroy();
		System.exit(0);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Log.d(TAG, "onConfigurationChanged");
		super.onConfigurationChanged(newConfig);
	}

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if(msg.what == 0) {
				if (PsCamera.CONNECT_NORMAL == msg.arg1) {
					if (1 == mWorkMode) {
						mTofEnabledCk.setChecked(true);
						mRgbAecCk.setChecked(true);
						mWorkModeSp.setEnabled(true);
						mDoMirrorCk.setEnabled(true);
						mTofEnabledCk.setEnabled(true);
						mRgbAecCk.setEnabled(true);
						mIR16Ck.setEnabled(true);
						bRestartCamera.setEnabled(true);
						if (mPicoCamera != null)
							mPicoCamera.setRgbAecEnabled(true);
					} else if (2 == mWorkMode) {
						mTofEnabledCk.setChecked(false);
						mRgbAecCk.setChecked(true);
						mWorkModeSp.setEnabled(true);
						mDoMirrorCk.setEnabled(true);
						mTofEnabledCk.setEnabled(true);
						mRgbAecCk.setEnabled(true);
						mIR16Ck.setEnabled(false);
						bRestartCamera.setEnabled(true);
						if (mPicoCamera != null)
							mPicoCamera.setRgbAecEnabled(true);
					} else if (3 == mWorkMode) {
						mWorkModeSp.setEnabled(true);
						mDoMirrorCk.setEnabled(false);
						mTofEnabledCk.setEnabled(false);
						mRgbAecCk.setEnabled(false);
						mIR16Ck.setEnabled(false);
					}
					bRestartCamera.setEnabled(true);
					mTextDeviceInfo.setText("fwVer: " + fwVer + "\nsdkVersion: " + sdkVersion);
					mUpgradeBtn.setEnabled(true);
				}
			}else if(msg.what == 1){
				//Toast.makeText(MainActivity.this, " open camera failed ,already opened", Toast.LENGTH_SHORT).show();
			}
			else if(msg.what == 2){
				if(null != mWorkModeSp ) {
					mWorkModeSp.setEnabled(false);
				}
				if(null != mDoMirrorCk ) {
					mDoMirrorCk.setEnabled(false);
				}
				if(null != mTofEnabledCk ) {
					mTofEnabledCk.setEnabled(false);
				}
				if(null != mRgbAecCk ) {
					mRgbAecCk.setEnabled(false);
				}

				if(null != bRestartCamera) {
					bRestartCamera.setEnabled(false);
				}

				if((null != mUpgradeBtn) && (true == mUpgradeBtn.isEnabled())) {
					mUpgradeBtn.setEnabled(false);
				}
				if(null != mIR16Ck ) {
					mIR16Ck.setEnabled(false);
				}
				mTextDeviceInfo.setText("");
			}
		}
	};

	private Handler mHandlerUpgradeStatus = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (0 == msg.what)
			{
				if (msg.obj !=  null){
					mTextUpgradeStatus.setText(msg.obj.toString());
				}
			}
		}
	};
	private final OnPicoCameraConnectListener mOnPicoCameraConnectListener = new OnPicoCameraConnectListener() {
		@Override
		public void onAttach() {
			if (DEBUG) Log.e(TAG, "onAttach:");
		}

		@Override
		public void onConnect(int connectStatus) {
			if (DEBUG) Log.e(TAG, "onConnect connectStatus:"+connectStatus);
			if (PsCamera.CONNECT_NORMAL == connectStatus)
			{
				bHasDeviceRegister = true;
				if(mPicoCamera != null) {
					String sn = mPicoCamera.getSn();
					Log.i(TAG, "SN =  " + sn);
					fwVer = mPicoCamera.getFWVerion();
					Log.i(TAG, "fwVer =  " + fwVer);
					String hwVer = mPicoCamera.getHWVerion();
					Log.i(TAG, "hwVer =  " + hwVer);
					sdkVersion = mPicoCamera.getSDKVerion();
					Log.i(TAG, "SDKVersion  =  " + sdkVersion);
					String deviceName = mPicoCamera.getDeviceName();
					Log.i(TAG, "deviceName  =  " + deviceName);
					String deviceFlashType = mPicoCamera.getFlashType();
					Log.i(TAG, "deviceFlashType  =  " + deviceFlashType);
				}
			}
			Message startMsg = mHandler.obtainMessage();
			startMsg.what = 0;
			startMsg.arg1 = connectStatus;
			mHandler.sendMessage(startMsg);
		}

		@Override
		public void onDisconnect() {
			if (DEBUG) Log.e(TAG, "onDisconnect");
			bHasDeviceRegister = false;
			Message startMsg = mHandler.obtainMessage();
			startMsg.what = 2;
			mHandler.sendMessage(startMsg);
		}

		@Override
		public void onDettach() {
			if (DEBUG) Log.e(TAG, "onDettach");
		}

		@Override
		public void onCancel() {
			if (DEBUG) Log.e(TAG, "onCancel");
		}

		@Override
		public void onError() {
			if (DEBUG) Log.e(TAG, "onError");
			Message startMsg = mHandler.obtainMessage();
			startMsg.what = 1;
			mHandler.sendMessage(startMsg);
		}
	};

	public class UpgradeStatusCallback implements IUpgradeStatusCallback {
		@Override
		public void onUpgradeStatus(int stage, int params){
			Message fwMsg = mHandlerUpgradeStatus.obtainMessage();
			fwMsg.what = 0;
			if (-1 == params){
				fwMsg.obj = "upgrade failed,wait for the device to reboot";
			}else{
				switch (stage)
				{
					case DeviceStatus.DEVICE_PRE_UPGRADE_IMG_COPY:
					{
						Log.e(TAG, "StatusCallback: DEVICE_PRE_UPGRADE_IMG_COPY status:" + params);
						fwMsg.obj = "copy Firmware.img to Camera, reuslt:"+(-1 == params ? "NG" : "OK");
					}
					break;
					case DeviceStatus.DEVICE_UPGRADE_IMG_CHECK_DOING:
					{
						Log.e(TAG, "StatusCallback: DEVICE_UPGRADE_IMG_CHECK_DOING status:" + params);
						fwMsg.obj = "checking Firmware.img";
					}
					break;
					case DeviceStatus.DEVICE_UPGRADE_IMG_CHECK_DONE:
					{
						Log.e(TAG, "StatusCallback: DEVICE_PRE_UPGRADE_IMG_COPY status:" + params);
						fwMsg.obj = "check result:"+(-1 == params ? "NG" : "OK");
					}
					break;
					case DeviceStatus.DEVICE_UPGRADE_UPGRAD_DOING:
					{
						Log.e(TAG, "StatusCallback: DEVICE_UPGRADE_UPGRAD_DOING percent:"+params +"%");
						fwMsg.obj = "upgrade percent:" + params + "%";
					}
					break;
					case DeviceStatus.DEVICE_UPGRADE_RECHECK_DOING:
					{
						Log.e(TAG, "StatusCallback: DEVICE_UPGRADE_RECHECK_DOING"+params);
						if(params < 0){
							fwMsg.obj = "upgrade failed:";
						}else{
							fwMsg.obj = "recheck result:"+(-1 == params ? "NG" : "OK");
						}

					}
					break;
					case DeviceStatus.DEVICE_UPGRADE_RECHECK_DONE:
					{
						Log.e(TAG, "StatusCallback: DEVICE_UPGRADE_RECHECK_DONE:"+params);
						fwMsg.obj = "recheck result:"+(-1 == params ? "NG" : "OK");
					}
					break;
					case DeviceStatus.DEVICE_UPGRADE_UPGRAD_DONE:
					{
						Log.e(TAG, "StatusCallback: DEVICE_UPGRADE_UPGRAD_DONE:" + params);
						fwMsg.obj = "upgrade result:"+(-1 == params ? "NG" : "OK") + ",wait for the device to reboot";
					}
					break;
					default:
						Log.e(TAG, "StatusCallback: other stage:"+stage);
						fwMsg.obj = "upgrade failed,wait for the device to reboot";
						break;
				}
			}
			mHandlerUpgradeStatus.sendMessage(fwMsg);
		}
	}

	public class FrameCallback implements IFrameCallback {
		@Override
		public void onFrame(PsFrame DepthFrame,PsFrame IrFrame,PsFrame RgbFrame) {
			if(null != DepthFrame) {
				int center_L = DepthFrame.frameData.get(DepthFrame.width * DepthFrame.height + DepthFrame.width);
				int center_H = DepthFrame.frameData.get(DepthFrame.width * DepthFrame.height + DepthFrame.width + 1);
				int center = (int)((center_H << 8) | (center_L& 0xFF));
				if (mBmpDepth.getWidth() != DepthFrame.width || mBmpDepth.getHeight() != DepthFrame.height){
					mBmpDepth = Bitmap.createBitmap(DepthFrame.width, DepthFrame.height, Bitmap.Config.ARGB_8888);
				}
				mPicoCamera.Y16ToRgba_bf(DepthFrame.frameData, mBmpDepth,DepthFrame.width,DepthFrame.height, 1500);
				Canvas canvas = new Canvas(mBmpDepth);
				Paint paint = new Paint();
				paint.setAntiAlias(true);
				paint.setDither(true);
				paint.setTextSize(40);
				paint.setColor(Color.parseColor("#ff0000"));
				canvas.drawText(String.valueOf(center), (mBmpDepth.getWidth() / 2) - 20, (mBmpDepth.getHeight() / 2), paint);
				canvas.drawText(".", (mBmpDepth.getWidth() / 2), (mBmpDepth.getHeight() / 2 + 20), paint);
				mRenderDepth.setBuf(mBmpDepth);
				mGlSurfaceViewDepth.requestRender();
			}
			if(null != IrFrame){

				if (mBmpIr.getWidth() != IrFrame.width || mBmpIr.getHeight() != IrFrame.height) {
					mBmpIr = Bitmap.createBitmap(IrFrame.width, IrFrame.height, Bitmap.Config.ARGB_8888);
				}
				if (PixelFormatGray16.ordinal() == IrFrame.pixelFormat){
					mPicoCamera.Y16ToRgba_bf(IrFrame.frameData, mBmpIr,IrFrame.width,IrFrame.height, 3840);
				}
				else{
					mPicoCamera.Y8ToRgba_bf(IrFrame.frameData, mBmpIr,IrFrame.width,IrFrame.height);
				}
				mRenderIr.setBuf(mBmpIr);
				mGlSurfaceViewIr.requestRender();

			}
			if(null != RgbFrame){
				if (mBmpRgb.getWidth() != RgbFrame.width || mBmpRgb.getHeight() != RgbFrame.height) {
					mBmpRgb = Bitmap.createBitmap(RgbFrame.width, RgbFrame.height, Bitmap.Config.ARGB_8888);
				}
				mPicoCamera.RgbToRgba_bf(RgbFrame.frameData,mBmpRgb,RgbFrame.width,RgbFrame.height);
				mRenderColor.setBuf(mBmpRgb);
				mGlSurfaceViewColor.requestRender();
			}
		}
	}
}
