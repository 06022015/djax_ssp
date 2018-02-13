package com.ssp.core.impl;

import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import com.ssp.api.Constant;
import com.ssp.api.dto.BidData;
import com.ssp.api.dto.DSPResponse;
import com.ssp.api.entity.jpa.AdBlockInfo;
import com.ssp.api.entity.jpa.DSPInfo;
import com.ssp.api.entity.jpa.WinNoticeEntity;
import com.ssp.api.exception.QPSLimitOverFlowException;
import com.ssp.api.exception.SSPException;
import com.ssp.api.service.SSPService;
import com.ssp.core.task.DSPNotifyTask;
import com.ssp.core.task.DSPTask;
import com.ssp.core.util.RTBRequest;
import com.ssp.core.util.RTBRequestFactory;
import com.ssp.core.util.SSPBean;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import com.google.openrtb.OpenRtb.BidRequest;

import javax.transaction.Transactional;

/**
 * Created by IntelliJ IDEA.
 * User: ashqures
 * Date: 7/18/17
 * Time: 11:16 PM
 * To change this template use File | Settings | File Templates.
 */
@Service("sspService")
@Transactional
public class SSPServiceImpl implements SSPService{

    private static Logger logger = LoggerFactory.getLogger(SSPServiceImpl.class);

    private Map<String,String> defaultProp;

    private RTBRequestFactory rtbRequestFactory;

    public SSPServiceImpl() {
        initDefaultProp();
        this.rtbRequestFactory = new RTBRequestFactory();
    }

    @Autowired
    private SSPBean sspBean;

    public BidData processRequest(Map<String,String> parameter) throws SSPException , IOException {
        Long pubId = Long.parseLong(parameter.get(Constant.PUBLISHER_ID));
        AdBlockInfo adBlockInfo = sspBean.getJpaRepository().getAdBlockInfo(pubId, Long.parseLong(parameter.get(Constant.BLOCK_ID)));
        if(null == adBlockInfo)
            throw new SSPException(HttpStatus.BAD_REQUEST.value(),"Adblock info is not found for pub id:- "+ pubId + " and block id:-"+ parameter.get(Constant.BLOCK_ID));
        logger.debug("Publisher detail := "+adBlockInfo.toString());
        CityResponse location = getLocation(parameter.get(Constant.IP));
        logger.debug("Geo location:= "+ (null != location?location.toString():location));
        parameter.put(Constant.LMT, sspBean.getProperties().getProperty("ssp.lmt"));
        parameter.put(Constant.CURRENCY, sspBean.getProperties().getProperty("ssp.currency"));
        parameter.putAll(defaultProp);
        RTBRequest rtbRequest = rtbRequestFactory.rtb(Integer.parseInt(adBlockInfo.getAdFormat()),Integer.parseInt(adBlockInfo.getAppType()));
        BidRequest bidRequest = rtbRequest.generate(adBlockInfo,location, parameter);
        String requestContent = rtbRequest.getBidAsString(bidRequest);
        logger.debug("Bid request:= "+requestContent);
        //this.sspBean.getMongoRepository().saveRTBJSON(requestContent);
        //logger.debug("rtb saved in mongo ");
        List<DSPInfo> dspInfos = sspBean.getJpaRepository().getAllDSP(adBlockInfo.getAdFormat());
        logger.debug("dspinfo "+dspInfos);
        Integer maxResponseTime = Integer.parseInt(sspBean.getProperties().getProperty(Constant.DSP_MAX_RESPONSE_PROP));
        List<Future<DSPResponse>> taskList = new ArrayList<Future<DSPResponse>>();
        JSONObject rtbJSON = parseRTBRequestAsJSON(requestContent);
        JSONArray dspIds = new JSONArray();
        for(DSPInfo dspInfo : dspInfos){
            dspInfo.setMaxResponseTime(maxResponseTime);
            try{
                DSPTask dspRequestTask = new DSPTask(sspBean,bidRequest, dspInfo, requestContent, rtbRequest);
                Future<DSPResponse> task = sspBean.getDspExecutor().submit(dspRequestTask);
                taskList.add(task);
                //dspIds.add(dspInfo.getUserId());
            }catch (QPSLimitOverFlowException e){
                logger.error(e.getMessage());
            }
        }
        rtbJSON.put("adspaceid", parameter.get(Constant.BLOCK_ID));
        logger.debug("called dsp ");
        Double maxValue = -1.0;
        DSPResponse winningDSP = null;
        JSONArray responses = new JSONArray();
        for(Future<DSPResponse> future : taskList){
            try {
                //DSPResponse dspResponse = future.get(maxResponseTime+10, TimeUnit.MILLISECONDS);
                DSPResponse dspResponse = future.get();
                if(null != dspResponse){
                    dspIds.add(dspResponse.getDspInfo().getUserId());
                    responses.add(dspResponse.getResponseAsJSON());
                    if(dspResponse.getCode() == HttpStatus.OK.value()){
                        if(dspResponse.getBidData().getAuctionPrice()>maxValue){
                            maxValue = dspResponse.getBidData().getAuctionPrice();
                            winningDSP = dspResponse;
                        }
                    }
                }
            } catch (InterruptedException e) {
                logger.error("Thread interrupted. "+ e.getMessage());
            } catch (ExecutionException e) {
                logger.error("dsp task execution task error:- "+ e.getMessage());
            } /*catch (TimeoutException e) {
                logger.error("dsp response time out ignoring it:- error message:- " + e.getMessage());
            }*/
        }
        logger.debug("read dsp response");
        rtbJSON.put("dsp", dspIds);
        rtbJSON.put("responses", responses);
        //logger.debug("Saving rtb request");
        //this.sspBean.getMongoRepository().saveRTBJSON(rtbJSON.toJSONString());
       // logger.debug("Saved rtb request");
        logger.debug("Saving dsp response");
        //this.sspBean.getMongoRepository().saveDSPResponse(dspResponsesAsJSON.toJSONString());
        logger.debug("Saved dsp response");
        if(null != winningDSP){
            JSONObject wonDSP = new JSONObject();
            wonDSP.put("dsp", winningDSP.getDspInfo().getUserId());
            wonDSP.put("price", winningDSP.getBidData().getAuctionPrice().floatValue()/1000);
            rtbJSON.put("won", wonDSP);
            logger.debug("notifying will URL");
            DSPNotifyTask notifyTask = new DSPNotifyTask(sspBean,winningDSP.getBidData().getFullNURL());
            sspBean.getDspNotifyExecutor().submit(notifyTask);
            saveWinningBid(winningDSP, adBlockInfo, pubId.intValue(), Integer.parseInt(parameter.get(Constant.BLOCK_ID)));
        }
        logger.debug("Saving in mongo..." + rtbJSON);
        this.sspBean.getMongoRepository().save(sspBean.getProperties().getProperty("ssp.mongo.collection.name"),rtbJSON.toJSONString());
        logger.debug("Saved in mongo...");
        BidData bidData = null;
        if(null != winningDSP){
            bidData = winningDSP.getBidData();
            bidData.setWidth(adBlockInfo.getWidth());
            bidData.setHeight(adBlockInfo.getHeight());
        }
        return bidData;
    }

