package com.kitsune.makanyuk;

import static com.nineoldandroids.view.ViewPropertyAnimator.animate;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.kitsune.foursquarehelper.FoursquareHelper;
import com.kitsune.foursquarehelper.FoursquareHelper.OnRequestVenueListener;
import com.kitsune.foursquarehelper.Venue;
import com.kitsune.thirdlib.LocationHelper;
import com.kitsune.thirdlib.LocationHelper.OnGetLocationListener;
import com.kitsune.thirdlib.ShakeEventListener;
import com.kitsune.thirdlib.ShakeEventListener.OnShakeListener;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.Animator.AnimatorListener;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.view.ViewHelper;

public class MainActivity extends Activity 
{
	// location
	private LocationHelper mLocHelper;
	private Location mCurrentLocation;
	
	// shaky shaky..
	private SensorManager mSensorManager;
	private ShakeEventListener mSensorListener;
	private FoursquareHelper mFoursquareHelper;
	private Vibrator mVibrator; 
	
	// components
	ImageView mImageStage;
	TextView mLocationText;
	TextView mTextStage;
	TextView mVenueTitleText;
	LinearLayout mButtonGroup;
	
	// venue
	Venue mFoundedVenue;

	int[] mImages = { R.drawable.ic_bubble, R.drawable.ic_cup, R.drawable.ic_fire, R.drawable.ic_food, R.drawable.ic_heart, R.drawable.ic_money,  R.drawable.ic_paperplane };
	int counter = 0;
	
	// flag
	private boolean isInSearchVenue = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		setContentView( R.layout.fragment_search );
		
		// main components
		mImageStage = (ImageView) findViewById(R.id.mainStageImage);
		mTextStage = (TextView) findViewById(R.id.mainViewText);
		mButtonGroup = (LinearLayout) findViewById(R.id.buttonGroup);
		mButtonGroup.setVisibility(LinearLayout.INVISIBLE);
		
		mLocationText = (TextView) findViewById(R.id.mainLocationText);
		mLocationText.setText( "mencari lokasi mu ..." );
		
		mVenueTitleText = (TextView) findViewById(R.id.mainVenueTitleText);
		mVenueTitleText.setVisibility( TextView.INVISIBLE );

		// location helper
		mLocHelper = new LocationHelper( this );
		mLocHelper.setOnGetLocationListener(getLocationListener);
		
		// sensor helper
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mSensorListener = new ShakeEventListener();
		mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		
		// four square
		mFoursquareHelper = new FoursquareHelper(this, "L1USFKH2MOHXVAE5BE4A4SWE0OXGVN3E1KRJKUI5UDYMCHLT", "01QZXTU0FL5UDX1KDPNPON0CUES3W3I00J2K2KOIU1OK3ZTL");
		mFoursquareHelper.setOnRequestVenueListener( requestVenueListener );

		// listen to shake from user
		mSensorListener.setOnShakeListener(shakeListener);
		
