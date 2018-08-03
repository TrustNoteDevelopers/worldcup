package com.thingtrust.sportslottery.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 〈〉
 *
 * @author WangYu
 * @create 2018/5/21
 * @since 1.0.0
 */
@Service
@Slf4j
public class FifaService {
    @Autowired
    private UrlConfig urlConfig;
    @Autowired
    private FifaCathecticRepository fifaCathecticRepository;
    @Autowired
    private FifaMatchRepository fifaMatchRepository;
    @Autowired
    private FifaOddsRepository fifaOddsRepository;

    /**
     * 生成地址
     *
     * @return
     */
    private String generateAddress() {
        final String url = urlConfig.getNodeurl() + "/address";
        final String body = OkHttpUtils.get(url, null);
        if (StringUtils.isNotEmpty(body)) {
            final JSONObject jsonObject = JSON.parseObject(body);
            final String address = (String) jsonObject.get("address");
            return address;
        }
        return null;
    }

    /**
     * 检查地址余额
     *
     * @param address
     * @return
     */
    @Synchronized
    public ResponseResult checkBalances(final String address) {
        final String url = urlConfig.getNodeurl() + "/checkbalance";
        final Map<String, String> hashMap = Maps.newHashMap();
        hashMap.put("address", address);
        final String body = OkHttpUtils.get(url, hashMap);
        if (StringUtils.isNotEmpty(body)) {
            final FifaCathecticExample example = new FifaCathecticExample();
            example.createCriteria().andToAddrEqualTo(address);
            final FifaCathectic fifaCathectic = fifaCathecticRepository.selectOneByExample(example);
            final FifaMatch fifaMatch = fifaMatchRepository.selectById(fifaCathectic.getMatchId());
            if (LocalDateTime.now().isAfter(fifaMatch.getCathecticEndTime()) || fifaMatch.getCurrentAmount().compareTo(fifaMatch.getMaxAmount()) != -1) {
                return ResponseResult.failure(BizErrorCodeEnum.OVER_TIME_ERROR);
            }

            final JSONObject jsonObject = JSON.parseObject(body);
            final BalanceEntity data = jsonObject.getObject("data", BalanceEntity.class);
            final BigDecimal outs = data.getAll_outputs().divide(new BigDecimal(1000000));
            if (outs.compareTo(fifaCathectic.getAmount()) != -1) {
                fifaCathectic.setPaymentStatus(PaymentStatusEnum.PAYMENT_SUCCESS.getCode());
                fifaCathectic.setAmount(outs.compareTo(new BigDecimal(100)) == 1 ? new BigDecimal(100) : outs);
                fifaCathecticRepository.updateById(fifaCathectic);
                fifaMatch.setCurrentAmount(outs.compareTo(new BigDecimal(100)) == 1 ? new BigDecimal(100) : outs
                        .add(fifaMatch.getCurrentAmount()));
                fifaMatchRepository.updateById(fifaMatch);
                return ResponseResult.success(0);
            }
        } else {
            return ResponseResult.failure(BizErrorCodeEnum.INTERNET_ERROR);
        }
        return ResponseResult.success(1);
    }

    /**
     * 检查地址有效
     *
     * @param address
     * @return
     */
    public int checkAddress(final String address) {
        final String url = urlConfig.getNodeurl() + "/addressvalidation";
        final Map<String, String> hashMap = Maps.newHashMap();
        hashMap.put("address", address);
        final String body = OkHttpUtils.get(url, hashMap);
        int errCode = 1;
        if (body != null) {
            final JSONObject jsonObject = JSON.parseObject(body);
            errCode = (int) jsonObject.get("errCode");
        }
        return errCode;
    }