    private JSONObject parseRTBRequestAsJSON(String rtbRequest)  {
        JSONParser parser = new JSONParser();
        try {
            return  (JSONObject) parser.parse(rtbRequest);
        } catch (ParseException e) {
            logger.error("Unable to convert RTB request as JSONObject");
            throw new SSPException(500,"Unable to convert RTB request as JSONObject");
        }
    }

    private void saveWinningBid(DSPResponse dspResponse, AdBlockInfo adBlockInfo, Integer pubId, Integer blockId){
        WinNoticeEntity winNotice = new WinNoticeEntity();
        winNotice.setDspId(dspResponse.getDspInfo().getUserId().intValue());
        winNotice.setPublisherId(pubId);
        winNotice.setPublisherShare(adBlockInfo.getFloorPrice()/1000);
        winNotice.setDspBidAmount(dspResponse.getBidData().getAuctionPrice().floatValue()/1000);
        winNotice.setRequestId(dspResponse.getBidData().getAuctionId());
        winNotice.setAdSpaceId(blockId);
        this.sspBean.getJpaRepository().saveWinningBid(winNotice);
    }

    private CityResponse getLocation(String ip){
        try {
            return sspBean.getLocationService().getLocation(ip);
        } catch (IOException  e) {
            logger.error("Not able to read location from maxmind");
            //throw new SSPException("Not able to read location from maxmind",e,HttpStatus.BAD_REQUEST.value());
        } catch (GeoIp2Exception e) {
            logger.error("Not able to read location from maxmind");
            //throw new SSPException("Not able to read location from maxmind",e,HttpStatus.BAD_REQUEST.value());
        }
        return null;
    }

    private void initDefaultProp(){
        this.defaultProp = new HashMap<String, String>();
        this.defaultProp.put("video.min.bitrate", sspBean.getProperties().getProperty("video.min.bitrate"));
        this.defaultProp.put("video.max.bitrate", sspBean.getProperties().getProperty("video.max.bitrate"));
        this.defaultProp.put("video.max.extended", sspBean.getProperties().getProperty("video.max.extended"));
        this.defaultProp.put("video.min.duration", sspBean.getProperties().getProperty("video.min.duration"));
        this.defaultProp.put("video.max.duration", sspBean.getProperties().getProperty("video.max.duration"));
    }
}
