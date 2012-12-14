//package brock.soniferous;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.List;
//
//import brock.soniferous.device.DeviceBase;
//
//public class Soundscape {
//	
//	private final HashMap<Integer, Integer> mResIDtoSoundIDMap = new HashMap<Integer, Integer>();
//	private final HashMap<DeviceBase, Sound> mDeviceToSoundMap =  new HashMap<DeviceBase, Sound>();
//	
//	public void updateSounds(DeviceBase[] devices, Class typeOfDevice) {
//  	// TODO: questionable assumption to revisit: two different types of devices can't have the same unique ID.
//		// TODO: This logic is kind of gross. Using a HashMap with DeviceBase as key can probably be refactored into
//		// something more intuitive.
//		for (int i = 0; i < devices.length && i < Sound.MAX_SOUNDPOOL_STREAMS; ++i) { 
//  		DeviceBase device = devices[i];
//  		if (mDeviceToSoundMap.containsKey(device)) {
//  			// If a sound is already playing, then update it.
//  			Sound currentSound = mDeviceToSoundMap.get(device);
//  			Sound newSound = devices[i].getSound();
//  			
//  			// The current sound is already playing, so just update its properties instead of replacing
//  			// it with the new sound object.
//  			currentSound.setAmplitude(newSound.getAmplitude());
//  			currentSound.setFrequency(newSound.getFrequency());
//  		}
//  		else if (!mDeviceToSoundMap.containsKey(device)) {
//  			// If there is a new sound, then add it.
//  			Sound newSound = devices[i].getSound();
//  			mDeviceToSoundMap.put(device, newSound);
//  			newSound.startPlaying();
//  		}
//  		else {
//  			// Remove stale sounds (i.e., get rid of devices that are no longer detected). 
//  			DeviceBase[] possiblyStaleDevices = (DeviceBase[])mDeviceToSoundMap.keySet().toArray();
//  			List<DeviceBase> devicesToRemove = new ArrayList<DeviceBase>();
//  			List<DeviceBase> updatedDevices = Arrays.asList(devices); 
//  			for (int j = 0; j < possiblyStaleDevices.length; ++j) {
//  				DeviceBase possiblyStaleDevice = possiblyStaleDevices[j];
//  				if (possiblyStaleDevice.getClass() == typeOfDevice && !updatedDevices.contains(possiblyStaleDevice)) {
//  					devicesToRemove.add(possiblyStaleDevice);
//  					mDeviceToSoundMap.get(possiblyStaleDevice).stopPlaying();
//  				}
//  			}
//  			
//  			for (int j = 0; j < devicesToRemove.size(); ++j) {
//  				mDeviceToSoundMap.remove(devicesToRemove.get(j));
//  			}
//  		}
//  	}
//  	// delete devices that are currently playing and not in the list
//  	// add devices that are not playing but are in the list
//	}
//}
