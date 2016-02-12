/*
 * Copyright (c) 2016, Apptentive, Inc. All Rights Reserved.
 * Please refer to the LICENSE file for the terms and conditions
 * under which redistribution and use of this file is permitted.
 */

package com.apptentive.android.sdk;

import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;

import com.apptentive.android.sdk.adapter.ApptentiveViewPagerAdapter;
import com.apptentive.android.sdk.model.FragmentFactory;
import com.apptentive.android.sdk.module.ActivityContent;
import com.apptentive.android.sdk.module.engagement.EngagementModule;
import com.apptentive.android.sdk.module.engagement.interaction.fragment.ApptentiveBaseFragment;
import com.apptentive.android.sdk.module.engagement.interaction.model.Interaction;
import com.apptentive.android.sdk.module.metric.MetricModule;
import com.apptentive.android.sdk.util.Util;


public class ApptentiveViewActivity extends AppCompatActivity {

	private ActivityContent.Type fragmentType;

	private boolean activityExtraBoolean;

	private Toolbar toolbar;
	private ViewPager viewPager;

	private ApptentiveViewPagerAdapter viewPager_Adapter;

	private int current_tab;

	protected void onCreate(Bundle savedInstanceState) {
		ApptentiveBaseFragment newFragment = null;
		try {
			Bundle bundle = getIntent().getExtras();
			String fragmentTypeString = bundle.getString(ActivityContent.KEY);

			if (fragmentTypeString != null) {
				fragmentType = ActivityContent.Type.parse(fragmentTypeString);
				if (fragmentType == ActivityContent.Type.ABOUT) {
					// Always apply Apptentive default red theme to Apptentive About, regardless hosting app theme override
					setTheme(R.style.ApptentiveTheme_About);
				} else if (fragmentType == ActivityContent.Type.INTERACTION) {
					bundle.putInt("toolbarLayoutId", R.id.apptentive_toolbar);
					newFragment = FragmentFactory.createFragmentInstance(bundle);
					applyApptentiveTheme(!newFragment.isShownAsModelDialog());
				}

				super.onCreate(savedInstanceState);

				try {
					switch (fragmentType) {
						case ENGAGE_INTERNAL_EVENT:
							String eventName = getIntent().getStringExtra(ActivityContent.EVENT_NAME);
							if (eventName != null) {
								EngagementModule.engageInternal(this, eventName);
							}
							break;
						case ABOUT:
							activityExtraBoolean = getIntent().getBooleanExtra(ActivityContent.EXTRA, true);
							break;
						case MESSAGE_CENTER_ERROR:
							break;
						case INTERACTION:
							break; // end INTERACTION
						default:
							Log.w("No Activity specified. Finishing...");
							break;
					}

					if (newFragment == null) {
						finish();
					}
				} catch (Exception e) {
					Log.e("Error starting ViewActivity.", e);
					MetricModule.sendError(this, e, null, null);
				}
			}
		} catch (Exception e) {
			Log.e("Error creating ViewActivity.", e);
			MetricModule.sendError(this, e, null, null);
		}

		setContentView(R.layout.apptentive_viewactivity);

		toolbar = (Toolbar) findViewById(R.id.apptentive_toolbar);
		setSupportActionBar(toolbar);
		ActionBar actionBar = getSupportActionBar();

		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		//current_tab = extra.getInt(SELECTED_TAB_EXTRA_KEY, 0);
		current_tab = 0;


		addFragmentToAdapter(newFragment, newFragment.getTitle());

		// Get the ViewPager and set it's PagerAdapter so that it can display items
		viewPager = (ViewPager) findViewById(R.id.apptentive_vp);
		viewPager.setAdapter(viewPager_Adapter);


		ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.OnPageChangeListener() {
			Boolean first = true;

			@Override
			public void onPageSelected(int position) {
				ApptentiveBaseFragment currentFragment = (ApptentiveBaseFragment) viewPager_Adapter.getItem(viewPager.getCurrentItem());
				if (!currentFragment.isShownAsModelDialog()) {
					toolbar.setVisibility(View.VISIBLE);
					String title = currentFragment.getTitle();
					if (!TextUtils.isEmpty(title)) {
						toolbar.setTitle(title);
					}
				} else {
					toolbar.setVisibility(View.GONE);
				}
				current_tab = position;
			}

			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
				if (first && positionOffset == 0 && positionOffsetPixels == 0) {
					onPageSelected(current_tab);
					first = false;
				}
			}

			@Override
			public void onPageScrollStateChanged(int pos) {
				// TODO Auto-generated method stub
			}
		};

		viewPager.addOnPageChangeListener(pageChangeListener);
	}

	@Override
	protected void onStart() {
		super.onStart();
		switch (fragmentType) {
			case ABOUT:
				AboutModule.getInstance().setupView(this, activityExtraBoolean);
				break;
			default:
				break;
		}
	}

	@Override
	public void onBackPressed() {
		ApptentiveBaseFragment currentFragment = (ApptentiveBaseFragment) viewPager_Adapter.getItem(viewPager.getCurrentItem());

		if (currentFragment != null && currentFragment.isVisible()) {
			if (currentFragment.onBackPressed()) {
				return;
			}

			FragmentManager childFragmentManager = currentFragment.getChildFragmentManager();

			if (childFragmentManager.getBackStackEntryCount() > 0) {
				childFragmentManager.popBackStack();
			}
		}

		super.onBackPressed();

	}

	@Override
	public void finish() {
		super.finish();
		overridePendingTransition(0, R.anim.slide_down_out);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		overridePendingTransition(R.anim.slide_up_in, 0);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				onBackPressed();
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void applyApptentiveTheme(boolean isFullScreenInteraction) {
		if (ApptentiveInternal.apptentiveTheme != null) {
			getTheme().setTo(ApptentiveInternal.apptentiveTheme);
		}
	}

	private void addFragmentToAdapter(ApptentiveBaseFragment f, String title) {
		if (viewPager_Adapter == null) {
			viewPager_Adapter = new ApptentiveViewPagerAdapter(getSupportFragmentManager());
		}
		viewPager_Adapter.add(f, title);
		viewPager_Adapter.notifyDataSetChanged();
	}

}
