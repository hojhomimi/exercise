import android.util.Base64;

import java.util.HashMap;
import java.util.Map;

public class Digest {
    public final String user;
    public final String pass;
    public final String nonce;
    public final String realm;
    public final String opaque;
    public final String qop;

    protected int nonceCount = 1;

    public Digest(String user, String pass, String wwwAuth) {
        Map<String, String> tupels = getTupels(wwwAuth);
        this.user = user;
        this.pass = pass;
        this.nonce = tupels.get("nonce");
        this.realm = tupels.get("Digest realm");
        this.opaque = tupels.get("opaque");
        this.qop = tupels.get("qop");
    }


    private Map<String, String> getTupels(String wwwAuth) {
        Map<String, String> tupels = new HashMap<>();
        String[] split = wwwAuth.split(", ");
        for (String part : split) {
            String[] pair = part.split("=");
            tupels.put(pair[0], pair[1].substring(1, pair[1].length() - 1));
        }
        return tupels;
    }

    public String calculateDigestAuthorization(String method, String digestURI) {
        String cnonce = Long.toHexString(Double.doubleToLongBits(Math.random()));
        return calculateDigestAuthorization(method, digestURI, cnonce);
    }

    String getISOHash(String s) {
        return Base64.encodeToString(s.getBytes(), Base64.NO_WRAP);
    }

    public String calculateDigestAuthorization(String method, String digestURI, String cnonce) {

        String HA1 = getISOHash(user + ":" + realm + ":" + pass).toLowerCase();
        String HA2 = getISOHash(method + ":" + digestURI).toLowerCase();
        String nonceCountStr = String.format("%08d", nonceCount++);
        String response = getISOHash(
                HA1 + ":" + nonce + ":" + nonceCountStr + ":" + cnonce + ":" + qop + ":"
                        + HA2).toLowerCase();

        return "Digest username=\"" + user + "\", realm=\"" + realm + "\", nonce=\"" + nonce
                + "\", uri=\"" + digestURI + "\", qop=auth, nc=" + nonceCountStr + ", cnonce=\""
                + cnonce + "\", response=\"" + response + "\", opaque=\"" + opaque + "\"";
    }

    public String calculateDigestAuthorizationNoSpaces(String method, String digestURI,
            String cnonce) {

        String HA1 = getISOHash(user + ":" + realm + ":" + pass).toLowerCase();
        String HA2 = getISOHash(method + ":" + digestURI).toLowerCase();
        String nonceCountStr = String.format("%08d", nonceCount++);
        String response = getISOHash(
                HA1 + ":" + nonce + ":" + nonceCountStr + ":" + cnonce + ":" + qop + ":"
                        + HA2).toLowerCase();

        return "Digest username=\"" + user + "\",realm=\"" + realm + "\",nonce=\"" + nonce
                + "\",uri=\"" + digestURI + "\",qop=auth,nc=" + nonceCountStr + ",cnonce=\""
                + cnonce + "\",response=\"" + response + "\",opaque=\"" + opaque + "\"";
    }
}
