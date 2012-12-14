package brock.soniferous;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.os.Parcel;
import android.os.Parcelable;

public class Sound implements Parcelable {
	private float mPlaybackRate;
	private final float mMinDeviceFrequency;
	private final float mMaxDeviceFrequency;
	private float mVolume;
	private final float mMinDeviceAmplitude;
	private final float mMaxDeviceAmplitude;
	private int mRepetitionIntervalMilliseconds;
	private boolean mMuted;
	private int mSoundResourceID;
	private int mSoundPoolStreamID;
	private float mDeviceAmplitude;

	private static SoundPool mSoundPool;
	//private static final int[] mSoundResourceIDs = new int[] {R.raw.acheta, R.raw.bsp3, R.raw.corncrak2, R.raw.gartenbl, R.raw.parus2, R.raw.wechselk, R.raw.zwergta};
	private static final int[] mSoundResourceIDs = new int[] {R.raw.acheta,
																														R.raw.bee2,
																														R.raw.amsel,
																														R.raw.amsel2,
																														R.raw.blackter,
																														R.raw.bsp3,
																														R.raw.chaffinc,
																														R.raw.corncrak2,
																														R.raw.fit,
																														R.raw.gambelim,
																														R.raw.gartenbl,
																														R.raw.grs,
																														R.raw.heckenb2,
																														R.raw.heckenbr,
																														R.raw.hnf,
																														R.raw.ksp4,
																														R.raw.nightin2,
																														R.raw.parus2,
																														R.raw.waldlaub3,
																														R.raw.wechselk,
																														R.raw.wendehal,
																														R.raw.zaunkoen,
																														R.raw.zkn2,
																														R.raw.zwergta
																														};
	private static final List<Integer> mLoadedSoundIDs = new ArrayList<Integer>();
	private static final ConcurrentHashMap<Integer, Integer> mResourceIDToSoundIDMap = new ConcurrentHashMap<Integer, Integer>();
	private ScheduledExecutorService mScheduleTaskExecutor = Executors.newScheduledThreadPool(1);
	
	public static final int MAX_SOUNDPOOL_STREAMS = 10;
	private static final float MIN_RATE = .4f;
	private static final float MAX_RATE = 1.6f;
	private static final float MIN_VOLUME = 0f;
	private static final float MAX_VOLUME = 1f;
	private static boolean mClosed = true;
	private static ReadWriteLock mReadWriteClosedLock = new ReentrantReadWriteLock();
	private static ReadWriteLock mReadWriteSoundStreamIDLock = new ReentrantReadWriteLock();
	
	// NOTE: initialize() needs to be called before using Sound
	public Sound(float deviceFrequency, float minDeviceFrequency, float maxDeviceFrequency, float deviceAmplitude, float minDeviceAmplitude, float maxDeviceAmplitude, int repetitionIntervalMilliseconds, int soundResourceID) {
		mMinDeviceFrequency = minDeviceFrequency;
		mMaxDeviceFrequency = maxDeviceFrequency;
		mDeviceAmplitude = deviceAmplitude;
		mVolume = getVolumeFromAmplitude(deviceAmplitude, minDeviceAmplitude, maxDeviceAmplitude);
		mMinDeviceAmplitude = minDeviceAmplitude;
		mMaxDeviceAmplitude = maxDeviceAmplitude;
		mRepetitionIntervalMilliseconds = repetitionIntervalMilliseconds;
		mMuted = false;
		mSoundResourceID = soundResourceID;
		if (deviceFrequency != 0) {
			mPlaybackRate = getRateFromDeviceFrequency(deviceFrequency, minDeviceFrequency, maxDeviceFrequency);
		}
	}
	
	// NOTE: initialize() needs to be called before using Sound
	public Sound(float deviceAmplitude, float minDeviceAmplitude, float maxDeviceAmplitude, int repetitionIntervalMilliseconds, int soundResourceID) {
		this(0, 0, 0, deviceAmplitude, minDeviceAmplitude, maxDeviceAmplitude, repetitionIntervalMilliseconds, soundResourceID);
	}
	
	private Sound(Parcel p) {
		this(0, p.readFloat(), p.readFloat(), p.readFloat(), p.readFloat(), p.readFloat(), p.readInt(), p.readInt());
		mMuted = (p.readByte() == 1);
		mPlaybackRate = p.readFloat();
	}
	
