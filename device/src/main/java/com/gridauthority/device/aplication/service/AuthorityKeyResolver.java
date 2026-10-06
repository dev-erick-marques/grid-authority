package com.gridauthority.device.aplication.service;
import com.gridauthority.device.infrastructure.config.CoordinatorProperties;
import com.gridauthority.device.domain.exception.InvalidPublicKeyFormatException;
import lombok.Getter; import lombok.RequiredArgsConstructor; import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service; import org.springframework.web.client.RestClient;
import jakarta.annotation.PostConstruct;
import java.security.*; import java.security.spec.X509EncodedKeySpec; import java.util.Base64; import java.util.Map;
@Service @RequiredArgsConstructor @Slf4j @Getter
public class AuthorityKeyResolver {
 private final CoordinatorProperties coordinator; private final RestClient restClient;
 private PublicKey publicKey; private String keyId;
 @PostConstruct
 public void resolve(){
  try{
   Map<?,?> body=restClient.get().uri(coordinator.getUrl()+"/api/authority/public-key").retrieve().body(Map.class);
   keyId=String.valueOf(body.get("keyId"));
   byte[] der=Base64.getDecoder().decode(String.valueOf(body.get("publicKeyBase64")));
   publicKey=KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(der));
   log.info("[AUTHORITY] Public key loaded keyId={} from coordinator",keyId);
  }catch(Exception e){throw new IllegalStateException("Unable to resolve coordinator public key",e);}
 }
 public PublicKey getResolvedPublicKey(){return publicKey;}
}
