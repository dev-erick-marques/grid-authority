package com.gridauthority.coordinator.api;
import com.gridauthority.coordinator.signing.LocalSigningService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequiredArgsConstructor @RequestMapping("/api/authority")
public class AuthorityController {
 private final LocalSigningService signing;
 @GetMapping("/public-key")
 public Map<String,String> getPublicKey(){return Map.of("keyId",signing.keyId(),"signingAlgorithm","SHA256withECDSA","publicKeyBase64",signing.publicKeyBase64());}
}
