package org.tron.test;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.tron.protos.Protocol;
import org.tron.utils.HttpClientUtils;
import org.tron.utils.TronUtils;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * trc20测试
 *
 * @Autor TrickyZh 2021/1/21
 * @Date 2021-01-21 21:04:03
 */
public class Trc20Test {
    //shasta测试网
    private static final String shastaTestNetUrl = "https://api.shasta.trongrid.io";

    //主网
    private static final String mainNetUrl = "https://api.trongrid.io";

    private String tronUrl = shastaTestNetUrl;

    /**
     * 合约精度
     */
    private BigDecimal decimal = new BigDecimal("1000000");

    /**
     * trc20合约地址 这个是shasta测试网上面的一个trc20代币
     */
    private String contract = "TVfi96PXjv1RySUeZ39eSQqJmLnr9frroK";

    /**
     * 查询trc20数量
     */
    @Test
    public void balanceOfTrc20()throws Throwable{
        String queryAddress = "TDzVKgBF9WSFox22qbdm7YYec9NjaXUrrr";

        String url = tronUrl + "/wallet/triggerconstantcontract";
        JSONObject param = new JSONObject();
        param.put("owner_address",TronUtils.toHexAddress(queryAddress));
        param.put("contract_address",TronUtils.toHexAddress(contract));
        param.put("function_selector","balanceOf(address)");
        List<Type> inputParameters = new ArrayList<>();
        inputParameters.add(new Address(TronUtils.toHexAddress(queryAddress).substring(2)));
        param.put("parameter",FunctionEncoder.encodeConstructor(inputParameters));
        String result = HttpClientUtils.postJson(url, param.toJSONString());
        BigDecimal amount = BigDecimal.ZERO;
        if(StringUtils.isNotEmpty(result)){
            JSONObject obj = JSONObject.parseObject(result);
            JSONArray results = obj.getJSONArray("constant_result");
            if(results != null && results.size() > 0){
                BigInteger _amount = new BigInteger(results.getString(0),16);
                amount = new BigDecimal(_amount).divide(decimal,6, RoundingMode.FLOOR);
            }
        }
        System.out.println(String.format("账号%s的balance=%s",queryAddress,amount.toString()));
    }

    /**
     * 发起trc20转账
     * @throws Throwable
     */
    @Test
    public void sendTrc20() throws Throwable {
        String privateKey = "7a2195d52c42c34a8de11633de7fdfbbf6883d2e95918ccd845230629fd95768";
        String toAddress = "TDzVKgBF9WSFox22qbdm7YYec9NjaXUrrr";
        BigDecimal amount = new BigDecimal("0.01");

        String ownerAddress = TronUtils.getAddressByPrivateKey(privateKey);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("contract_address", TronUtils.toHexAddress(contract));
        jsonObject.put("function_selector", "transfer(address,uint256)");
        List<Type> inputParameters = new ArrayList<>();
        inputParameters.add(new Address(TronUtils.toHexAddress(toAddress).substring(2)));
        inputParameters.add(new Uint256(amount.multiply(decimal).toBigInteger()));
        String parameter = FunctionEncoder.encodeConstructor(inputParameters);
        jsonObject.put("parameter", parameter);
        jsonObject.put("owner_address", TronUtils.toHexAddress(ownerAddress));
        jsonObject.put("call_value", 0);
        jsonObject.put("fee_limit", 6000000L);
        String trans1 = HttpClientUtils.postJson(tronUrl + "/wallet/triggersmartcontract", jsonObject.toString());
        JSONObject result = JSONObject.parseObject(trans1);
        if (result.containsKey("Error")) {
            System.out.println("send error==========");
            return;
        }
        JSONObject tx = result.getJSONObject("transaction");
        tx.getJSONObject("raw_data").put("data", Hex.toHexString("我是Tricky".getBytes()));//填写备注
        String txid = TronUtils.signAndBroadcast(tronUrl, privateKey, tx);
        if (txid != null) {
            System.out.println("交易Id:" + txid);
        }
    }
}
