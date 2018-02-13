package com.ssp.core.util;

import com.google.openrtb.OpenRtb;
import com.maxmind.geoip2.model.CityResponse;
import com.ssp.api.Constant;
import com.ssp.api.entity.jpa.AdBlockInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

public class NativeRTBWebRequest extends RTBRequest {

    public NativeRTBWebRequest() {
        super();
    }

    public OpenRtb.BidRequest generate(AdBlockInfo adBlockInfo, CityResponse location, Map<String, String> parameter) {
        OpenRtb.BidRequest bidRequest = null;
        try{
            bidRequest = OpenRtb.BidRequest.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setAt(OpenRtb.AuctionType.FIRST_PRICE)
                .setTmax(120)
                .addCur(parameter.get(Constant.CURRENCY))
                .setTest(false)
                .setRegs(OpenRtb.BidRequest.Regs.newBuilder()
                        .setCoppa(false))
                .addImp(OpenRtb.BidRequest.Imp.newBuilder()
                    .setId("1")
                    .setBidfloor(adBlockInfo.getFloorPrice())
                    .setNative(OpenRtb.BidRequest.Imp.Native.newBuilder()
                        .setRequest("")
                        .setVer("1.0")
                        .addApi(OpenRtb.APIFramework.forNumber(3))
                    )
                )
                .addAllBcat(new ArrayList<String>(0))//Confirmed from Hari
                .addAllBadv(new ArrayList<String>(0))//Confirmed from Hari
                .setSite(OpenRtb.BidRequest.Site.newBuilder()
                    .setId(adBlockInfo.getSiteId()+"")
                    .setName(adBlockInfo.getSiteName())
                    .setDomain(adBlockInfo.getSiteURL())
                    .addAllCat(Arrays.asList(adBlockInfo.getSiteCat(), adBlockInfo.getAdBlockCat()))
                    .setPrivacypolicy(true)
                    .setKeywords("") //Empty value. Confirmed from Hari
                    .addAllSectioncat(new ArrayList<String>(0))//Empty value. Confirmed from Hari
                    .addAllPagecat(new ArrayList<String>(0))//Empty value. Confirmed from Hari
                    .setPage(parameter.get(Constant.REQUESTER_URL))  // It should be requester URL
                    .setRef(parameter.get(Constant.REF_URL)) // It shouw ref request param.
                    .setPublisher(OpenRtb.BidRequest.Publisher.newBuilder()
                        .setId(adBlockInfo.getUserId()+"")
                        .addCat(adBlockInfo.getAdBlockCat())
                        .setDomain(adBlockInfo.getWebsite())
                        .setName(adBlockInfo.getFirstName()+ " "+ adBlockInfo.getLastName())
                    )
                )
                .setDevice(getDeviceBuilder(location, parameter))
                .setUser(OpenRtb.BidRequest.User.newBuilder().setId(adBlockInfo.getUserId()+""))
                //.setExtension()//Not required on current phase. Based on discussion over skype.
                .build();
        }catch (Exception e){
            e.printStackTrace();
        }
        return bidRequest;
    }
}
