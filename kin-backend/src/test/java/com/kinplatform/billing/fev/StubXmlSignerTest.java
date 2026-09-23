package com.kinplatform.billing.fev;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class StubXmlSignerTest {

    private final StubXmlSigner signer = new StubXmlSigner();

    @Test
    void sign_returnsCufeAndSignedXml() {
        XmlSigner.Signature signature = signer.sign("<Factura>1</Factura>");

        assertNotNull(signature.cufe());
        assertEquals(64, signature.cufe().length());
        assertThat(signature.signedXml()).contains("<ds:Signature");
        assertThat(signature.qrCode()).contains(signature.cufe());
    }

    @Test
    void sign_isDeterministicForSameInput() {
        String xml = "<Factura>1</Factura>";

        String first = signer.sign(xml).cufe();
        String second = signer.sign(xml).cufe();

        assertEquals(first, second);
    }

    @Test
    void sign_rejectsNullXml() {
        assertThrows(IllegalArgumentException.class, () -> signer.sign(null));
    }

    @Test
    void sha256Hex_matchesKnownDigest() {
        assertEquals(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                StubXmlSigner.sha256Hex("abc"));
    }
}