    /**
     * 批量打款
     */
    public ResponseResult bulkMoney(final List<Long> ids) {
        final FifaCathecticExample fifaCathecticExample = new FifaCathecticExample();
        fifaCathecticExample.createCriteria()
                .andIdIn(ids)
                .andBonusStatusEqualTo(NumberUtils.INTEGER_ZERO);
        final List<FifaCathectic> fifaCathecticList = fifaCathecticRepository.selectByExample(fifaCathecticExample);
        final List<Map> list = Lists.newArrayList();
        final List<Long> oddsIds = fifaCathecticList.stream()
                .map(FifaCathectic::getOddsId)
                .collect(Collectors.toList());
        final FifaOddsExample fifaOddsExample = new FifaOddsExample();
        fifaOddsExample.createCriteria()
                .andIdIn(oddsIds);
        final List<FifaOdds> fifaOddsList = fifaOddsRepository.selectByExample(fifaOddsExample);

        for (final FifaCathectic fifaCathectic : fifaCathecticList) {
            final Map map = Maps.newHashMap();
            map.put("address", fifaCathectic.getFromAddr());
            final BigDecimal odds = fifaOddsList.stream()
                    .filter(e -> e.getId().longValue() == fifaCathectic.getOddsId().longValue())
                    .map(FifaOdds::getOdds)
                    .findFirst()
                    .get();
            map.put("amount", fifaCathectic.getAmount().multiply(odds).multiply(new BigDecimal(1000000)));
            list.add(map);
        }
        final String jsonArray = JSONArray.toJSONString(list);
        final String str = sendToMultiAddress(jsonArray);
        final JSONObject jsonObject = JSON.parseObject(str);
        final MessageEntity error = jsonObject.getObject("error", MessageEntity.class);
        final List<Long> successIds;
        if (error != null) {
            if (error.getMessage().contains(urlConfig.getAddress())) {
                return ResponseResult.failure(BizErrorCodeEnum.NOT_ENOUGH_MONEY);
            } else {
                final List<String> errorAddr = Arrays.stream(error.getMessage().split(","))
                        .collect(Collectors.toList());
                final Map errorMap = Maps.newHashMap();
                errorAddr.stream()
                        .forEach(event -> {
                            errorMap.put(event, "1");
                        });

                successIds = fifaCathecticList.stream()
                        .filter(event -> errorMap.get(event.getFromAddr()) == null)
                        .map(FifaCathectic::getId)
                        .collect(Collectors.toList());
            }

        } else {
            successIds = fifaCathecticList.stream()
                    .map(FifaCathectic::getId)
                    .collect(Collectors.toList());
        }
        if (CollectionUtils.isNotEmpty(successIds)) {
            fifaCathecticExample.clear();
            fifaCathecticExample.createCriteria()
                    .andIdIn(successIds);
            fifaCathecticRepository.updateByExample(FifaCathectic.builder()
                    .bonusStatus(BonusStatusEnum.MARK_MONEY.getCode())
                    .build(), fifaCathecticExample);
        }
        return ResponseResult.success(str);
    }

    private String sendToMultiAddress(final String array) {
        final String parse = "[\"*****************\"," + array + "]";
        final JSONArray jsonArray = JSON.parseArray(parse);
        final Map map = Maps.newHashMap();
        map.put("method", "sendtomultiaddress");
        map.put("params", jsonArray);
        final String jsonRpcResult = OkHttpUtils.rpcRequestBodyPost(urlConfig.getBlockchainurl(), map);
        return jsonRpcResult;
    }

    public void checkPayment() {
        final FifaCathecticExample fifaCathecticExample = new FifaCathecticExample();
        fifaCathecticExample.createCriteria()
                .andPaymentStatusEqualTo(PaymentStatusEnum.WAIT_PAYMENT.getCode())
                .andCreateTimeBetween(LocalDateTime.now().minusHours(10), LocalDateTime.now())
        ;
        final List<FifaCathectic> fifaCathecticList = fifaCathecticRepository.selectByExample(fifaCathecticExample);
        fifaCathecticList.stream().forEach(fifaCathectic -> {
            checkBalances(fifaCathectic.getToAddr());
        });
    }
}
