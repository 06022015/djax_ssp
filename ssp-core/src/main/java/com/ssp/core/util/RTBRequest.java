package com.ssp.core.util;

import com.codahale.metrics.MetricRegistry;
import com.google.openrtb.OpenRtb;
import com.google.openrtb.json.OpenRtbJsonFactory;
import com.google.openrtb.util.OpenRtbValidator;
import com.maxmind.geoip2.model.CityResponse;
import com.ssp.api.Constant;
import com.ssp.api.entity.jpa.AdBlockInfo;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.Map;

public abstract class RTBRequest {

    private OpenRtbValidator validator;
    private OpenRtbJsonFactory openrtbJson;

    public RTBRequest() {
        validator = new OpenRtbValidator(new MetricRegistry());
        openrtbJson = OpenRtbJsonFactory.create();
    }


    public abstract OpenRtb.BidRequest generate(AdBlockInfo adBlockInfo , CityResponse location, Map<String,String> parameter);


    public OpenRtb.BidRequest.Device.Builder getDeviceBuilder(CityResponse location, Map<String,String> parameter){
        OpenRtb.BidRequest.Device.Builder deviceBuilder =  OpenRtb.BidRequest.Device.newBuilder()
                .setDnt(false)
                .setLmt(Boolean.parseBoolean(parameter.get(Constant.LMT)))
                .setIp(parameter.get(Constant.IP))
                .setUa(parameter.get(Constant.USER_AGENT))
                .setOs(parameter.get(Constant.DEVICE_OS))
                .setLanguage(parameter.get(Constant.DEVICE_LANG))
                .setMake(parameter.get(Constant.DEVICE_MAKE))
                .setJs(true)
                .setConnectiontype(getConnectionType(location))
                .setDevicetype(getDeviceType(parameter.get(Constant.FORM_FACTOR)))
                .setOsv(parameter.get(Constant.DEVICE_OS_VERSION))
                .setModel(parameter.get(Constant.DEVICE_MODEL))
                .setHwv(parameter.get(Constant.DEVICE_HWV));
        if(null != location){
            deviceBuilder.setGeo(OpenRtb.BidRequest.Geo.newBuilder()
                    .setLat((location.getLocation().getLatitude() != null) ? location.getLocation().getLatitude() : 0.000000)
                    .setLon((location.getLocation().getLongitude() != null) ? location.getLocation().getLongitude() : 0.000000)
                    .setCountry((location.getCountry().getIsoCode() != null) ? location.getCountry().getIsoCode() : "")
                    .setCity((location.getCity().getName() != null) ? location.getCity().getName() : "")
                    .setZip((location.getPostal().getCode() != null) ? location.getPostal().getCode() : "")
                    .setMetro(location.getLocation().getMetroCode()+"")
                    .setType(OpenRtb.LocationType.IP)
                    .setUtcoffset(200));
            //Region not required . based on discussion over skype with Hari
        }
        return  deviceBuilder;
    }

    private static OpenRtb.ConnectionType getConnectionType(CityResponse location){
        OpenRtb.ConnectionType connectionType = OpenRtb.ConnectionType.CONNECTION_UNKNOWN;
        if(null != location && location.getTraits().getConnectionType() != null){
            if(location.getTraits().getConnectionType().toString().equals("Cellular"))
                connectionType = OpenRtb.ConnectionType.CELL_UNKNOWN;
            else if(location.getTraits().getConnectionType().toString().equals("Cable/DSL"))
                connectionType = OpenRtb.ConnectionType.ETHERNET;
            else if(location.getTraits().getConnectionType().toString().equals("Corporate"))
                connectionType = OpenRtb.ConnectionType.WIFI;
        }
        return connectionType;
    }

    private static OpenRtb.DeviceType getDeviceType(String formFactor){
        OpenRtb.DeviceType deviceType = OpenRtb.DeviceType.MOBILE;
        if(StringUtils.isEmpty(formFactor))
            return deviceType;
        if(formFactor.equalsIgnoreCase("desktop"))
            deviceType = OpenRtb.DeviceType.PERSONAL_COMPUTER;
        else if(formFactor.equalsIgnoreCase("tablet"))
            deviceType = OpenRtb.DeviceType.TABLET;
        else if(formFactor.equalsIgnoreCase("smartphone"))
            deviceType = OpenRtb.DeviceType.MOBILE;
        else if(formFactor.equalsIgnoreCase("smart-tv"))
            deviceType = OpenRtb.DeviceType.CONNECTED_TV;
        return deviceType;
    }

    public OpenRtb.BidResponse.Builder getBidResponse(String dspResponse) throws IOException {
        return openrtbJson.newReader().readBidResponse(dspResponse).toBuilder();
    }

    public boolean isValid(OpenRtb.BidRequest bidRequest, OpenRtb.BidResponse.Builder bidResponse) throws IOException {
        return this.validator.validate(bidRequest, bidResponse);
    }

    public String getBidAsString(OpenRtb.BidRequest bidRequest)throws IOException{
        return openrtbJson.newWriter().writeBidRequest(bidRequest);
    }
}
