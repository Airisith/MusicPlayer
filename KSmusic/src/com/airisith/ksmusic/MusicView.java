package com.airisith.ksmusic;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

import com.airisith.modle.MusicInfo;
import com.airisith.util.Constans;
import com.airisith.util.MusicList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class MusicView extends Activity{
	private final String TAG = "MusicView";

	private int musicPosition = 0;
	private int playState = Constans.STATE_STOP;
	private int playMode = Constans.MODLE_ORDER;
	private List<MusicInfo> localMusicLists = null;
	private List<MusicInfo> currentMusicList = null;
	private List<MusicInfo> downloadMusicLists = null;
	
	private Intent musicIntent = null;
	private MusicInfo currentMusicInfo = null;
	private boolean turnTOback = false;
	private Handler timeHandler; // 实时更新歌曲时间
	private Timer timer;
	private MusicCompleteReceiver receiver;// 循环播放广播接收器
	
	private RelativeLayout topBackLayout;
	private TextView likeTextView;
	private TextView menuTextView;
	private TextView titelTextView;
	private TextView artisTextView;
	private TextView timeTextView;
	private ImageView orderImageView;
	private ImageView lastImageView;
	private ImageView playImageView;
	private ImageView nextImageView;
	private ImageView listImageView;
	private SeekBar seekBar;

	private int currentListId = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_music);
		topBackLayout = (RelativeLayout) findViewById(R.id.musicTop_backLayout);
		likeTextView = (TextView)findViewById(R.id.music_like);
		menuTextView = (TextView)findViewById(R.id.music_menu);
		titelTextView = (TextView)findViewById(R.id.musicTop_title);
		artisTextView = (TextView)findViewById(R.id.musicTop_artist);
		timeTextView = (TextView)findViewById(R.id.music_timeText);
		orderImageView = (ImageView)findViewById(R.id.music_order);
		lastImageView = (ImageView)findViewById(R.id.music_last);
		playImageView = (ImageView)findViewById(R.id.music_play);
		nextImageView = (ImageView)findViewById(R.id.music_next);
		listImageView = (ImageView)findViewById(R.id.music_list);
		seekBar = (SeekBar)findViewById(R.id.music_progressbar);
		
		musicIntent = new Intent(getApplicationContext(), MusicService.class);
		musicIntent.putExtra("Activity", "MusicView");
		try {
			playState = getIntent().getIntExtra("SERVICE_STATE", Constans.STATE_STOP);
		} catch (Exception e) {
		}
		
		
		// 获取音乐列表
		localMusicLists = MusicList.getMusicInfos(getApplicationContext());
		downloadMusicLists = new ArrayList<MusicInfo>();
		if (0 == currentListId) {
			currentMusicList = localMusicLists;
		} else {
			currentMusicList = downloadMusicLists;
		}
		
		// 广播接收器，用于一首歌播放完成后继续播放下一首的动作
		receiver = new MusicCompleteReceiver();
		IntentFilter intentfFilter = new IntentFilter();
		intentfFilter.addAction(Constans.MUSIC_END_ACTION_MUSIC);
		MusicView.this.registerReceiver(receiver, intentfFilter);
		
		// 各个View设置监听器
		topBackLayout.setOnClickListener(new OnbuttomItemClickeListener());
		likeTextView.setOnClickListener(new OnbuttomItemClickeListener());
		menuTextView.setOnClickListener(new OnbuttomItemClickeListener());
		orderImageView.setOnClickListener(new OnbuttomItemClickeListener());
		lastImageView.setOnClickListener(new OnbuttomItemClickeListener());
		playImageView.setOnClickListener(new OnbuttomItemClickeListener());
		nextImageView.setOnClickListener(new OnbuttomItemClickeListener());
		listImageView.setOnClickListener(new OnbuttomItemClickeListener());
		// 进度条拖拽
		seekBar.setOnSeekBarChangeListener(new OnProgressChagedListener());
	}

	@Override
	protected void onDestroy() {
		Log.w(TAG, "onDestroy");
		super.onDestroy();
		try {
			MusicService.updataTime(timeHandler, timer, false);
			// 停止接收广播
			MusicView.this.unregisterReceiver(receiver);
			stopService(musicIntent);
		} catch (Exception e) {
		}
	}

	@Override
	protected void onPause() {
		Log.w(TAG, "onPause");
		turnTOback = true;
		super.onPause();
	}

	@Override
	protected void onStart() {
		Log.w(TAG, "onStart");
		super.onStart();
		turnTOback = false;
		musicIntent.putExtra("Activity", "MusicView");
		try {
			int[] state = MusicInfo.getCurrentMusicInfo(getApplicationContext());
			currentListId = state[0];
			playMode = state[1];
			musicPosition = state[2];
		} catch (Exception e) {
		}
		try {
			// 获取歌曲播放信息
			int[] state = MusicInfo
					.getCurrentMusicInfo(getApplicationContext());
			if (0 == state[0]) {
				currentMusicList = localMusicLists;
				currentListId = 0;
			} else if (1 == state[0]) {
				currentMusicList = downloadMusicLists;
				currentListId = 1;
			}
			playMode = state[1];
			musicPosition = state[2];
		} catch (Exception e) {
		}
		Log.w(TAG, "列表:" + currentMusicList.size() + "循环模式:" + playMode
				+ "歌曲位置:" + musicPosition);
		try {
			currentMusicInfo = currentMusicList.get(musicPosition);
			Log.w(TAG, "歌曲:" + currentMusicInfo.getAbbrTitle());
		} catch (Exception e) {
		}
		try {
			Log.w(TAG, "是否正在播放" + playState);
			// 更新时间，接收由MusicService中的子线程发送的消息
			timer = new Timer(true);
			timeHandler = new UpdateInfoHandler();
			titelTextView.setText(currentMusicInfo.getAbbrTitle());
			artisTextView.setText(currentMusicInfo.getArtist());
			if (Constans.STATE_STOP == playState) {
				playImageView.setImageResource(R.drawable.play);
				timeTextView.setText("00:00/"+currentMusicInfo.getDurationStr());
			}else if (Constans.STATE_PUASE == playState) {
				playImageView.setImageResource(R.drawable.play);
				MusicService.updataTime(timeHandler, timer, true);
			}
			else {
				MusicService.updataTime(timeHandler, timer, true);
				playImageView.setImageResource(R.drawable.puase);
				//bcap.setImageBitmap(currentMusicInfo.getAlbum_bitmap());
				titelTextView.setText(currentMusicInfo.getAbbrTitle());
				artisTextView.setText(currentMusicInfo.getAbbrArtist());
			} 

		} catch (Exception e) {
		}
		try {
			IntentFilter intentfFilter = new IntentFilter();
			intentfFilter.addAction(Constans.MUSIC_END_ACTION_MUSIC);
			MusicView.this.registerReceiver(receiver, intentfFilter);
		} catch (Exception e) {
		}
	}

	@Override
	protected void onStop() {
		Log.w(TAG, "onStop");
		turnTOback = true;
		try {
			MusicService.updataTime(timeHandler, timer, false);
		} catch (Exception e) {
		}
		super.onStop();
	}
	
