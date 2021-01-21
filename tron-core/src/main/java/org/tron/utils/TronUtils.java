package org.tron.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.digests.SM3Digest;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.protos.Protocol;
import org.tron.protos.contract.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * tron-utils
 *
 * @Autor TrickyZh 2021/1/20
 * @Date 2021-01-20 18:46:18
 */
public class TronUtils {
	private static Logger logger = LoggerFactory.getLogger(TronUtils.class);
	static int ADDRESS_SIZE = 21;
	private static byte addressPreFixByte = (byte) 0x41; // 41 + address (byte) 0xa0; //a0 + address
	private static SecureRandom random = new SecureRandom();

	/**
	 * 转换成hex地址
	 *
	 * @param address
	 * @return
	 */
	public static String toHexAddress(String address) {
		if (StringUtils.isEmpty(address)) {
			throw new IllegalArgumentException("传入的地址不可为空");
		}
		if (!address.startsWith("T")) {
			throw new IllegalArgumentException("传入地址不合法:" + address);
		}
		return Hex.toHexString(decodeFromBase58Check(address));
	}

	/**
	 * 离线创建地址
	 *
	 * @return
	 */
	public static Map<String, String> createAddress() {
		ECKey eCkey = new ECKey(random);
		String privateKey = ByteArray.toHexString(eCkey.getPrivKeyBytes());
		byte[] addressBytes = eCkey.getAddress();
		String hexAddress = ByteArray.toHexString(addressBytes);
		Map<String, String> addressInfo = new HashMap<>();
		addressInfo.put("address", toViewAddress(hexAddress));
		addressInfo.put("hexAddress", hexAddress);
		addressInfo.put("privateKey", privateKey);
		return addressInfo;
	}

	/**
	 * 根据私钥获取地址
	 *
	 * @param privateKey
	 * @return
	 */
	public static String getAddressByPrivateKey(String privateKey) {
		byte[] privateBytes = Hex.decode(privateKey);
		ECKey ecKey = ECKey.fromPrivate(privateBytes);
		byte[] from = ecKey.getAddress();
		return toViewAddress(Hex.toHexString(from));
	}

	/**
	 * 广播交易信息 返回交易id
	 * @param tronUrl
	 * @param transaction
	 * @return
	 */
	public static String signAndBroadcast(String tronUrl,String privateKey,JSONObject transaction)throws Throwable{
		if(tronUrl.endsWith("/")){
			tronUrl= tronUrl.substring(0,tronUrl.length() - 1);
		}
		Protocol.Transaction tx = packTransaction(transaction.toJSONString());
		byte[] bytes = signTransactionByte(tx.toByteArray(), ByteArray.fromHexString(privateKey));
		String signTransation = Hex.toHexString(bytes);
		JSONObject jsonObjectGB = new JSONObject();
		jsonObjectGB.put("transaction", signTransation);
		String url = tronUrl + "/wallet/broadcasthex";
		String transationCompelet1 = HttpClientUtils.postJson(url, jsonObjectGB.toString());
		JSONObject transationCompelet = JSONObject.parseObject(transationCompelet1);
		if (transationCompelet.getBoolean("result")) {
			return transationCompelet.getString("txid");
		} else {
			logger.error(String.format("签名交易失败:%s",transationCompelet1));
			return null;
		}
	}

	/**
	 * 签名交易
	 * @param transaction
	 * @param privateKey
	 * @return
	 * @throws InvalidProtocolBufferException
	 * @throws NoSuchAlgorithmException
	 */
	public static byte[] signTransactionByte(byte[] transaction, byte[] privateKey) throws InvalidProtocolBufferException, NoSuchAlgorithmException {
		ECKey ecKey = ECKey.fromPrivate(privateKey);
		Protocol.Transaction transaction1 = Protocol.Transaction.parseFrom(transaction);
		byte[] rawdata = transaction1.getRawData().toByteArray();
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		digest.update(rawdata,0,rawdata.length);
		byte[] hash= digest.digest();
		byte[] sign = ecKey.sign(hash).toByteArray();
		return transaction1.toBuilder().addSignature(ByteString.copyFrom(sign)).build().toByteArray();
	}

	/**
	 * 转换成T开头的地址
	 * @param hexAddress
	 * @return
	 */
	public static String toViewAddress(String hexAddress) {
		return encode58Check(ByteArray.fromHexString(hexAddress));
	}

	public static String encode58Check(byte[] input) {
		try {
			byte[] hash0 = hash(true, input);
			byte[] hash1 = hash(true, hash0);
			byte[] inputCheck = new byte[input.length + 4];
			System.arraycopy(input, 0, inputCheck, 0, input.length);
			System.arraycopy(hash1, 0, inputCheck, input.length, 4);
			return Base58.encode(inputCheck);
		} catch (Throwable t) {
			logger.error(String.format("data error:%s", Hex.toHexString(input)), t);
		}
		return null;
	}

