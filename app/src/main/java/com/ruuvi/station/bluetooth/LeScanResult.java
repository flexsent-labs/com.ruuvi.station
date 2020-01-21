//package com.ruuvi.station.bluetooth;
//
//import android.bluetooth.BluetoothDevice;
//import android.content.Context;
//import android.util.Log;
//
//import com.neovisionaries.bluetooth.ble.advertising.ADManufacturerSpecific;
//import com.neovisionaries.bluetooth.ble.advertising.ADPayloadParser;
//import com.neovisionaries.bluetooth.ble.advertising.ADStructure;
//import com.neovisionaries.bluetooth.ble.advertising.EddystoneURL;
//import com.raizlabs.android.dbflow.data.Blob;
//import com.ruuvi.station.bluetooth.decoder.DecodeFormat2and4;
//import com.ruuvi.station.bluetooth.decoder.DecodeFormat3;
//import com.ruuvi.station.bluetooth.decoder.DecodeFormat5;
//import com.ruuvi.station.bluetooth.decoder.RuuviTagDecoder;
//import com.ruuvi.station.bluetooth.domain.HumidityCalibration;
//import com.ruuvi.station.bluetooth.domain.IRuuviTag;
//import com.ruuvi.station.util.Utils;
//
//import org.altbeacon.beacon.Beacon;
//import org.altbeacon.beacon.utils.UrlBeaconUrlCompressor;
//
//import java.util.List;
//
///**
// * Created by berg on 28/09/17.
// */
//
//class LeScanResult {
//
//    private static final String TAG = "LeScanResult";
//    public BluetoothDevice device;
//    public int rssi;
//    public byte[] scanData;
//
//    public IRuuviTag parse(Context context, RuuviTagFactory factory) {
//        IRuuviTag tag = null;
//
//        try {
//            // Parse the payload of the advertisement packet
//            // as a list of AD structures.
//            List<ADStructure> structures =
//                    ADPayloadParser.getInstance().parse(this.scanData);
//
//            // For each AD structure contained in the advertisement packet.
//            for (ADStructure structure : structures) {
//                if (structure instanceof EddystoneURL) {
//                    // Eddystone URL
//                    EddystoneURL es = (EddystoneURL) structure;
//                    if (es.getURL().toString().startsWith("https://ruu.vi/#") || es.getURL().toString().startsWith("https://r/")) {
//                        tag = from(context, factory, this.device.getAddress(), es.getURL().toString(), null, this.rssi);
//                    }
//                }
//                // If the AD structure represents Eddystone TLM.
//                else if (structure instanceof ADManufacturerSpecific) {
//                    ADManufacturerSpecific es = (ADManufacturerSpecific) structure;
//                    if (es.getCompanyId() == 0x0499) {
//                        tag = from(context, factory, this.device.getAddress(), null, this.scanData, this.rssi);
//                    }
//                }
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "Parsing ble data failed");
//        }
//        if (tag != null) {
//            tag = HumidityCalibration.apply(tag);
//        }
//
//        return tag;
//    }
//
//    private static IRuuviTag from(Context context, RuuviTagFactory factory, String id, String url, byte[] rawData, int rssi) {
//        RuuviTagDecoder decoder = null;
//        if (url != null && url.contains("#")) {
//            String data = url.split("#")[1];
//            rawData = Utils.parseByteDataFromB64(data);
//            decoder = new DecodeFormat2and4();
//        } else if (rawData != null) {
//            int protocolVersion = rawData[7];
//            switch (protocolVersion) {
//                case 3:
//                    decoder = new DecodeFormat3();
//                    break;
//                case 5:
//                    decoder = new DecodeFormat5();
//                    break;
//            }
//        }
//        if (decoder != null) {
//            IRuuviTag tag = decoder.decode(factory, rawData, 7);
//            if (tag != null) {
//                tag.setId(id);
//                tag.setUrl(url);
//                tag.setRssi(rssi);
//                tag.setRawData(rawData);
//                tag.setRawDataBlob(new Blob(rawData));
//                tag = HumidityCalibration.apply(tag);
//            }
//            return tag;
//        }
//        return null;
//    }
//
//    public static IRuuviTag fromAltbeacon(Context context, RuuviTagFactory factory, Beacon beacon) {
//        try {
//            byte pData[] = new byte[128];
//            List<Long> data = beacon.getDataFields();
//            for (int i = 0; i < data.size(); i++)
//                pData[i] = (byte)(data.get(i) & 0xFF);
//            RuuviTagDecoder decoder = null;
//            String url = null;
//            int format = beacon.getBeaconTypeCode();
//            if (data.size() > 0) format = pData[0];
//            switch (format) {
//                case 3:
//                    decoder = new DecodeFormat3();
//                    break;
//                case 5:
//                    decoder = new DecodeFormat5();
//                    break;
//                case 0x10:
//                    // format 2 & 4
//                    if (beacon.getServiceUuid() == 0xfeaa) {
//                        if (beacon.getId1() == null) break;
//                        url = UrlBeaconUrlCompressor.uncompress(beacon.getId1().toByteArray());
//                        if (url.contains("https://ruu.vi/#")) {
//                            String urlData = url.split("#")[1];
//                            pData = Utils.parseByteDataFromB64(urlData);
//                            decoder = new DecodeFormat2and4();
//                        }
//                    }
//                    break;
//            }
//            if (decoder != null) {
//                try {
//                    IRuuviTag tag = decoder.decode(factory, pData, 0);
//                    tag.setId(beacon.getBluetoothAddress());
//                    tag.setRssi(beacon.getRssi());
//                    tag.setUrl(url);
//                    tag.setRawData(pData);
//                    tag.setRawDataBlob(new Blob(pData));
//                    tag = HumidityCalibration.apply(tag);
//                    //Log.d(TAG, "logged tag with format: " + tag.dataFormat + " and mac: " + tag.id + " temp: " + tag.temperature);
//                    return tag;
//                } catch (Exception e) {
//                    Log.e(TAG, "Failed to parse tag data");
//                }
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "Failed to parse ruuviTag");
//        }
//        return null;
//    }
//}
