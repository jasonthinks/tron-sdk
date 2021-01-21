package org.tron.common.crypto;

import org.tron.common.crypto.ECKey.ECDSASignature;
import org.tron.common.crypto.sm2.SM2;
import org.tron.common.crypto.sm2.SM2.SM2Signature;

import java.security.SecureRandom;
import java.security.SignatureException;

public class SignUtils {
   public static SignInterface getGeneratedRandomSign(boolean isECKeyCryptoEngine) {
     if (isECKeyCryptoEngine) {
       return new ECKey();
     }
     return new SM2();
   }

  public static SignInterface getGeneratedRandomSign(
      SecureRandom secureRandom, boolean isECKeyCryptoEngine) {
    if (isECKeyCryptoEngine) {
      return new ECKey(secureRandom);
    }
    return new SM2(secureRandom);
  }

   public static SignInterface fromPrivate(byte[] privKeyBytes, boolean isECKeyCryptoEngine) {
     if (isECKeyCryptoEngine) {
       return ECKey.fromPrivate(privKeyBytes);
     }
     return SM2.fromPrivate(privKeyBytes);
   }

  public static byte[] signatureToAddress(byte[] messageHash, String
      signatureBase64, boolean isECKeyCryptoEngine) throws SignatureException {
    if (isECKeyCryptoEngine) {
      return ECKey.signatureToAddress(messageHash, signatureBase64);
    }
    return SM2.signatureToAddress(messageHash, signatureBase64);
  }

  public static SignatureInterface fromComponents(byte[] r, byte[] s, byte v, boolean isECKeyCryptoEngine) {
     if (isECKeyCryptoEngine) {
       return ECDSASignature.fromComponents(r, s, v);
     }
     return SM2Signature.fromComponents(r, s, v);
  }

  public static byte[] signatureToAddress(byte[] messageHash, SignatureInterface signatureInterface,
      boolean isECKeyCryptoEngine) throws SignatureException{
     if (isECKeyCryptoEngine) {
       return ECKey.signatureToAddress(messageHash, (ECDSASignature)signatureInterface);
     }
     return SM2.signatureToAddress(messageHash, (SM2Signature)signatureInterface);
  }
}
