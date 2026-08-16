package com.ssafy.layover.common.crypto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EncryptedValue {

    private String cipherText;
    private String iv;
}
