import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.rubato.math.certificates.AgdaCertificates;

/**
 * Writes GeneratedCertificates.agda to standard output: the certificates of
 * AgdaCertificates.exampleModule() (Flieder, arXiv 2512.04090, Ex. 2.4, B.2 and
 * B.3; sources/AGDA-CERTIFICATES-SPEC.md). run-checks.sh compares the output
 * with the committed file, and so does AgdaCertificatesTest in the Maven build.
 */
public class ExportCertificates {

    public static void main(String[] args) throws Exception {
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8.name());
        out.print(AgdaCertificates.exampleModule());
        out.flush();
    }
}
