package com.kitsune.makanyuk;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

public class AboutActivity extends Activity
{

	ImageButton githubButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);
		
		githubButton = (ImageButton) findViewById(R.id.githubButton);
		githubButton.setOnClickListener( githubButtonListener );
		
		((MakanYukApplication) getApplication()).getFlurryInstance().pageView();
	}
	
	private OnClickListener githubButtonListener = new OnClickListener() 
	{
		@Override
		public void onClick(View v) 
		{
			((MakanYukApplication) getApplication()).getFlurryInstance().logEvent( "Github Button Click" );
			
			String url = "http://makan-yuk.panjigautama.com";
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse(url));
			startActivity(intent);
		}
	};
	
	@Override
	protected void onStart() 
	{
		super.onStart();
		((MakanYukApplication) getApplication()).getFlurryInstance().startSession( AboutActivity.this );
	}
	
	@Override
	protected void onStop() 
	{
		super.onStop();
		((MakanYukApplication) getApplication()).getFlurryInstance().endSession( AboutActivity.this );
	}

	
}
