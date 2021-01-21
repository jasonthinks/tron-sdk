package org.tron.test;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.tron.utils.HttpClientUtils;
import org.tron.utils.TronUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * trx相关操作
 *
 * @Autor TrickyZh 2021/1/21
 * @Date 2021-01-21 20:19:11
 */
public class TrxTest {
    //shasta测试网
    private static final String shastaTestNetUrl = "https://api.shasta.trongrid.io";

    //主网
    private static final  String mainNetUrl = "https://api.trongrid.io";

    private String tronUrl = shastaTestNetUrl;

    /**
     * trx精度  1 trx = 1000000 sun
     */
    private BigDecimal decimal = new BigDecimal("1000000");

    /**
     * 转账
     */
    @Test
    public void sendTrx() throws Throwable {
        BigDecimal trxAmount = new BigDecimal("0.5");
        String privateKey = "7a2195d52c42c34a8de11633de7fdfbbf6883d2e95918ccd845230629fd95768";
        String toAddress = "TDzVKgBF9WSFox22qbdm7YYec9NjaXUrrr";

        String url = tronUrl + "/wallet/createtransaction";
        JSONObject param = new JSONObject();
        param.put("owner_address",TronUtils.toHexAddress(TronUtils.getAddressByPrivateKey(privateKey)));
        param.put("to_address",TronUtils.toHexAddress(toAddress));
        param.put("amount",trxAmount.multiply(decimal).toBigInteger());
        String _result = HttpClientUtils.postJson(url, param.toJSONString());
        String txid = null;//交易id
        if(StringUtils.isNotEmpty(_result)){
            JSONObject transaction = JSONObject.parseObject(_result);
            transaction.getJSONObject("raw_data").put("data", Hex.toHexString("这里是备注信息".getBytes()));
            txid = TronUtils.signAndBroadcast(tronUrl, privateKey, transaction);
            System.out.println(txid);
        }
    }

    /**
     * 查询额度
     */
    @Test
    public void balanceOf() throws Throwable {
        String queryAddress = "TA1gLs6FS8eik5NJqjvm73L4qRqWDmLwmh";
        String url = tronUrl + "/wallet/getaccount";
        JSONObject param = new JSONObject();
        param.put("address", TronUtils.toHexAddress(queryAddress));
        String result = HttpClientUtils.postJson(url, param.toJSONString());
        BigInteger balance = BigInteger.ZERO;
        if (!StringUtils.isEmpty(result)) {
            JSONObject obj = JSONObject.parseObject(result);
            BigInteger b = obj.getBigInteger("balance");
            if(b != null){
                balance = b;
            }
        }

        System.out.println("trx:"+new BigDecimal(balance).divide(decimal,6, RoundingMode.FLOOR));
    }
}