	private static byte[] decode58Check(String input) throws Exception {
		byte[] decodeCheck = Base58.decode(input);
		if (decodeCheck.length <= 4) {
			return null;
		}
		byte[] decodeData = new byte[decodeCheck.length - 4];
		System.arraycopy(decodeCheck, 0, decodeData, 0, decodeData.length);
		byte[] hash0 = hash(true, decodeData);
		byte[] hash1 = hash(true, hash0);
		if (hash1[0] == decodeCheck[decodeData.length] && hash1[1] == decodeCheck[decodeData.length + 1]
				&& hash1[2] == decodeCheck[decodeData.length + 2] && hash1[3] == decodeCheck[decodeData.length + 3]) {
			return decodeData;
		}
		return null;
	}

	/**
	 * Calculates the SHA-256 hash of the given bytes.
	 *
	 * @param input the bytes to hash
	 * @return the hash (in big-endian order)
	 */
	public static byte[] hash(boolean isSha256, byte[] input) throws NoSuchAlgorithmException {
		return hash(isSha256, input, 0, input.length);
	}

	/**
	 * Calculates the SHA-256 hash of the given byte range.
	 *
	 * @param input  the array containing the bytes to hash
	 * @param offset the offset within the array of the bytes to hash
	 * @param length the number of bytes to hash
	 * @return the hash (in big-endian order)
	 */
	public static byte[] hash(boolean isSha256, byte[] input, int offset, int length) throws NoSuchAlgorithmException {
		if (isSha256) {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			digest.update(input, offset, length);
			return digest.digest();
		} else {
			SM3Digest digest = new SM3Digest();
			digest.update(input, offset, length);
			byte[] eHash = new byte[digest.getDigestSize()];
			digest.doFinal(eHash, 0);
			return eHash;
		}
	}


	public static byte[] decodeFromBase58Check(String addressBase58) {
		try {
			byte[] address = decode58Check(addressBase58);
			if (!addressValid(address)) {
				return null;
			}
			return address;
		} catch (Throwable t) {
			logger.error(String.format("decodeFromBase58Check-error:" + addressBase58), t);
		}
		return null;
	}

	private static boolean addressValid(byte[] address) {
		if (ArrayUtils.isEmpty(address)) {
			return false;
		}
		if (address.length != ADDRESS_SIZE) {
			return false;
		}
		byte preFixbyte = address[0];
		return preFixbyte == addressPreFixByte;
		// Other rule;
	}

