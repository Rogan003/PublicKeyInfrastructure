package org.example.publickeyinfrastructure.Utils;

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import java.io.IOException;
import java.io.StringReader;

public class PemReaderUtil {
    public static byte[] readPem(String csrPem) throws IOException {
        String sanitizedPem = ensureCorrectPemFormat(csrPem);

        try (PemReader reader = new PemReader(new StringReader(sanitizedPem))) {
            PemObject pemObject = reader.readPemObject();

            if (pemObject == null) {
                throw new IllegalArgumentException("Invalid PEM format: No PEM object found.");
            }

            return pemObject.getContent();
        }
    }

    private static String ensureCorrectPemFormat(String csrPem) {
        return csrPem.replace("\\n", "\n").trim();
    }
}