		ImageButton share = (ImageButton) findViewById(R.id.shareButton);
		share.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v)
			{
				Intent emailIntent = new Intent();
				emailIntent.setAction(Intent.ACTION_SEND);
				
				// Native email client doesn't currently support HTML, but it doesn't hurt to try in case they fix it
				emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Ayo makan disini !");
				String shareMessage = "makan di " + mFoundedVenue.getName() + " yuk ! " + mFoundedVenue.getCanonicalUrl();
				emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareMessage);
				emailIntent.setType("message/rfc822");
				
				PackageManager pm = getPackageManager();
				Intent sendIntent = new Intent(Intent.ACTION_SEND);     
				sendIntent.setType("text/plain");
				
				Intent openInChooser = Intent.createChooser(emailIntent, "Share Venue");
				
				List<ResolveInfo> resInfo = pm.queryIntentActivities(sendIntent, 0);
				List<LabeledIntent> intentList = new ArrayList<LabeledIntent>();        
				for (int i = 0; i < resInfo.size(); i++) 
				{
				    // Extract the label, append it, and repackage it in a LabeledIntent
					ResolveInfo ri = resInfo.get(i);
					String packageName = ri.activityInfo.packageName;
					if(packageName.contains("android.email")) 
					{
					    emailIntent.setPackage(packageName);
					} 
					else if(packageName.contains("twitter") || packageName.contains("facebook") || packageName.contains("mms") || packageName.contains("android.gm") || packageName.contains("whatsapp") || packageName.contains("line") || packageName.contains("kakao") || packageName.contains("wechat") ) 
					{
					    Intent intent = new Intent();
					    intent.setComponent(new ComponentName(packageName, ri.activityInfo.name));
					    intent.setAction(Intent.ACTION_SEND);
					    
					    intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Ayo makan disini !");
						intent.putExtra(android.content.Intent.EXTRA_TEXT, shareMessage);
						
					    if(packageName.contains("android.gm")) 
					    {
			                intent.setType("message/rfc822");
			            }
					    else
					    {
					    	intent.setType("text/plain");
					    }
					    intentList.add(new LabeledIntent(intent, packageName, ri.loadLabel(pm), ri.icon));
					}
				}
				
				// convert intentList to array
				LabeledIntent[] extraIntents = intentList.toArray( new LabeledIntent[ intentList.size() ]);
				
				openInChooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents);
				startActivity(openInChooser);

			}
		});
		
		ImageButton location = (ImageButton) findViewById(R.id.locationButton);
		location.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) 
			{
				String start_address 		= "saddr=" + mCurrentLocation.getLatitude() + "," + mCurrentLocation.getLongitude();
				String destination_address 	= "daddr=" + mFoundedVenue.getLocationLatitude() + "," + mFoundedVenue.getLocationLongitude();
				String targetUri 			= "http://maps.google.com/maps?" + start_address + "&" + destination_address;
				
				Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(targetUri));
				intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
				startActivity(intent);
			}
		});
		
	}
	
	// shake listener
	private OnShakeListener shakeListener = new ShakeEventListener.OnShakeListener() 
	{
		
		@Override
		public void onShake() 
		{
			// lets give feedback to user			
			if( (mCurrentLocation != null) && ( !isInSearchVenue ) )
			{
				// flag UI
				mTextStage.setText( getResources().getString(R.string.find_location) );
				mTextStage.setVisibility( TextView.VISIBLE );
				
				mVenueTitleText.setVisibility( TextView.INVISIBLE );
				mButtonGroup.setVisibility( LinearLayout.INVISIBLE );

				// set flag
				isInSearchVenue = true;
				
				animate(mImageStage).alpha(0f).setListener(new ImageStageAnimator(mImageStage));
				mFoursquareHelper.requestNearestVenue( Double.toString(mCurrentLocation.getLatitude()), Double.toString(mCurrentLocation.getLongitude()), "4d4b7105d754a06374d81259", null, null, null);
				mVibrator.vibrate(500);
			}
			else
			{
				Toast.makeText(MainActivity.this, "Sedang mencari lokasi mu..", Toast.LENGTH_SHORT).show();
			}
			
		}
		
	};
	
	void updateLocation( Location location, List<Address> addresses )
	{
		mCurrentLocation = location;
		if( addresses.size() > 0 )
		{
			Address addr = addresses.get(0);
			mLocationText.setText( "kamu lagi ada di sekitar " + addr.getLocality() );
		}
	}
	
	// location listener
	private OnGetLocationListener getLocationListener = new OnGetLocationListener() 
	{
		
		@Override
		public void onLocationUpdate(Location location) 
		{
			if( location != null )
			{
				List<Address> addresses = mLocHelper.getCurrentAddress();
				updateLocation( location, addresses );
			}
			
			Toast.makeText( MainActivity.this, "latest location : "+location.getLatitude()+","+location.getLongitude(), Toast.LENGTH_SHORT ).show();
		}
	};
	
	// foursquare helper listener
	private OnRequestVenueListener requestVenueListener = new OnRequestVenueListener() 
	{
		
		@Override
		public void onFetchSuccess(List<Venue> venues) 
		{
			Toast.makeText( MainActivity.this, "venues : "+venues.size(), Toast.LENGTH_LONG).show();
			
			if( isInSearchVenue )
			{
				animate(mImageStage).cancel();
				isInSearchVenue = false;
				
				Random rand = new Random(); 
				int position = rand.nextInt( venues.size() ); 

				mFoundedVenue = venues.get( position );
				showUpVenue();
			}
		}
		
		@Override
		public void onFetchFailed(String response) 
		{
			Toast.makeText( MainActivity.this, response, Toast.LENGTH_LONG).show();
			if( isInSearchVenue )
			{
				animate(mImageStage).cancel();
				isInSearchVenue = false;
			}
		}
	};

	@Override
	protected void onResume() 
	{
		super.onResume();
		mSensorManager.registerListener( mSensorListener,
		        mSensorManager.getDefaultSensor( Sensor.TYPE_ACCELEROMETER ),
		        SensorManager.SENSOR_DELAY_UI);
		
		// update location
		if( mLocHelper!= null )
		{
			mLocHelper.requestUpdateLocation();
		}
	}

	@Override
	protected void onPause() 
	{
		// remove location = listener
		if( mLocHelper!= null )
		{
			mLocHelper.removeUpdateLocation();
		}
		
		// remove sensor listener
		if( mSensorManager != null )
		{
			mSensorManager.unregisterListener( mSensorListener );
		}
		super.onPause();
	}

	void showUpVenue()
	{
		mTextStage.setVisibility( TextView.INVISIBLE );
		mVenueTitleText.setText( mFoundedVenue.getName() );
		mVenueTitleText.setVisibility( TextView.VISIBLE );
		mButtonGroup.setVisibility(LinearLayout.VISIBLE);
		
		ViewHelper.setAlpha( mVenueTitleText, 0f );
		ViewHelper.setAlpha( mImageStage, 0f );
		ViewHelper.setAlpha( mButtonGroup, 0f );
		
		mImageStage.setImageDrawable( getResources().getDrawable( R.drawable.ic_light ) );
		
		animate(mVenueTitleText).alpha(1f).setDuration(500).setListener(null);
		animate(mImageStage).alpha(1f).setDuration(500).setListener(null);
		animate(mButtonGroup).alpha(1f).setDuration(500).setListener(null);

	}

	private class ImageStageAnimator extends AnimatorListenerAdapter implements AnimatorListener
	{
		
		private float alpha = 1;
		private ImageView imageStage;
		private boolean onCancel = false;

		public ImageStageAnimator( ImageView imageStage )
		{
			this.imageStage = mImageStage;
		}
		
		@Override
		public void onAnimationStart(Animator animation) 
		{
			onCancel = false;
		}

		@Override
		public void onAnimationEnd(Animator animation) 
		{
			if( alpha == 0 && !onCancel )
			{ 
				alpha = 1; 
				imageStage.setImageDrawable( getResources().getDrawable( mImages[counter]) );
				counter++;
				if( counter == mImages.length )
				{
					counter = 0;
				}
			}
			else
			{ 
				alpha = 0; 
			};
			
			if( onCancel )
			{
				imageStage.setImageDrawable( getResources().getDrawable( R.drawable.ic_search ) );
				ViewHelper.setAlpha( imageStage, 1f );
			}
			else
			{
				animate(mImageStage).alpha(alpha).setDuration(500);
			}
			
		}

		@Override
		public void onAnimationCancel(Animator animation) 
		{
			onCancel = true;
		}
		
	}
	
}