//	/**
//	 * 返回键设置
//	 */
//	@Override
//    public void onBackPressed() { 
//        Log.w(TAG, "onBackPressed") ;
//        Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
//		intent.putExtra("SERVICE_STATE", playState);
//		startActivity(intent);
//    } 
	/**
	 * 歌曲命令
	 * 
	 * @param musicInfos
	 *            歌曲列表信息
	 * @param playCommand
	 *            播放命令：play，puase，stop
	 * @param position
	 *            歌曲位于列表中的位置
	 * @param rate
	 *            播放的位置，整个歌曲时间定为100, 如果为负数，则表示继续从当前位置播放
	 * @param upTime
	 *            是否更新时间
	 */
	private void MusicCommad(List<MusicInfo> musicInfos, int playCommand,
			int position, int rate, Boolean upTime) {
		if ((musicInfos != null)&&(Constans.ACTIVITY_CHANGED_CMD != playCommand)) {
			MusicInfo musicInfo = musicInfos.get(position);
			currentMusicInfo  = musicInfo;
			Log.w(TAG, "开始播放第" + position + "首歌");
			Log.w(TAG, musicInfo.getUrl().toString());
			// Intent intent = new Intent();
			musicIntent.putExtra("url", musicInfo.getUrl());
			musicIntent.putExtra("CMD", playCommand);
			musicIntent.putExtra("rate", rate);
			startService(musicIntent); // 启动服务
			titelTextView.setText(musicInfo.getAbbrTitle());
			artisTextView.setText(musicInfo.getArtist());
			playImageView.setImageResource(R.drawable.puase);
			//bcap.setImageBitmap(musicInfo.getAlbum_bitmap());
			if (false == turnTOback ) {
				MusicService.updataTime(timeHandler, timer, upTime);
			}
				
			MusicInfo.putCurrentMusicInfo(getApplicationContext(),
					currentListId , playMode, musicPosition);
			Log.w(TAG, "保存歌曲信息：list,mode,position:" + currentListId + playMode
					+ musicPosition);
		}
	}
	
	/**
	 * 音乐播放结束广播接收器，继续播放
	 * 
	 * @author Administrator
	 * 
	 */
	private class MusicCompleteReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (Constans.MODLE_ORDER == playMode) {
				if (musicPosition<currentMusicList.size()-1) {
					musicPosition = musicPosition + 1;
				} else {
					musicPosition = 0;
				}
			} else if (Constans.MODLE_RANDOM == playMode) {
				musicPosition = (int) (Math.random() * currentMusicList.size());
			} else {

			}
			playState = Constans.STATE_PLAY;
			MusicCommad(currentMusicList, Constans.PLAY_CMD, musicPosition, 0,
					true);
		}
	}
	
	/**
	 * 接收service发送的消息，实时更新时间
	 * 
	 * @author Administrator
	 * 
	 */
	@SuppressLint("HandlerLeak")
	private class UpdateInfoHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			try {
				String[] time = (String[]) msg.obj;
				int progress = msg.what;
				timeTextView.setText(time[0]+"/"+time[1]);
				seekBar.setProgress(progress);
			} catch (Exception e) {
			}
		}

	}
	
	/**
	 * 按钮监听器
	 * @author Administrator
	 *
	 */
	private class OnbuttomItemClickeListener implements OnClickListener{

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.musicTop_backLayout:
				Intent intent = new Intent(getApplicationContext(),
						HomeActivity.class);
				intent.putExtra("SERVICE_STATE", playState);
				// activity发生改变，将消息传给Service
				musicIntent.putExtra("Activity", Constans.ACTIVITY_HOME);
				MusicCommad(currentMusicList, Constans.ACTIVITY_CHANGED_CMD, musicPosition, 0,
						true); // 通知serviceActivity发生改变
				unregisterReceiver(receiver); //停止接收广播，转由HomeActivity接收
				startActivity(intent);
				break;
			case R.id.music_like:
				
				break;
			case R.id.music_menu:
				
				break;
			case R.id.music_order:
				if (Constans.MODLE_ORDER == playMode) {
					playMode = Constans.MODLE_RANDOM;
					orderImageView.setImageResource(R.drawable.random);
				} else if (Constans.MODLE_RANDOM == playMode) {
					playMode = Constans.MODLE_SINGLE;
					orderImageView.setImageResource(R.drawable.single);
				} else {
					playMode = Constans.MODLE_ORDER;
					orderImageView.setImageResource(R.drawable.order);
				}
				break;
			case R.id.music_last:
				if (Constans.MODLE_RANDOM == playMode) {
					musicPosition = (int) (Math.random() * currentMusicList
							.size());
				} else {
					if (musicPosition>0) {
						musicPosition = musicPosition - 1;
					} else {
						musicPosition = currentMusicList.size()-1;
					}
				}
				playState = Constans.STATE_PLAY;
				MusicCommad(currentMusicList, Constans.PLAY_CMD, musicPosition,
						0, true);
				break;
			case R.id.music_play:
				if (Constans.STATE_PLAY == playState) {
					MusicCommad(currentMusicList, Constans.PUASE_CMD,
							musicPosition, 0, true);
					playImageView.setImageResource(R.drawable.play);
					playState = Constans.STATE_PUASE;
				} else if (Constans.STATE_PUASE == playState) {
					MusicCommad(currentMusicList, Constans.PLAY_CMD,
							musicPosition, -1, true);
					playImageView.setImageResource(R.drawable.puase);
					playState = Constans.STATE_PLAY;
				} else {
					MusicCommad(currentMusicList, Constans.PLAY_CMD,
							musicPosition, 0, true);
					playImageView.setImageResource(R.drawable.puase);
					playState = Constans.STATE_PLAY;
				}
				break;
				
			case R.id.music_next:
				if (Constans.MODLE_RANDOM == playMode) {
					musicPosition = (int) (Math.random() * currentMusicList
							.size());
				} else {
					if (musicPosition<currentMusicList.size()-1) {
						musicPosition = musicPosition + 1;
					} else {
						musicPosition = 0;
					}
				}
				playState = Constans.STATE_PLAY;
				MusicCommad(currentMusicList, Constans.PLAY_CMD, musicPosition,
						0, true);
				break;
			case R.id.music_list:
				
				break;
			default:
				break;
			}
		}
	}
	
	/**
	 * 进度条拖拽事件
	 * @author Administrator
	 *
	 */
	private class OnProgressChagedListener implements OnSeekBarChangeListener{

		private float progressRate ; //之前设置最大为200
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			if (fromUser) {
				this.progressRate = (float)progress/200;
				Log.w(TAG, "进度："+progressRate);
			}
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			int rate = (int)(progressRate*100);
			MusicCommad(currentMusicList, Constans.PLAY_CMD, musicPosition,
					rate, true);
		}
		
	}
}