	/**
	 * 报装成transaction
	 *
	 * @param strTransaction
	 * @return
	 */
	public static Protocol.Transaction packTransaction(String strTransaction) {
		JSONObject jsonTransaction = JSONObject.parseObject(strTransaction);
		JSONObject rawData = jsonTransaction.getJSONObject("raw_data");
		JSONArray contracts = new JSONArray();
		JSONArray rawContractArray = rawData.getJSONArray("contract");
		for (int i = 0; i < rawContractArray.size(); i++) {
			try {
				JSONObject contract = rawContractArray.getJSONObject(i);
				JSONObject parameter = contract.getJSONObject("parameter");
				String contractType = contract.getString("type");
				Any any = null;
				switch (contractType) {
					case "AccountCreateContract":
						AccountContract.AccountCreateContract.Builder accountCreateContractBuilder = AccountContract.AccountCreateContract
								.newBuilder();
						JsonFormat.merge(parameter.getJSONObject("value").toString(),
								accountCreateContractBuilder);
						any = Any.pack(accountCreateContractBuilder.build());
						break;
					case "TransferContract":
						BalanceContract.TransferContract.Builder transferContractBuilder = BalanceContract.TransferContract.newBuilder();
						JsonFormat
								.merge(parameter.getJSONObject("value").toString(), transferContractBuilder);
						any = Any.pack(transferContractBuilder.build());
						break;
					case "TransferAssetContract":
						AssetIssueContractOuterClass.TransferAssetContract.Builder transferAssetContractBuilder = AssetIssueContractOuterClass.TransferAssetContract
								.newBuilder();
						JsonFormat.merge(parameter.getJSONObject("value").toString(),
								transferAssetContractBuilder);
						any = Any.pack(transferAssetContractBuilder.build());
						break;
					case "VoteAssetContract":
						VoteAssetContractOuterClass.VoteAssetContract.Builder voteAssetContractBuilder = VoteAssetContractOuterClass.VoteAssetContract.newBuilder();
						JsonFormat
								.merge(parameter.getJSONObject("value").toString(), voteAssetContractBuilder);
						any = Any.pack(voteAssetContractBuilder.build());
						break;
					case "VoteWitnessContract":
						WitnessContract.VoteWitnessContract.Builder voteWitnessContractBuilder = WitnessContract.VoteWitnessContract
								.newBuilder();
						JsonFormat
								.merge(parameter.getJSONObject("value").toString(), voteWitnessContractBuilder);
						any = Any.pack(voteWitnessContractBuilder.build());
						break;
					case "WitnessCreateContract":
						WitnessContract.WitnessCreateContract.Builder witnessCreateContractBuilder = WitnessContract.WitnessCreateContract
								.newBuilder();
						JsonFormat.merge(parameter.getJSONObject("value").toString(),
								witnessCreateContractBuilder);
						any = Any.pack(witnessCreateContractBuilder.build());
						break;
					case "AssetIssueContract":
						AssetIssueContractOuterClass.AssetIssueContract.Builder assetIssueContractBuilder = AssetIssueContractOuterClass.AssetIssueContract.newBuilder();
						JsonFormat
								.merge(parameter.getJSONObject("value").toString(), assetIssueContractBuilder);
						any = Any.pack(assetIssueContractBuilder.build());
						break;
					case "WitnessUpdateContract":
						WitnessContract.WitnessUpdateContract.Builder witnessUpdateContractBuilder = WitnessContract.WitnessUpdateContract
								.newBuilder();
						JsonFormat.merge(parameter.getJSONObject("value").toString(),
								witnessUpdateContractBuilder);
						any = Any.pack(witnessUpdateContractBuilder.build());
						break;
					case "ParticipateAssetIssueContract":
						AssetIssueContractOuterClass.ParticipateAssetIssueContract.Builder participateAssetIssueContractBuilder =
								AssetIssueContractOuterClass.ParticipateAssetIssueContract.newBuilder();
						JsonFormat.merge(parameter.getJSONObject("value").toString(),
								participateAssetIssueContractBuilder);
						any = Any.pack(participateAssetIssueContractBuilder.build());
						break;
					case "AccountUpdateContract":
						AccountContract.AccountUpdateContract.Builder accountUpdateContractBuilder = AccountContract.AccountUpdateContract
								.newBuilder();
						JsonFormat.merge(parameter.getJSONObject("value").toString(),
								accountUpdateContractBuilder);
						any = Any.pack(accountUpdateContractBuilder.build());
						break;
					case "FreezeBalanceContract":
						BalanceContract.FreezeBalanceContract.Builder freezeBalanceContractBuilder = BalanceContract.FreezeBalanceContract
								.newBuilder();
						JsonFormat.merge(parameter.getJSONObject("value").toString(),
								freezeBalanceContractBuilder);
						any = Any.pack(freezeBalanceContractBuilder.build());
						break;
					case "UnfreezeBalanceContract":
						BalanceContract.UnfreezeBalanceContract.Builder unfreezeBalanceContractBuilder = BalanceContract.UnfreezeBalanceContract
								.newBuilder();
						JsonFormat.merge(parameter.getJSONObject("value").toString(),
								unfreezeBalanceContractBuilder);
						any = Any.pack(unfreezeBalanceContractBuilder.build());
						break;
					case "UnfreezeAssetContract":
						AssetIssueContractOuterClass.UnfreezeAssetContract.Builder unfreezeAssetContractBuilder = AssetIssueContractOuterClass.UnfreezeAssetContract
								.newBuilder();
						JsonFormat.merge(parameter.getJSONObject("value").toString(),
								unfreezeAssetContractBuilder);
						any = Any.pack(unfreezeAssetContractBuilder.build());
						break;
					case "WithdrawBalanceContract":
						BalanceContract.WithdrawBalanceContract.Builder withdrawBalanceContractBuilder = BalanceContract.WithdrawBalanceContract
								.newBuilder();
						JsonFormat.merge(parameter.getJSONObject("value").toString(),
								withdrawBalanceContractBuilder);
						any = Any.pack(withdrawBalanceContractBuilder.build());
						break;
					case "UpdateAssetContract":
						AssetIssueContractOuterClass.UpdateAssetContract.Builder updateAssetContractBuilder = AssetIssueContractOuterClass.UpdateAssetContract
								.newBuilder();
						JsonFormat
								.merge(parameter.getJSONObject("value").toString(), updateAssetContractBuilder);
						any = Any.pack(updateAssetContractBuilder.build());
						break;
					case "SmartContract":
						SmartContractOuterClass.SmartContract.Builder smartContractBuilder = SmartContractOuterClass.SmartContract.newBuilder();
						JsonFormat
								.merge(parameter.getJSONObject("value").toString(), smartContractBuilder);
						any = Any.pack(smartContractBuilder.build());
						break;
					case "TriggerSmartContract":
						SmartContractOuterClass.TriggerSmartContract.Builder triggerSmartContractBuilder = SmartContractOuterClass.TriggerSmartContract
								.newBuilder();
						JsonFormat
								.merge(parameter.getJSONObject("value").toString(),
										triggerSmartContractBuilder);
						any = Any.pack(triggerSmartContractBuilder.build());
						break;
					// todo add other contract
					default:
				}
				if (any != null) {
					String value = Hex.toHexString(any.getValue().toByteArray());
					parameter.put("value", value);
					contract.put("parameter", parameter);
					contracts.add(contract);
				}
			} catch (Exception e) {
				e.printStackTrace();
				;
			}
		}
		rawData.put("contract", contracts);
		jsonTransaction.put("raw_data", rawData);
		Protocol.Transaction.Builder transactionBuilder = Protocol.Transaction.newBuilder();
		try {
			JsonFormat.merge(jsonTransaction.toString(), transactionBuilder);
			return transactionBuilder.build();
		} catch (Exception e) {
			return null;
		}

	}
}