	public static void initialize() {
		mClosed = false;
		mLoadedSoundIDs.clear();
		mResourceIDToSoundIDMap.clear();
		
		mSoundPool = new SoundPool(MAX_SOUNDPOOL_STREAMS, AudioManager.STREAM_MUSIC, 0);
		mSoundPool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
			public void onLoadComplete(SoundPool soundPool, int soundID, int status) {
				synchronized(mLoadedSoundIDs) {
					mLoadedSoundIDs.add(soundID);
				}
			}
		});
		
		for (int i = 0; i < mSoundResourceIDs.length; ++i) {
			int soundID = mSoundPool.load(MyApp.getContext(), mSoundResourceIDs[i], 1);
			mResourceIDToSoundIDMap.put(mSoundResourceIDs[i], soundID);
		}
	}
	
	public static void close() {
		mReadWriteClosedLock.writeLock().lock();
		mClosed = true;
		mReadWriteClosedLock.writeLock().unlock();
		
		mLoadedSoundIDs.clear();
		mResourceIDToSoundIDMap.clear();
		mSoundPool.release();
		mSoundPool = null;
	}
	
	public void startPlaying() {
		mReadWriteClosedLock.readLock().lock();
		boolean closed = mClosed;
		mReadWriteClosedLock.readLock().unlock();
		if (closed) {
			return;
		}
		
		if (mRepetitionIntervalMilliseconds != 0) {
			playSoundWithInterval();
			return;
		}
		
		final int soundID = mResourceIDToSoundIDMap.get(mSoundResourceID);
		if (mLoadedSoundIDs.contains(soundID)) {
			mReadWriteSoundStreamIDLock.writeLock().lock();
			mSoundPoolStreamID = mSoundPool.play(soundID, mVolume, mVolume, 1, -1, mPlaybackRate);
			if (mSoundPoolStreamID == 0) {
				// TODO: log warning
			}
			mReadWriteSoundStreamIDLock.writeLock().unlock();
		}
		else {
			// Sounds are loaded asynchronously, so we need to have a thread that checks if the sound is loaded and sleeps if it isn't.
			mScheduleTaskExecutor.schedule(new Runnable() {
			  public void run() {
			  	while (true) {
			  		mReadWriteClosedLock.readLock().lock();
			  		boolean closed = mClosed;
			  		mReadWriteClosedLock.readLock().unlock();
			  		if (closed) {
			  			return;
			  		}
			  		
				  	boolean soundIsLoaded = false;
				  	synchronized(mLoadedSoundIDs) {
				  		soundIsLoaded = mLoadedSoundIDs.contains(soundID);
				  	}
				  	
				  	if (soundIsLoaded) {
				  		mReadWriteSoundStreamIDLock.writeLock().lock();
				  		mSoundPoolStreamID = mSoundPool.play(soundID, mVolume, mVolume, 1, -1, mPlaybackRate);
							if (mSoundPoolStreamID == 0) {
								// TODO: log warning
							}
							mReadWriteSoundStreamIDLock.writeLock().unlock();
							return;
				  	}
				  	else {
				  		try {
								Thread.sleep(100);
							}
				  		catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
				  	}
			  	}
			  }
			}, 0, TimeUnit.SECONDS);
		}
	}
	
	private void playSoundWithInterval() {
				final int soundID = mResourceIDToSoundIDMap.get(mSoundResourceID);
				mScheduleTaskExecutor.scheduleWithFixedDelay(new Runnable() {
				  public void run() {
				  	while (true) {
				  		mReadWriteClosedLock.readLock().lock();
				  		boolean closed = mClosed;
				  		mReadWriteClosedLock.readLock().unlock();
				  		if (closed) {
				  			return;
				  		}
				  		
					  	boolean soundIsLoaded = false;
					  	synchronized(mLoadedSoundIDs) {
					  		soundIsLoaded = mLoadedSoundIDs.contains(soundID);
					  	}
					  	
					  	if (soundIsLoaded) {
					  		mReadWriteSoundStreamIDLock.writeLock().lock();
					  		mSoundPoolStreamID = mSoundPool.play(soundID, mVolume, mVolume, 1, 0, mPlaybackRate);
								if (mSoundPoolStreamID == 0) {
									// TODO: log warning
								}
								mReadWriteSoundStreamIDLock.writeLock().unlock();
								return;
					  	}
					  	else {
					  		try {
									Thread.sleep(100);
								}
					  		catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
					  	}
				  	}
				  }
				}, (long)mRepetitionIntervalMilliseconds, (long)mRepetitionIntervalMilliseconds, TimeUnit.MILLISECONDS);
	}
	
	public void stopPlaying() {
		mReadWriteClosedLock.readLock().lock();
		boolean closed = mClosed;
		mReadWriteClosedLock.readLock().unlock();
		if (closed) {
			return;
		}

		if (mRepetitionIntervalMilliseconds != 0) { // if there's a repetition interval, then there's a thread we need to kill before continuing.
			try {
				mScheduleTaskExecutor.shutdownNow();
				mScheduleTaskExecutor.awaitTermination((long)mRepetitionIntervalMilliseconds * 5, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				// TODO log warning
				e.printStackTrace();
			}
		}
		mReadWriteSoundStreamIDLock.readLock().lock();
		mSoundPool.stop(mSoundPoolStreamID);
		mReadWriteSoundStreamIDLock.readLock().unlock();
	}
	
	public void adjustPlaybackRateFromDeviceFrequency(float deviceFrequency) {
		mReadWriteClosedLock.readLock().lock();
		boolean closed = mClosed;
		mReadWriteClosedLock.readLock().unlock();
		if (closed) {
			return;
		}
		
		// Map the frequency range to the rate range in order to determine the appropriate rate value.
		float rate = getRateFromDeviceFrequency(deviceFrequency, mMinDeviceFrequency, mMaxDeviceFrequency);
		mReadWriteSoundStreamIDLock.readLock().lock();
		mSoundPool.setRate(mSoundPoolStreamID, rate);
		mReadWriteSoundStreamIDLock.readLock().unlock();
	}
	
	public float getVolume() {
		return mVolume;
	}
	public void adjustVolumeFromDeviceAmplitude(float deviceAmplitude) {
		mDeviceAmplitude = deviceAmplitude;
		mReadWriteClosedLock.readLock().lock();
		boolean closed = mClosed;
		mReadWriteClosedLock.readLock().unlock();
		if (closed) {
			return;
		}
		
		mVolume = getVolumeFromAmplitude(mDeviceAmplitude, mMinDeviceAmplitude, mMaxDeviceAmplitude);
		mReadWriteSoundStreamIDLock.readLock().lock();
		mSoundPool.setVolume(mSoundPoolStreamID, mVolume, mVolume);
		mReadWriteSoundStreamIDLock.readLock().unlock();
	}
	
	public float getRepetitionInterval() {
		return mRepetitionIntervalMilliseconds;
	}
	
	public boolean getMuted() {
		return mMuted;
	}
	
	public void setMuted(boolean muted) {
		mReadWriteClosedLock.readLock().lock();
		boolean closed = mClosed;
		mReadWriteClosedLock.readLock().unlock();
		if (closed) {
			return;
		}

		mReadWriteSoundStreamIDLock.readLock().lock();
		mMuted = muted;
		if (muted) {
			mSoundPool.pause(mSoundPoolStreamID);
		}
		else {
			mSoundPool.resume(mSoundPoolStreamID);
		}
		mReadWriteSoundStreamIDLock.readLock().unlock();
	}
	
	public int getSoundResourceID() {
		return mSoundResourceID;
	}
	
	public int getSoundPoolStreamID() {
		return mSoundPoolStreamID;
	}
	
	private static float getRateFromDeviceFrequency(float deviceFrequency, float minDeviceFrequency, float maxDeviceFrequency) {
		float rate = (((deviceFrequency - minDeviceFrequency) * (MAX_RATE - MIN_RATE)) / (maxDeviceFrequency - minDeviceFrequency)) + MIN_RATE; 
		// make sure the rate is in the valid range.
		return Math.max(Math.min(rate, MAX_RATE), MIN_RATE);
	}
	
	private static float getVolumeFromAmplitude(float deviceAmplitude, float minDeviceAmplitude, float maxDeviceAmplitude) {
		float volume = (((deviceAmplitude - minDeviceAmplitude) * (MAX_VOLUME - MIN_VOLUME)) / (maxDeviceAmplitude - minDeviceAmplitude)) + MIN_VOLUME;
		// make sure the volume is in the valid range.
		return Math.max(Math.min(volume, MAX_VOLUME), MIN_VOLUME);
	}

	public int describeContents() {
		return this.hashCode();
	}

	public void writeToParcel(Parcel dest, int flags) {
		// The order of these lines matters! The values get read out in the same order they're written. Lame.
		dest.writeFloat(mMinDeviceFrequency);
		dest.writeFloat(mMaxDeviceFrequency);
		dest.writeFloat(mVolume);
		dest.writeFloat(mMinDeviceAmplitude);
		dest.writeFloat(mMaxDeviceAmplitude);
		dest.writeInt(mRepetitionIntervalMilliseconds);
		dest.writeInt(mSoundResourceID);
		dest.writeByte((byte)(mMuted ? 1 : 0));
		dest.writeFloat(mPlaybackRate);
	}
	
	public static final Parcelable.Creator<Sound> CREATOR =
	new Parcelable.Creator<Sound>() {
		public Sound createFromParcel(Parcel p) {
			return new Sound(p);  
		}
	
		public Sound[] newArray(int size) {
			return new Sound[size];
		}
	};
}
