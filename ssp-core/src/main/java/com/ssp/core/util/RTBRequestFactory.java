package com.ssp.core.util;

import com.ssp.api.Constant;
import com.ssp.api.entity.jpa.AdBlockInfo;
import com.ssp.api.exception.InvalidTypeException;

import java.util.HashMap;
import java.util.Map;

public class RTBRequestFactory {

    private RTBRequest bannerWebRequest;
    private RTBRequest nativeWebRequest;
    private RTBRequest videoWebRequest;

    public RTBRequestFactory() {
        this.bannerWebRequest = new BannerRTBWebRequest();
        this.nativeWebRequest = new NativeRTBWebRequest();
        this.videoWebRequest = new VideoRTBWebRequest();
    }

    public RTBRequest rtb(int adFormat, int appType) throws  InvalidTypeException{
        RTBRequest rtbRequest;
        switch (adFormat){
            case 1:rtbRequest = getBannerRTBRequest(appType);
                break;
            case 2:rtbRequest = getVideoRTBRequest(appType);break;
            case 3:rtbRequest = getNativeRTBRequest(appType);break;
            default:throw new InvalidTypeException("Invalid AdFormat: "+ adFormat);
        }
        return rtbRequest;
    }

    private RTBRequest getBannerRTBRequest(int appType) throws InvalidTypeException {
        if(appType == 1 || appType == 2)
            return this.bannerWebRequest;
        else if(appType == 3 || appType == 4)
            return this.bannerWebRequest;
        else
            throw new InvalidTypeException("Invalid Banner AppType: "+appType);
    }

    private RTBRequest getNativeRTBRequest(int appType) throws InvalidTypeException {
        if(appType == 1 || appType == 2)
            return this.nativeWebRequest;
        else if(appType == 3 || appType == 4)
            return this.nativeWebRequest;
        else
            throw new InvalidTypeException("Invalid Native AppType: "+appType);
    }

    private RTBRequest getVideoRTBRequest(int appType) throws InvalidTypeException {
        if(appType == 1 || appType == 2)
            return this.videoWebRequest;
        else if(appType == 3 || appType == 4)
            return this.videoWebRequest;
        else
            throw new InvalidTypeException("Invalid Video AppType: "+appType);
    }

    public static void main(String args[]){
        AdBlockInfo adBlockInfo = new AdBlockInfo(1,"Ashif", "Qureshi", "http://wwww.ashif.qureshi.com",1,"qureshi_sote",
                "http://www.ashif.com", '1',"CAT_A",'3',200, 100,'1',2.0f,"BLOCK_A");
        Map<String,String> defaultProp = new HashMap<String, String>();
        defaultProp.put("video.min.bitrate", "300");
        defaultProp.put("video.max.bitrate", "1500");
        defaultProp.put("video.max.extended", "30");
        defaultProp.put("video.min.duration", "5");
        defaultProp.put("video.max.duration", "30");
        defaultProp.put("currency", "USD");
        defaultProp.put("requesterURL", "http://www.ashif.com");
        defaultProp.put("refUrl", "http://www.ashifqureshi.com");
        defaultProp.put("ip", "127.0.0.1");
        defaultProp.put("userAgent", "Browser");
        defaultProp.put(Constant.DEVICE_OS, "");
        defaultProp.put(Constant.DEVICE_CARRIER, "");
        defaultProp.put(Constant.DEVICE_MODEL, "");
        defaultProp.put(Constant.DEVICE_OS_VERSION, "");
        defaultProp.put(Constant.DEVICE_MAKE, "");
        defaultProp.put(Constant.DEVICE_TYPE, "");
        defaultProp.put(Constant.DEVICE_LANG, "");
        defaultProp.put(Constant.DEVICE_HWV, "");
        RTBRequestFactory factory = new RTBRequestFactory();
        try {
            RTBRequest rtbRequest = factory.rtb(Integer.parseInt(adBlockInfo.getAdFormat()),Integer.parseInt(adBlockInfo.getAppType()));
            System.out.println(rtbRequest.generate(adBlockInfo, null, defaultProp));
        } catch (InvalidTypeException e) {
            e.printStackTrace();
        }
    }
}

