package com.thingtrust.sportslottery.rest;


import com.thingtrust.sportslottery.service.FifaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@Slf4j
@RequestMapping(value = "/community/fifa")
public class FifaController {

    @Autowired
    private FifaService fifaService;

    /**
     * 检查付款状态
     *
     * @param address
     * @return
     */
    @GetMapping("/check-balances")
    public ResponseResult checkBalances(final String address) {
        final ResponseResult responseResult = fifaService.checkBalances(address);
        return responseResult;
    }

    /**
     * 检查地址有效
     *
     * @param address
     * @return
     */
    @GetMapping("/check-address")
    public ResponseResult checkAddress(final String address) {
        return ResponseResult.success(fifaService.checkAddress(address));
    }

}